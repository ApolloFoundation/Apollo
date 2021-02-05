/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShardingScheduler {
    private static final int DEFAULT_SHARD_DELAY_MS = 1000;
    private volatile Random random = new Random();
    private final Queue<ShardScheduledRecord> scheduledShards = new PriorityQueue<>(Comparator.comparing(ShardScheduledRecord::getShardHeight)); // will sort heights from lowest to highest automatically
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-shard-scheduler"));
    private final Event<TrimConfig> trimEvent;
    private final ShardService shardService;
    private final BlockchainConfig blockchainConfig;
    private volatile boolean shardingFailed = true;
    private final ShardSchedulingConfig config;
    private final TimeService timeService;


    @Inject
    public ShardingScheduler(Event<TrimConfig> trimEvent,
                             ShardService shardService,
                             BlockchainConfig blockchainConfig,
                             ShardSchedulingConfig config,
                             TimeService timeService
    ) {
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
        this.config = config;
        this.trimEvent = trimEvent;
        this.shardService = shardService;
    }

    void setRandom(Random random) {
        this.random = random;
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    private void trySharding() {
        ShardScheduledRecord shardRecord = null;
        long nextShardDelay = DEFAULT_SHARD_DELAY_MS;
        try {
            synchronized (this) {
                if (!scheduledShards.isEmpty()) {
                    ShardScheduledRecord firstRecord = scheduledShards.element();
                    long scheduledTime = firstRecord.timeDelay + firstRecord.schedulingTime;
                    long currentTime = timeService.systemTimeMillis();
                    if (currentTime >= scheduledTime) {
                        shardRecord = firstRecord;
                        scheduledShards.remove();
                    } else {
                        nextShardDelay = scheduledTime - currentTime;
                    }
                }
            }
            if (shardRecord != null) {
                CompletableFuture<MigrateState> shardingProcess = shardService.tryCreateShardAsync(shardRecord.shardHeight, shardRecord.blockchainHeight);
                if (shardingProcess == null) {
                    logErrorAndDisableSharding("Error in shard scheduling process. Sharding may be started earlier or blockchain is scanning; shard height " + shardRecord.shardHeight);
                    return;
                }
                shardingProcess.join();
                Shard lastShard = shardService.getLastShard();
                if (lastShard == null) {
                    logErrorAndDisableSharding("After sharding process at height {}, no shards exist in shard table", shardRecord.shardHeight);
                    return;
                }
                if (lastShard.getShardState() != ShardState.FULL) {
                    logErrorAndDisableSharding("Last sharding process at height {] was not finished. Will not try to create new shards", shardRecord.shardHeight);
                    return;
                }

                synchronized (this) {
                    if (scheduledShards.isEmpty()) {
                        updateTrimConfig(true, true);

                    } else {
                        for (ShardScheduledRecord record : scheduledShards) {
                            record.schedulingTime = timeService.systemTimeMillis();
                        }
                    }
                }
            }
        } finally {
           scheduleBackgroundShardingTask(nextShardDelay);
        }
    }

    void scheduleBackgroundShardingTask(long delay) {
        executorService.schedule(this::trySharding, delay, TimeUnit.MILLISECONDS);
    }

    public List<ShardScheduledRecord> scheduledShardings() {
        return new ArrayList<>(scheduledShards);
    }

    private void updateTrimConfig(boolean enableTrim, boolean clearQueue) {
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(enableTrim, clearQueue));
    }


    public void init(int lastBlockHeight, int lastTrimBlockchainHeight) {
        if (!config.isCreateShards()) {
            return;
        }
        Integer lastShardHeight = 0;
        Shard shard = shardService.getLastShard();
        if (shard != null) {
            lastShardHeight = shard.getShardHeight();
            if (shard.getShardState() == ShardState.INIT || shard.getShardState() == ShardState.IN_PROGRESS) {
                logErrorAndDisableSharding("Last sharding process was failed, will not try to create all delayed shards");
                return;
            }
        }
        int trimHeight = Math.max(lastTrimBlockchainHeight, lastShardHeight + 1);
        int maxAllowedShardHeight = lastBlockHeight - config.getMaxRollback();

        List<HeightConfig> allHeightConfigs = blockchainConfig.getAllActiveConfigsBetweenHeights(lastShardHeight, maxAllowedShardHeight + 1);
        int searchHeight = lastShardHeight + 1;
        for (int i = 0; i < allHeightConfigs.size(); i++) {
            HeightConfig currentConfig = allHeightConfigs.get(i);
            int activeToHeight = lastBlockHeight;
            if (i + 1 < allHeightConfigs.size()) { // next config
                activeToHeight = allHeightConfigs.get(i + 1).getHeight();
            }
            if (!currentConfig.isShardingEnabled()) {
                searchHeight = activeToHeight + 1;
                continue;
            }
            while (true) {
                int nextShardHeight = nextShardHeight(searchHeight, activeToHeight, currentConfig.getShardingFrequency());
                if (nextShardHeight == -1) {
                    break;
                }
                if (nextShardHeight < trimHeight) {
                    logErrorAndDisableSharding("Next shard should be created at height {}, but last trim was at height {}", nextShardHeight, trimHeight);
                }
                synchronized (this) {
                    log.info("Schedule new sharding at height {}", nextShardHeight);
                    scheduledShards.add(new ShardScheduledRecord(0, nextShardHeight, lastBlockHeight, timeService.systemTimeMillis()));
                    updateTrimConfig(false, false);
                }
                searchHeight = nextShardHeight + 1;
            }
            searchHeight = activeToHeight + 1;
        }
        scheduleBackgroundShardingTask(0);
    }

    private int nextShardHeight(int fromHeight, int toHeight, int frequency) {
        int targetHeight = fromHeight + (frequency - fromHeight % frequency);
        if (targetHeight > toHeight) {
            return -1;
        }
        return targetHeight;
    }

    public void onBlockPushed(@Priority(javax.interceptor.Interceptor.Priority.APPLICATION) @Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int blockHeight = block.getHeight();
        long lastShardHeight = getLastShardHeight();
        int heightForSharding = blockHeight - config.getMaxRollback();
        if (heightForSharding <= 0) {
            return;
        }
        if (heightForSharding <= lastShardHeight) {
            return;
        }
        HeightConfig heightConfig = blockchainConfig.getConfigAtHeight(heightForSharding - 1);
        boolean isShardingEnabled = heightConfig.isShardingEnabled() && !shardingFailed && config.isCreateShards() ;
        boolean isTimeForSharding = heightForSharding % heightConfig.getShardingFrequency() == 0;
        if (isShardingEnabled && isTimeForSharding) {
            scheduleSharding(heightForSharding, blockHeight);
        }
    }

    private Long getLastShardHeight() {
        long lastShardHeight = 0;
        Shard shard = shardService.getLastShard();
        if (shard != null) {
            lastShardHeight = shard.getShardHeight();
        }
        return lastShardHeight;
    }

    private synchronized void scheduleSharding(int height, int blockchainHeight) {
        ShardScheduledRecord record = new ShardScheduledRecord(shardingDelayMs(), height, blockchainHeight, System.currentTimeMillis());
        scheduledShards.add(record);
        log.info("Schedule new shard creation at height {}, blockchain height {}, delay {} min", height, blockchainHeight, record.timeDelay / 60 / 1000);
        updateTrimConfig(false, true);
    }

    private long shardingDelayMs() {
        if (config.shardDelayed()) {
            return 1000 * 60 * random.nextInt(config.getMaxDelay() - config.getMinDelay() + 1) + config.getMinDelay() ;
        } else {
            return 0;
        }
    }

    private void logErrorAndDisableSharding(String error, Object... args) {
        log.error(error, args);
        shardingFailed = true;
        synchronized (this) {
            scheduledShards.clear();
            updateTrimConfig(true, false);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ShardScheduledRecord {
        private long timeDelay;
        private int shardHeight;
        private int blockchainHeight;
        private long schedulingTime;
    }

}
