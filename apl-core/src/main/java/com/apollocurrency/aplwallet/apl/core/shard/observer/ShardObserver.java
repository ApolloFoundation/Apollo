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
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class ShardObserver {

    private final BlockchainConfig blockchainConfig;
    private final ShardService shardService;
    private final PropertiesHolder propertiesHolder;
    private Random random;
    private int lastTrimHeight;
    private int randomShardHeightDivergence = 0; // random value number of blocks range
    public static int SHARD_MIN_STEP_BLOCKS = 2000;

    private int generatePositiveIntBiggerThenZero(int trimFrequency) {
        return random.nextInt(trimFrequency - 1) + 1;
    }

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig,
                         ShardService shardService,
                         PropertiesHolder propertiesHolder,
                         Random random) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardService = Objects.requireNonNull(shardService, "shardService is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
        this.random = Objects.requireNonNullElseGet(random, Random::new);
        // random value within 0..500 number of blocks range
        this.randomShardHeightDivergence = generatePositiveIntBiggerThenZero(Constants.DEFAULT_TRIM_FREQUENCY / 2);
        log.debug("random Shard Height Divergence value = {}", randomShardHeightDivergence);
    }

    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int blockHeight = block.getHeight();
        long lastShardHeight = getLastShardHeight();
        Optional<HeightConfig> configNearShardHeight = getTargetConfig(lastShardHeight, blockHeight);
        boolean isShardingEnabled = configNearShardHeight.isPresent() && isShardingEnabled(configNearShardHeight.get());
        NextShardHeightResult timeForShard = isTimeForShard(lastShardHeight, blockHeight, configNearShardHeight);
        log.debug("onBlockPushed: blockHeight={}, configNearShardHeight={}, isShardingEnabled={}, isTimeForShard={}",
            blockHeight, configNearShardHeight, isShardingEnabled, timeForShard);
        if (isShardingEnabled
            && timeForShard.isTimeToDoNextShard()
            && timeForShard.getNextShardHeightValue() != -1L) {
            //calculate target shard height
            int newShardHeight = (int)timeForShard.getNextShardHeightValue();
            log.debug("DO sharding on [{}] for by config = {}, blockHeight={}",
                newShardHeight, configNearShardHeight, blockHeight);
            tryCreateShardAsync(newShardHeight, blockHeight);
        } else {
            log.debug("Sharding is NOT STARTED, enabled by config ? = '{}', timeForShard = {}",
                isShardingEnabled, timeForShard);
        }
    }


    public CompletableFuture<MigrateState> tryCreateShardAsync(int newShardBlockHeight, int currentBlockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        log.debug("tryCreateShardAsync at currentHeight = {} by new/target shardHeight = {}",
            currentBlockchainHeight, newShardBlockHeight);
        completableFuture = shardService.tryCreateShardAsync(newShardBlockHeight, currentBlockchainHeight);
        return completableFuture;
    }


    private boolean isShardingEnabled(HeightConfig currentConfig) {
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        return shardingEnabled && !isShardingOff;
    }

    private NextShardHeightResult isTimeForShard(long lastShardHeight, int blockchainHeight, Optional<HeightConfig> configNearShardHeight) {
        NextShardHeightResult result = new NextShardHeightResult();

        //Q. can we create shard if we late for entire shard frequency?
        //Q. how much blocks we could be late? (frequency - 2) is OK?
        //Q. Do we count on some other parameters like lastTrimBlockHeight?
        if (configNearShardHeight.isEmpty()) {
            log.trace("isTimeForShard(): Sharding is not now. lastShardHeight = {} configNearShardHeight = {}", lastShardHeight, configNearShardHeight);
            return result;
        }
        int shardingFrequency = configNearShardHeight.get().getShardingFrequency();
        long nextShardHeight = -1; // unknown next shard height
        long howLateWeCanBe = -1;
        if (lastShardHeight > 0) { //
            if (lastShardHeight < configNearShardHeight.get().getHeight()) {
                // last shard was created by previous config
                boolean isPreviousShardDisabled = blockchainConfig.getPreviousConfig().isPresent() &&
                    !blockchainConfig.getPreviousConfig().get().isShardingEnabled();
                if (isPreviousShardDisabled) {
                    int configHeight = configNearShardHeight.get().getHeight();
                    nextShardHeight = configHeight + shardingFrequency;
                    howLateWeCanBe = nextShardHeight + randomShardHeightDivergence;
                } else {
                    log.warn("isTimeForShard(): CHECK logic for sharding config processing! lastShardHeight={}, blockchainHeight={}, configNearShardHeight={}",
                        lastShardHeight, blockchainHeight, configNearShardHeight);
                }
            } else {
                nextShardHeight = lastShardHeight + shardingFrequency;
                howLateWeCanBe = nextShardHeight + randomShardHeightDivergence;
            }
        } else {
            // no sharding
            boolean isPreviousShardDisabled = blockchainConfig.getPreviousConfig().isPresent() &&
                !blockchainConfig.getPreviousConfig().get().isShardingEnabled();
            if (isPreviousShardDisabled) { // previous config was disabled
                HeightConfig configHeight = configNearShardHeight.get();
                int probableShardingHeight = blockchainHeight - this.propertiesHolder.MAX_ROLLBACK() - randomShardHeightDivergence;
                if ( configHeight.isShardingEnabled()
                    && probableShardingHeight % configHeight.getShardingFrequency() == 0
                    && probableShardingHeight > configHeight.getHeight() ) {
                    nextShardHeight = probableShardingHeight;
                } else {
                    nextShardHeight = configHeight.getHeight() + shardingFrequency;
                }
            } else {
                nextShardHeight = lastShardHeight + shardingFrequency;
            }
            howLateWeCanBe = nextShardHeight + randomShardHeightDivergence;
        }
        int heightGoodToShard = blockchainHeight - this.propertiesHolder.MAX_ROLLBACK();


        if (heightGoodToShard >= 0) {
            if (heightGoodToShard < howLateWeCanBe) {
                log.warn("isTimeForShard(): Not a shard time at height '{}' blocks! Current blockchainHeight: {}",
                    heightGoodToShard, blockchainHeight);
            } else {
                result.setTimeToDoNextShard(true);
                result.setNextShardHeightValue(nextShardHeight);
                log.debug("isTimeForShard(): Time for sharding is OK. blockchainHeight: {}, nextShardHeight={}, result={}",
                    blockchainHeight, nextShardHeight, result);
            }
        } else {
            log.trace("isTimeForShard(): Sharding is not now. lastTrimHeight: {} nextShardHeight: {}", lastShardHeight, nextShardHeight);
        }
        log.debug("isTimeForShard(): Check shard conditions:  heightGoodToShard = {},  blockchainHeight = {}"
                + ", shardingFrequency = {} Result: {}", heightGoodToShard, blockchainHeight,
            shardingFrequency, result);
        return result;
    }

    private Long getLastShardHeight() {
        long lastShardHeight;
        Shard shard = shardService.getLastShard();
        if (shard == null) {
            log.debug("No last shard yet");
            lastShardHeight = 0;
        } else {
            lastShardHeight = shard.getShardHeight();
            log.debug("Last shard was = {}", lastShardHeight);
        }
        return lastShardHeight;
    }

    private Optional<HeightConfig> getTargetConfig(long lastShardHeight, int blockchainHeight) {
        log.debug("start getTargetConfig(): lastShardHeight = {}, blockchainHeight = {}",
            lastShardHeight, blockchainHeight);
        int heightToShard = blockchainHeight - (this.propertiesHolder.MAX_ROLLBACK() + randomShardHeightDivergence);
        if (heightToShard <= 0) {
            log.debug("getTargetConfig(): heightToShard = {}, lastShardHeight = {}, blockchainHeight = {}",
                heightToShard, lastShardHeight, blockchainHeight);
            return Optional.empty();
        }

        HeightConfig configAtTrimHeight;

        configAtTrimHeight = blockchainConfig.getConfigAtHeight(heightToShard);
        Optional<HeightConfig> previousConfigOptional = blockchainConfig.getPreviousConfigByHeight(heightToShard);
//        Optional<HeightConfig> previousConfigOptional = blockchainConfig.getPreviousConfig(); // it was set up previously
        log.debug("getTargetConfig(): heightToShard={}\nconfigAtTrimHeight = {}\npreviousConfigOptional = {}",
            heightToShard, configAtTrimHeight, previousConfigOptional);
        if (!configAtTrimHeight.isShardingEnabled()) {
            // check if we has to finish sharding from previous config
            if (previousConfigOptional.isPresent()) {
                HeightConfig previousConfig = previousConfigOptional.get();
                if ((lastShardHeight + previousConfig.getShardingFrequency()) == heightToShard) {
                    // take previous config for current (latest) sharding
                    configAtTrimHeight = previousConfig;
                } else {
                    configAtTrimHeight = blockchainConfig.getPreviousConfig().isPresent()
                        && blockchainConfig.getPreviousConfig().get().isShardingEnabled()
                        && (lastShardHeight + blockchainConfig.getPreviousConfig().get().getShardingFrequency()) <= heightToShard
                        && (lastShardHeight + blockchainConfig.getPreviousConfig().get().getShardingFrequency()) <= blockchainConfig.getCurrentConfig().getHeight()
                        ?
                        blockchainConfig.getPreviousConfig().get() // previous config
                        : blockchainConfig.getCurrentConfig(); // fall back to current config
                }
            }
        } else {
            // let check possibly unfinished sharding(s) from previous config
            if (previousConfigOptional.isPresent()
                && previousConfigOptional.get().isShardingEnabled()
                && lastShardHeight != 0
                && (lastShardHeight + previousConfigOptional.get().getShardingFrequency()) <= heightToShard
                && (lastShardHeight + previousConfigOptional.get().getShardingFrequency()) <= configAtTrimHeight.getHeight()) {
                // we didn't finished shard in previous config
                configAtTrimHeight = previousConfigOptional.get();
            }
            // else fallback to 'configAtTrimHeight' (not previous config)
        }

        log.debug("getTargetConfig(): [{}],  lastShardHeight = {}, blockchainHeight = {}, configAtTrimHeight = {}",
            (configAtTrimHeight != null
                && configAtTrimHeight.isShardingEnabled()),
            lastShardHeight, blockchainHeight, configAtTrimHeight
        );
        if (configAtTrimHeight != null && configAtTrimHeight.isShardingEnabled()) {
            log.debug("getTargetConfig(): result = {}", configAtTrimHeight);
            return Optional.of(configAtTrimHeight);
        }
        log.debug("getTargetConfig(): EMPTY result = {}", configAtTrimHeight);
        return Optional.empty();
    }

    @Data
    public class NextShardHeightResult {
        boolean isTimeToDoNextShard = false;
        long nextShardHeightValue = -1L;
    }

}
