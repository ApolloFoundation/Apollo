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

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig, ShardService shardService) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardService = Objects.requireNonNull(shardService, "shardService is NULL");
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
        if (currentConfig.isShardingEnabled()) {
            log.debug("LastTrimHeight-{}, blockchainHeight - {}", lastTrimBlockHeight, blockchainHeight);
            if (lastTrimBlockHeight != 0 && lastTrimBlockHeight % currentConfig.getShardingFrequency() == 0) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
            }
        }
        return completableFuture;
    }
}
