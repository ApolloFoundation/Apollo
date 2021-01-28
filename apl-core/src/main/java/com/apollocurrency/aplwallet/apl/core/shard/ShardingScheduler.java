/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
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
    private static final Random random = new Random();
    private final Queue<ShardScheduledRecord> shardHeights = new PriorityQueue<>(Comparator.comparing(ShardScheduledRecord::getShardHeight)); // will sort heights from lowest to highest automatically
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-shard-scheduler"));
    private final Event<TrimConfig> trimEvent;
    private final ShardService shardService;
    private final BlockchainConfig blockchainConfig;
    private volatile boolean shardingFailed = true;
    private final ShardSchedulingConfig config;


    @Inject
    public ShardingScheduler(Event<TrimConfig> trimEvent,
                             ShardService shardService,
                             BlockchainConfig blockchainConfig,
                             ShardSchedulingConfig config
    ) {
        this.blockchainConfig = blockchainConfig;
        this.config = config;
        this.trimEvent = trimEvent;
        this.shardService = shardService;
    }

    @PostConstruct
    void init() {
        if (config.isCreateShards()) {
            executorService.scheduleWithFixedDelay(this::trySharding, 30_000, 30_000, TimeUnit.MILLISECONDS);
        }
    }

    private void trySharding() {
        ShardScheduledRecord shardRecord = null;
        synchronized (this) {
            if (!shardHeights.isEmpty()) {
                ShardScheduledRecord firstRecord = shardHeights.element();
                if (System.currentTimeMillis() < firstRecord.timeDelay + firstRecord.schedulingTime) {
                    shardRecord = firstRecord;
                    shardHeights.remove();
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
                if (shardHeights.isEmpty()) {
                    updateTrimConfig(true, true);
                } else {
                    for (ShardScheduledRecord record : shardHeights) {
                        record.schedulingTime = System.currentTimeMillis();
                    }
                }
            }
        }
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
        int trimHeight = Math.max(lastTrimBlockchainHeight - config.getMaxRollback(), lastShardHeight + 1);

        List<HeightConfig> allHeightConfigs = blockchainConfig.getAllActiveConfigsBetweenHeights(lastShardHeight, lastBlockHeight);
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

            while (searchHeight > activeToHeight) {
                if (searchHeight % currentConfig.getShardingFrequency() == 0) {
                    synchronized (this) {
                        shardHeights.add(new ShardScheduledRecord())
                    }
                }
            }
            searchHeight = activeToHeight + 1;
        }
    }

    public synchronized void scheduleSharding(int height, int blockchainHeight) {
        if (!shardingFailed && config.isCreateShards()) {
            shardHeights.add();
            updateTrimConfig(false, true);
        } else {
            log.warn("Sharding is disabled, last shard failed '{}', disabled by props '{}'", shardingFailed, !config.isCreateShards());
        }
    }

    private ShardScheduledRecord record(int shardHeight, int blockchainHeight) {
        return new ShardScheduledRecord(random.nextInt(3000 * 1000) + 600 * 1000, shardHeight, blockchainHeight, System.currentTimeMillis());
    }

    private void logErrorAndDisableSharding(String error, Object... args) {
        log.error(error, args);
        shardingFailed = true;
        synchronized (this) {
            shardHeights.clear();
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
