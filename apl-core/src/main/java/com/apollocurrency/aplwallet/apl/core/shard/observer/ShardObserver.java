/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShardObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

    private final BlockchainConfig blockchainConfig;
    private final ShardService shardService;
    private PropertiesHolder propertiesHolder;

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig, ShardService shardService, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardService = Objects.requireNonNull(shardService, "shardService is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
    }


    public void onTrimDoneAsync(@Observes @Async TrimData trimData) {
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
    }

    public void onTrimDone(@Observes @Sync TrimData trimData) {
        try {
            CompletableFuture<MigrateState> future = tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
            if (future != null) {
                future.get();
            }
        }
        catch (InterruptedException | ExecutionException e) {
            log.error(e.toString(), e);
        }
    }

    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        int shardingFrequency = 1;
        if (shardingEnabled && !isShardingOff) {
            if (!blockchainConfig.isJustUpdated()) {
                // config didn't change from previous trim scheduling
                shardingFrequency = currentConfig.getShardingFrequency();
            } else {
                // config has changed from previous trim scheduling, try to get previous 'shard frequency' value
                shardingFrequency = blockchainConfig.getPreviousConfig().isPresent() ?
                        blockchainConfig.getPreviousConfig().get().getShardingFrequency() // previous config
                        : currentConfig.getShardingFrequency(); // fall back
            }
            log.debug("Check shard conditions: ? [{}],  lastTrimBlockHeight = {}, blockchainHeight = {}"
                    + ", shardingFrequency = {} ({})",
                    shardingFrequency != 0 ?
                            lastTrimBlockHeight % shardingFrequency == 0 : "zeroDivision",
                    lastTrimBlockHeight, blockchainHeight,
                    shardingFrequency, blockchainConfig.isJustUpdated());
            if (lastTrimBlockHeight != 0 && lastTrimBlockHeight % shardingFrequency == 0) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
            } else {
                log.debug("No attempt to create new shard at height '{}' (because lastTrimHeight={}), ({})",
                        blockchainHeight, lastTrimBlockHeight, blockchainConfig.isJustUpdated());
            }
            if (blockchainConfig.isJustUpdated()) {
                blockchainConfig.resetJustUpdated(); // reset flag
            }
        } else {
            log.debug("Sharding is disabled on node : {} && {}", shardingEnabled, isShardingOff);
        }
        return completableFuture;
    }
}
