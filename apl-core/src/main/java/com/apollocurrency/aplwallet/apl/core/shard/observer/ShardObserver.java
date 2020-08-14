/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.Constants;
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
    private final PropertiesHolder propertiesHolder;
    private int lastTrimHeight;
    public static int SHARD_MIN_STEP_BLOCKS = 2000;

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig, ShardService shardService, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardService = Objects.requireNonNull(shardService, "shardService is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
    }

    public void onTrimDoneAsync(@ObservesAsync @TrimEvent TrimData trimData) {
        lastTrimHeight = trimData.getTrimHeight(); // new code
//        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight()); // original code
    }

    public void onTrimDone(@Observes @TrimEvent TrimData trimData) {
        lastTrimHeight = trimData.getTrimHeight(); // new code
//        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight()); // original code
    }

/*
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
*/

// NEW CODE GOES HERE...
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int blockHeight = block.getHeight();
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        boolean isShardingEnabled = isShardingEnabled(currentConfig);
        boolean isTimeForShard = isTimeForShard(lastTrimHeight, blockHeight/*, currentConfig*/);
        log.debug("onBlockPushed: blockHeight={}, currentConfig={}, isShardingEnabled={}, isTimeForShard={}",
            blockHeight, currentConfig, isShardingEnabled, isTimeForShard);
        if (isShardingEnabled &&
//            blockHeight % SHARD_MIN_STEP_BLOCKS == 0) {
            isTimeForShard) {
            tryCreateShardAsync(lastTrimHeight, blockHeight);
        } else {
            log.debug("Sharding is disabled by config ? = {}, isTimeForShard ? ={}", isShardingEnabled, isTimeForShard);
        }
    }


    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
//        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();

//        if (isShardingEnabled(currentConfig)) {

//            if (isTimeForShard(lastTrimBlockHeight, blockchainHeight/*, currentConfig*/)) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
//            } else {
//                log.debug("No attempt to create new shard at height '{}' (because lastTrimHeight={}), ({})",
//                    blockchainHeight, lastTrimBlockHeight, blockchainConfig.isJustUpdated());
//            }
/*
            // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated() + resetJustUpdated()' usage
            if (blockchainConfig.isJustUpdated()) {
                blockchainConfig.resetJustUpdated(); // reset flag
            }
*/
//        } else {
//            log.debug("Sharding is disabled by config");
//        }
        return completableFuture;
    }


    private boolean isShardingEnabled(HeightConfig currentConfig) {
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        return shardingEnabled && !isShardingOff;
    }

    private Long getLastShardHeight() {
        long lastShardHeight;
        Shard shard = shardService.getLastShard();
        if (shard == null) {
            log.debug("No last shard yet");
            lastShardHeight = 0;
        } else {
            lastShardHeight = shard.getShardHeight();
        }
        return lastShardHeight;
    }

    private HeightConfig getShardingFrequency(/*HeightConfig currentConfig, */int lastTrimBlockHeight, int blockchainHeight) {
        int shardingFrequency = 0;
/*
        if (!blockchainConfig.isJustUpdated()) {
            // config didn't change from previous trim scheduling
            shardingFrequency = currentConfig.getShardingFrequency();
        } else {
            // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated()' usage and checking
            // config has changed from previous trim scheduling, try to get previous 'shard frequency' value
            shardingFrequency = blockchainConfig.getPreviousConfig().isPresent()
                && blockchainConfig.getPreviousConfig().get().isShardingEnabled()
                ? blockchainConfig.getPreviousConfig().get().getShardingFrequency() // previous config
                : currentConfig.getShardingFrequency(); // fall back
        }
*/
        int heightToShard = blockchainHeight - this.propertiesHolder.MAX_ROLLBACK();
//        HeightConfig configAtTrimHeight = blockchainConfig.getConfigAtHeight(lastTrimBlockHeight);
        HeightConfig configAtTrimHeight = blockchainConfig.getConfigAtHeight(heightToShard);
        log.debug("Check shard conditions: ? [{}],  lastTrimBlockHeight = {}, blockchainHeight = {}"
                + ", configAtTrimHeight = {}",
            (lastTrimBlockHeight != 0
                && configAtTrimHeight != null
                && configAtTrimHeight.isShardingEnabled()
                /*&& lastTrimBlockHeight % configAtTrimHeight.getShardingFrequency() == 0*/),
            lastTrimBlockHeight, blockchainHeight, configAtTrimHeight
        );
//        shardingFrequency = configAtTrimHeight.getShardingFrequency();
//        return shardingFrequency;
        return configAtTrimHeight;
    }

/**
     * Well, it is subject of discussion yet what parameter should trigger
     * sharding At the moment sharding is bound to trims and we do not count on
     * blockchainHeight at all
     *
     * @param lastTrimBlockHeight
     * @param blockchainHeight
     * @param currentConfig
     * @return
     */

    private boolean isTimeForShard(int lastTrimBlockHeight, int blockchainHeight/*, HeightConfig currentConfig*/) {

        HeightConfig configNearShardHeight = getShardingFrequency(lastTrimBlockHeight, blockchainHeight);
        //Q. can we create shard if we late for entire shard frequency?
        //Q. how much blocks we could be late? (frequency - 2) is OK?
        //Q. Do we count on some other parameters like lastTrimBlockHeight?
        long lastShardHeight = getLastShardHeight();
        int shardingFrequency = configNearShardHeight.getShardingFrequency();
        long howLateWeCanBe = shardingFrequency - 2;
        long nextShardHeight = lastShardHeight + shardingFrequency;
        int howLateWeAre = blockchainHeight - this.propertiesHolder.MAX_ROLLBACK();
//        long howLateWeAre = blockchainHeight - Constants.MAX_AUTO_ROLLBACK * 2 - nextShardHeight;
        boolean res = false;

        if (howLateWeAre >= 0) {
            if (howLateWeAre > howLateWeCanBe) {
                log.warn("We have missed shard for {} blocks! lastTrimHeight: {} blockchainHeight: {}",
                    howLateWeAre, lastTrimBlockHeight, blockchainHeight);
            } else {
                res = true;
                log.debug("Time for sharding is OK. lastTrimHeight: {} blockchainHeight: {}",
                    lastTrimBlockHeight, blockchainHeight);
            }
        } else {
            log.trace("Sharding is not now. lastTrimHeight: {} nextShardHeight: {}", lastShardHeight, nextShardHeight);
        }

        log.debug("Check shard conditions:  howLateWeAre = {},  lastTrimBlockHeight = {}, blockchainHeight = {}"
                + ", shardingFrequency = {} justUpdted: {} Result: {}",
            howLateWeAre, lastTrimBlockHeight, blockchainHeight,
            shardingFrequency, blockchainConfig.isJustUpdated(), res);

        return res;
    }

}
