/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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

    //no matter how we get signal sync or async, do it async
    public void onTrimDoneAsync(@ObservesAsync @TrimEvent TrimData trimData) {
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
    }

    public void onTrimDone(@Observes @TrimEvent TrimData trimData) {
//do it async anyway because we have to exit from trim and unlock it
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
    }

    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        log.debug("Is sharding enabled GLOBALLY ? : '{}'", !isShardingOff);
        if (!isShardingOff) {
            HeightConfig configAtTrimHeight = blockchainConfig.getConfigAtHeight(lastTrimBlockHeight);
            log.debug("Check shard conditions: ? [{}],  lastTrimBlockHeight = {}, blockchainHeight = {}"
                    + ", configAtTrimHeight = {}",
                (lastTrimBlockHeight != 0
                    && configAtTrimHeight != null
                    && configAtTrimHeight.isShardingEnabled()
                    && lastTrimBlockHeight % configAtTrimHeight.getShardingFrequency() == 0),
                lastTrimBlockHeight, blockchainHeight, configAtTrimHeight
            );
            if (lastTrimBlockHeight != 0
                && configAtTrimHeight != null
                && configAtTrimHeight.isShardingEnabled()
                && lastTrimBlockHeight % configAtTrimHeight.getShardingFrequency() == 0) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
            } else {
                log.debug("No attempt to create new shard lastTrimHeight = {}, configAtTrimHeight = {} (because {})",
                    blockchainHeight, lastTrimBlockHeight, configAtTrimHeight);
            }
        }
        return completableFuture;
    }
}
