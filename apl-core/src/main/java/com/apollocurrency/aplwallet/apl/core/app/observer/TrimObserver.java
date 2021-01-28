/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private final TrimService trimService;
    private final Object lock = new Object();
    private final Queue<Integer> trimHeights = new PriorityQueue<>(); // will sort heights from lowest to highest automatically
    private volatile boolean trimDerivedTablesEnabled = true;
    private final int trimFrequency;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-task-random-trim"));
    private final Blockchain blockchain;
    private final long trimDelay;
    private final Random random;
    /**
     * Callable task for method to run. Next run is scheduled as soon as previous has finished
     */
    private final Callable<Void> taskToCall = () -> {
        try {
            // Do work.
            processScheduledTrimEvent();
        } finally {
            // Reschedule next new Callable with next random delay within 5 sec range
            synchronized (lock) {
                scheduleTrimTask();
            }
        }
        return null;
    };


    @Inject
    public TrimObserver (TrimService trimService,
                         @Property(value = "apl.trimProcessingDelay", defaultValue = "500") int trimDelay,
                         @Property(value = "apl.trimFrequency", defaultValue = "1000")
                         Random random,
                         Blockchain blockchain) {
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.trimFrequency = Constants.DEFAULT_TRIM_FREQUENCY;
        this.trimDelay = trimDelay;
        this.random = random == null ? new Random() : random;
    }


    @PostConstruct
    void init() {
        scheduleTrimTask();
    }

    private void scheduleTrimTask() {
        long delay = calculateDelay();
        executorService.schedule(taskToCall, delay, TimeUnit.SECONDS);
    }

    private long calculateDelay() {
        long delay;
        if (trimHeights.size() >= 3 || trimDelay < 0) {
            delay = 2; // speedup trims
        } else {
            long correctedTrimDelay  = Math.max(trimDelay, 50);
            long minTrimDelay = trimDelay / 4;
            delay = ThreadLocalRandom.current().nextLong(correctedTrimDelay - minTrimDelay) + minTrimDelay;
        }
        return delay;
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    boolean isTrimDerivedTablesEnabled() {
        return trimDerivedTablesEnabled;
    }

    private void processScheduledTrimEvent() {
        log.trace("processTrimEvent() scheduled on previous run...");
        if (trimDerivedTablesEnabled) {
            boolean performTrim = false;
            Integer trimHeight = null;
            synchronized (lock) {
                if (trimDerivedTablesEnabled) {
                    trimHeight = trimHeights.peek();
                    performTrim = trimHeight != null && trimHeight <= blockchain.getHeight();
                    if (performTrim) {
                        trimHeights.remove();
                    }
                }
            }
            if (performTrim) {
                log.debug("Perform trim on blockchain height={}", trimHeight);
                trimService.trimDerivedTables(trimHeight, true);
            } else {
                log.trace("NO performed trim on height={}", trimHeight);
            }
        }
    }

    List<Integer> getTrimHeights() {
        synchronized (lock) {
            return new ArrayList<>(trimHeights);
        }
    }

    public void onTrimConfigUpdated(@Observes @TrimConfigUpdated TrimConfig trimConfig) {
        log.info("Set trim to {} ", trimConfig.isEnableTrim());
        this.trimDerivedTablesEnabled = trimConfig.isEnableTrim();
        if (trimConfig.isClearTrimQueue()) {
            synchronized (lock) {
                trimHeights.clear();
            }
        }
    }

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        //TODO: please replace 5000 with meaningful constant name
        if (block.getHeight() % 5000 == 0) {
            log.info("Scan: processed block " + block.getHeight());
        }
        if (trimDerivedTablesEnabled && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTablesOnBlockchainHeight(block.getHeight(), false);
        }
    }

    public int onBlockPushed(@Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int scheduleTrimHeight = -1;
        if (block.getHeight() % trimFrequency == 0) {
            synchronized (lock) {
                scheduleTrimHeight = block.getHeight();
                log.debug("Schedule next trim for height={} at {}", scheduleTrimHeight, block.getHeight());
                trimHeights.add(scheduleTrimHeight);
            }
        } else {
            log.trace("Skip Trim schedule on block height='{}' NOT div % by trimFreq={}", block.getHeight(), trimFrequency);
        }
        return scheduleTrimHeight;
    }
}
