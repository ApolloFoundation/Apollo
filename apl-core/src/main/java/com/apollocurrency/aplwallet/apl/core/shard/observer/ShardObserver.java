/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardingScheduler;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Slf4j
@Singleton
public class ShardObserver {

    private final BlockchainConfig blockchainConfig;
    private final ShardingScheduler shardingScheduler;
    private final PropertiesHolder propertiesHolder;
    private final ShardService shardService;

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig,
                         PropertiesHolder propertiesHolder,
                         ShardingScheduler shardingScheduler, ShardService shardService) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.shardingScheduler = Objects.requireNonNull(shardingScheduler, "Sharding scheduler is NULL");
        this.shardService = shardService;
    }

    /**
     * Triggered on every block
     * @param block to be processed and sharding started when conditions are met
     */
    public void onBlockPushed(@Priority(javax.interceptor.Interceptor.Priority.APPLICATION) @Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int blockHeight = block.getHeight();
        long lastShardHeight = getLastShardHeight();
        int heightForSharding = blockHeight - propertiesHolder.MAX_ROLLBACK();
        if (heightForSharding <= 0) {
            return;
        }
        if (heightForSharding <= lastShardHeight) {
            return;
        }
        HeightConfig heightConfig = blockchainConfig.getConfigAtHeight(heightForSharding - 1);
        boolean isShardingEnabled = isShardingEnabled(heightConfig);
        boolean isTimeForSharding = heightForSharding % heightConfig.getShardingFrequency() == 0;
        if (isShardingEnabled && isTimeForSharding) {
            shardingScheduler.scheduleSharding(heightForSharding, blockHeight);
        }
    }

    /**
     * Return flag is sharding is possible
     * @param currentConfig current config
     * @return true if possible, false otherwise
     */
    private boolean isShardingEnabled(HeightConfig currentConfig) {
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.trace("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        return shardingEnabled && !isShardingOff;
    }

    /**
     * Get latest shard height
     * @return sharding height
     */
    private Long getLastShardHeight() {
        long lastShardHeight;
        Shard shard = shardService.getLastShard();
        if (shard == null) {
            log.trace("No last shard yet");
            lastShardHeight = 0;
        } else {
            lastShardHeight = shard.getShardHeight();
            log.trace("Last shard was = {}", lastShardHeight);
        }
        return lastShardHeight;
    }

}
