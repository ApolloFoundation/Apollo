/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
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

import static com.apollocurrency.aplwallet.apl.util.Constants.LONG_TIME_FIVE_SECONDS;
import static com.apollocurrency.aplwallet.apl.util.Constants.LONG_TIME_TWO_SECONDS;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private final TrimService trimService;
    private final Object lock = new Object();
    private final Queue<Integer> trimHeights = new PriorityQueue<>(); // will sort heights from lowest to highest automatically
    private final int maxRollback;
    private volatile boolean trimDerivedTablesEnabled = true;
    private final int trimFrequency;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-task-random-trim"));
    private final BlockchainConfig blockchainConfig;
    private final PropertiesHolder propertiesHolder;
    private final Blockchain blockchain;
    private final Random random;
    private final boolean isShardingOff;
    /**
     * Callable task for method to run. Next run is scheduled as soon as previous has finished
     */
    private Callable<Void> taskToCall = new Callable<>() {
        public Void call() {
            try {
                // Do work.
                processScheduledTrimEvent();
            } finally {
                // Reschedule next new Callable with next random delay within 5 sec range
                executorService.schedule(this,
                    ThreadLocalRandom.current().nextLong(
                        LONG_TIME_FIVE_SECONDS - 1L) + 1L, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    };

    @Inject
    public TrimObserver(TrimService trimService, BlockchainConfig blockchainConfig,
                        PropertiesHolder propertiesHolder, Random random,
                        Blockchain blockchain) {
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.trimFrequency = Constants.DEFAULT_TRIM_FREQUENCY;
        this.isShardingOff = this.propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        this.random = Objects.requireNonNullElseGet(random, Random::new);
        this.maxRollback = this.propertiesHolder.getIntProperty("apl.maxRollback", 720);
    }

    @PostConstruct
    void init() {
        if (this.blockchainConfig.getCurrentConfig().isShardingEnabled()
            && this.blockchainConfig.getCurrentConfig().getShardingFrequency() > 0 && this.trimFrequency > 0
            && this.blockchainConfig.getCurrentConfig().getShardingFrequency() < this.trimFrequency) {
            String error = String.format(
                "SHARDING FREQUENCY ERROR: configured 'shard frequency value'=%d is LOWER then 'DEFAULT_TRIM_FREQUENCY'=%d",
                this.blockchainConfig.getCurrentConfig().getShardingFrequency(), this.trimFrequency);
            log.error(error);
            throw new RuntimeException(error);
        }
        // schedule first run with random delay in 2 sec range
        executorService.schedule(taskToCall, ThreadLocalRandom.current().nextLong(
            LONG_TIME_TWO_SECONDS - 1L) + 1L, TimeUnit.MILLISECONDS);
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
            // we need to know that current config was just changed by 'APPLY_BLOCK' event on earlier processing stage
            // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated() usage
            boolean isConfigJustUpdated = blockchainConfig.isJustUpdated();
            HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
            boolean shardingEnabled = currentConfig.isShardingEnabled();
            log.debug("Is sharding DISabled ? : '{}' || '{}' on height={}",
                !shardingEnabled, isShardingOff, block.getHeight());
            int randomTrimHeightIncrease = 0; // we will scheduled trim height by random value or by zero
            if (!shardingEnabled || isShardingOff) {
                // non sharded node, schedule next trim event processing randomized and added to current height all the time
                randomTrimHeightIncrease = generatePositiveIntBiggerThenZero(trimFrequency);
                log.trace("'Not sharded', trim height random increase = {}", randomTrimHeightIncrease);
            } else {
                // sharded node should 'predict' next shard height and DO NOT randomize in such case
                int trimHeight = Math.max(0, (block.getHeight() - maxRollback));
                int shardingFrequency;
                if (!isConfigJustUpdated) {
                    // config didn't change from previous trim scheduling
                    shardingFrequency = currentConfig.getShardingFrequency();
                } else {
                    // config has changed from previous trim scheduling, try to get previous 'shard frequency' value
                    shardingFrequency = blockchainConfig.getPreviousConfig().isPresent()
                        && blockchainConfig.getPreviousConfig().get().isShardingEnabled() ?
                        blockchainConfig.getPreviousConfig().get().getShardingFrequency() // previous config
                        : currentConfig.getShardingFrequency(); // fall back
                }
                // the boolean - if shard is possible by trim height
                boolean isShardingOnTrimHeight = (Math.max(trimHeight, 0)) % shardingFrequency == 0;

                // that boolean - if shard is possible by current blockchain height
                // we don't want support config params for 'maxRollback', 'shardFrequency' matched incorrectly/badly to each other
                // so we use that in order do not to 'miss' shard height additionally
                boolean isShardingOnBlockHeight = block.getHeight() % shardingFrequency == 0;
                // generate pseudo random for 'trim height divergence'
                randomTrimHeightIncrease = generatePositiveIntBiggerThenZero(trimFrequency);
/*
                if (isShardingOnBlockHeight || isShardingOnTrimHeight) {
                    // prevent trim randomization on 'potentially dangerous heights'
                    randomTrimHeightIncrease = 0; // schedule that case in any conditions
                } else if (trimService.isTrimming()) {
                    // SAFE SKIP, DO NOT schedule next if we haven't finished previous trim
                    log.debug("Schedule next trim SKIPPED={}, trimHeight = {} / {}, shardFreq={} ({}}), isShardingOnTrimHeight={}, isShardingOnBlockHeight={}",
                        randomTrimHeightIncrease, trimHeight, block.getHeight(), shardingFrequency,
                        isConfigJustUpdated, isShardingOnTrimHeight, isShardingOnBlockHeight);
                    return scheduleTrimHeight;
                }
*/
                log.debug("Schedule next trim for rndIncrease={}, height/trimHeight = {} / {}, shardFreq={} ({}}), isShardingOnTrimHeight={}, isShardingOnBlockHeight={}",
                    randomTrimHeightIncrease, trimHeight, block.getHeight(), shardingFrequency, isConfigJustUpdated, isShardingOnTrimHeight, isShardingOnBlockHeight);
            }
            synchronized (lock) {
                scheduleTrimHeight = block.getHeight() + randomTrimHeightIncrease; // increase next trim height with possible divergence
                log.debug("Schedule next trim for height={} at {}", scheduleTrimHeight, block.getHeight());
                trimHeights.add(scheduleTrimHeight);
            }
        } else {
            log.trace("Skip Trim schedule on block height='{}' NOT div % by trimFreq={}", block.getHeight(), trimFrequency);
        }
        return scheduleTrimHeight;
    }

    private int generatePositiveIntBiggerThenZero(int trimFrequency) {
        return random.nextInt(trimFrequency - 1) + 1;
    }

}
