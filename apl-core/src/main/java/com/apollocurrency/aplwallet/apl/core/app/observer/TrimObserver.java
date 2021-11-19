/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.config.TrimEventCommand;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
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
import java.util.concurrent.TimeUnit;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private static final int QUEUE_NO_SPEEDUP_SIZE_THRESHOLD = 3;
    public static final int MIN_ALLOWED_TRIM_DELAY = 5;
    private final TrimService trimService;
    private final Object lock = new Object();
    private final Queue<Integer> trimHeights = new PriorityQueue<>(); // will sort heights from lowest to highest automatically
    private volatile boolean trimDerivedTablesEnabled = true;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-task-random-trim"));
    private final Blockchain blockchain;
    private volatile Random random;
    private volatile TrimConfig trimConfig;
    /**
     * Callable task for method to run. Next run is scheduled as soon as previous has finished
     */
    private final Callable<Void> taskToCall = () -> {
        try {
            // Do work.
            processScheduledTrimEvent();
        } finally {
            // Reschedule next new Callable with next random delay within timeDelay sec range
                scheduleTrimTask();
        }
        return null;
    };


    @Inject
    public TrimObserver (TrimService trimService,
                         TrimConfig trimConfig,
                         Blockchain blockchain) {
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.trimConfig = trimConfig;
        this.random = new Random();
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @PostConstruct
    void init() {
        scheduleTrimTask();
    }

    private void scheduleTrimTask() {
        long delay = calculateDelay();
        executorService.schedule(taskToCall, delay, TimeUnit.MILLISECONDS);
    }

    private long calculateDelay() {
        long delay = trimConfig.getDefaultTrimDelay();
        synchronized (lock) {
            int trimDelay = trimConfig.getTrimDelay();
            if (trimDerivedTablesEnabled && !trimHeights.isEmpty() && trimHeights.size() <= QUEUE_NO_SPEEDUP_SIZE_THRESHOLD && trimDelay >= 0) {
                int correctedTrimDelay  = Math.max(trimDelay, MIN_ALLOWED_TRIM_DELAY);
                int minTrimDelay = trimDelay / 4;
                delay = 1000L * (random.nextInt(correctedTrimDelay - minTrimDelay + 1) + minTrimDelay);
                log.debug("Next trim operation delay '{}' ms", delay);
            }
        }
        return delay;
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    public boolean trimEnabled() {
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
                trimService.trimDerivedTables(trimHeight);
            } else {
                log.trace("NO performed trim on height={}", trimHeight);
            }
        }
    }

    public List<Integer> getTrimQueue() {
        synchronized (lock) {
            return new ArrayList<>(trimHeights);
        }
    }

    public void onTrimConfigUpdated(@Observes @TrimConfigUpdated TrimEventCommand trimEventCommand) {
        log.info("Set trim to {} ", trimEventCommand.isEnableTrim());
        this.trimDerivedTablesEnabled = trimEventCommand.isEnableTrim();
        if (trimEventCommand.isClearTrimQueue()) {
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
        if (trimDerivedTablesEnabled && block.getHeight() % trimConfig.getTrimFrequency() == 0) {
            trimService.trimDerivedTables(block.getHeight());
        }
    }

    public int onBlockPushed(@Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int scheduleTrimHeight = -1;
        if (block.getHeight() % trimConfig.getTrimFrequency() == 0) {
            synchronized (lock) {
                scheduleTrimHeight = block.getHeight();
                log.debug("Schedule next trim for height={} at {}", scheduleTrimHeight, block.getHeight());
                trimHeights.add(scheduleTrimHeight);
            }
        } else {
            log.trace("Skip Trim schedule on block height='{}' NOT div % by trimFreq={}", block.getHeight(), trimConfig.getTrimFrequency());
        }
        return scheduleTrimHeight;
    }

    public void setTrimConfig(TrimConfig trimConfig) {
        this.trimConfig = trimConfig;
    }
}
