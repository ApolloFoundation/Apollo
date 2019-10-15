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

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.toString(), e);
        }
    }

    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        if (shardingEnabled && !isShardingOff) {
            log.debug("Check shard conditions: ? [{}],  lastTrimBlockHeight = {}, blockchainHeight = {}"
                            + ", shardingFrequency = {}",
                    currentConfig.getShardingFrequency() != 0 ?
                            lastTrimBlockHeight % currentConfig.getShardingFrequency() == 0 : "zeroDivision",
                    lastTrimBlockHeight, blockchainHeight,
                    currentConfig.getShardingFrequency());
            if (lastTrimBlockHeight != 0 && lastTrimBlockHeight % currentConfig.getShardingFrequency() == 0) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
            } else {
                log.debug("No attempt to create new shard at height '{}'", blockchainHeight);
            }
        } else {
            log.debug("Sharding is disabled on node : {} && {}", shardingEnabled, isShardingOff);
        }
        return completableFuture;
    }
}
