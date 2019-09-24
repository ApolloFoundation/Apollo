/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimObserver {
    private static final Logger log = LoggerFactory.getLogger(TrimObserver.class);
    private final TrimService trimService;
    private volatile boolean trimDerivedTables = true;
    private int trimFrequency;
    private final Object lock = new Object();
    private final Queue<Integer> trimHeights = new PriorityQueue<>(); // will sort heights from lowest to highest automatically
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private BlockchainConfig blockchainConfig;
    private PropertiesHolder propertiesHolder;
    private Random random;
    private boolean isShardingOff;
    private final int maxRollback;

    @Inject
    public TrimObserver(TrimService trimService, BlockchainConfig blockchainConfig,
                        PropertiesHolder propertiesHolder, Random random) {
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.trimFrequency = Constants.DEFAULT_TRIM_FREQUENCY;
        this.isShardingOff = this.propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        if (random == null) {
            this.random = new Random();
        } else {
            this.random = random;
        }
        this.maxRollback = this.propertiesHolder.getIntProperty("apl.maxRollback", 720);
    }

    @PostConstruct
    void init() {
        if (this.blockchainConfig.getCurrentConfig() != null && this.blockchainConfig.getCurrentConfig().isShardingEnabled()
                && this.blockchainConfig.getCurrentConfig().getShardingFrequency() > 0 && this.trimFrequency > 0
                && this.blockchainConfig.getCurrentConfig().getShardingFrequency() < this.trimFrequency) {
            String error = String.format(
                    "SHARDING FREQUENCY ERROR: configured 'shard frequency value'=%d is LOWER then 'DEFAULT_TRIM_FREQUENCY'=%d",
                    this.blockchainConfig.getCurrentConfig().getShardingFrequency(), this.trimFrequency);
            log.error(error);
            throw new RuntimeException(error);
        }
        executorService.scheduleWithFixedDelay(this::processTrimEvent, 0, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    boolean isTrimDerivedTables() {
        return trimDerivedTables;
    }

    private void processTrimEvent() {
        if (trimDerivedTables) {
            Integer trimHeight = null;
            synchronized (lock) {
                if (trimDerivedTables) {
                    trimHeight = trimHeights.poll();
                }
            }
            if (trimHeight != null) {
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
        this.trimDerivedTables = trimConfig.isEnableTrim();
        if (trimConfig.isClearTrimQueue()) {
            synchronized (lock) {
                trimHeights.clear();
            }
        }
    }

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        //TODO: please replace 5000 with meaningfull constant name
        if (block.getHeight() % 5000 == 0) {
            log.info("Scan: processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            trimService.doTrimDerivedTablesOnBlockchainHeight(block.getHeight(), false);
        }
    }

    public int onBlockPushed(@Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int scheduleTrimHeight = -1;
        if (block.getHeight() % trimFrequency == 0) {
            // we need to know that current config was just changed by 'APPLY_BLOCK' event on earlier processing stage
            boolean isConfigJustUpdated = blockchainConfig.isJustUpdated();
            HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
            boolean shardingEnabled = currentConfig.isShardingEnabled();
            log.debug("Is sharding DISabled ? : '{}' || '{}' on height={}",
                    !shardingEnabled, isShardingOff, block.getHeight());
            int randomTrimHeightIncrease; // we will scheduled trim height by random value or by zero
            if (!shardingEnabled || isShardingOff) {
                // non sharded node, schedule next trim event processing randomized and added to current height all the time
                randomTrimHeightIncrease = random.nextInt(trimFrequency);
                log.trace("'Not sharded', trim height random increase = {}", randomTrimHeightIncrease);
            } else {
                // sharded node should 'predict' next shard height and DO NOT randomize in such case
                int trimHeight = (block.getHeight() - maxRollback);
                int shardingFrequency;
                if (!isConfigJustUpdated) {
                    // config didn't change from previous trim scheduling
                    shardingFrequency = currentConfig.getShardingFrequency();
                } else {
                    // config has changed from previous trim scheduling, try to get previous 'shard frequency' value
                    shardingFrequency = blockchainConfig.getPreviousConfig().isPresent() ?
                            blockchainConfig.getPreviousConfig().get().getShardingFrequency() // previous config
                                : currentConfig.getShardingFrequency(); // fall back
                }
                // the boolean - if shard is possible by trim height
                boolean isShardingOnTrimHeight = (Math.max(trimHeight, 0)) % shardingFrequency == 0;
                // the boolean - if shard is possible by current blockchain height
                boolean isShardingOnBlockHeight = block.getHeight() % shardingFrequency == 0;
                // generate pseudo random for 'trim height divergence'
                randomTrimHeightIncrease = generatePositiveIntBiggerThenZero(trimFrequency);
                if (isShardingOnBlockHeight || isShardingOnTrimHeight) {
                    // prevent trim randomization on 'potentially dangerous heights'
                    randomTrimHeightIncrease = 0;
                }
                log.debug("randomTrimHeightIncrease={}, trimHeight = {} / {}, shardFreq={} ({}}), isShardingOnTrimHeight={}, isShardingOnBlockHeight={} {}",
                        randomTrimHeightIncrease, trimHeight, block.getHeight(), shardingFrequency, isConfigJustUpdated, isShardingOnTrimHeight, isShardingOnBlockHeight,
                        randomTrimHeightIncrease < 0 ? "WOW!" : ""); // little 'wow mark' for visual check
            }
            synchronized (lock) {
                scheduleTrimHeight = block.getHeight() - randomTrimHeightIncrease; // decrease next trim height with possible divergence
                log.debug("Schedule next trim for height = {} at {}", scheduleTrimHeight, block.getHeight());
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
