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
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Singleton
public class ShardObserver {

    private final BlockchainConfig blockchainConfig;
    private final ShardService shardService;
    private final PropertiesHolder propertiesHolder;
    private Random random;
    private int randomShardHeightDivergence = 0; // random value number of blocks range
    private final ReentrantLock lock = new ReentrantLock();

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

    /**
     * Triggered on every block
     * @param block to be processed and sharding started when conditions are met
     */
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        int blockHeight = block.getHeight();
        long lastShardHeight = getLastShardHeight();
        Optional<HeightConfig> configNearShardHeight;
        boolean isShardingEnabled;
        NextShardHeightResult timeForShard;
        lock.lock();
        try {
            configNearShardHeight = getTargetConfig(lastShardHeight, blockHeight);
            isShardingEnabled = configNearShardHeight.isPresent() && isShardingEnabled(configNearShardHeight.get());
            timeForShard = computeNextShardTimeAndHeight(lastShardHeight, blockHeight, configNearShardHeight);
            log.trace("onBlockPushed: blockHeight={}, configNearShardHeight={}, isShardingEnabled={}, isTimeForShard={}",
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
                log.trace("Sharding is NOT STARTED, enabled by config ? = '{}', timeForShard = {}",
                    isShardingEnabled, timeForShard);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start asynch sharding process
     * @param newShardBlockHeight new target sharding height value
     * @param currentBlockchainHeight current height
     * @return completable future
     */
    public CompletableFuture<MigrateState> tryCreateShardAsync(int newShardBlockHeight, int currentBlockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        log.debug("tryCreateShardAsync at currentHeight = {} by new/target shardHeight = {}",
            currentBlockchainHeight, newShardBlockHeight);
        completableFuture = shardService.tryCreateShardAsync(newShardBlockHeight, currentBlockchainHeight);
        return completableFuture;
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
     * Check if sharding can be started and prepare data for it.
     * @param lastShardHeight latest shard height
     * @param currentBlockHeight current blockchain height
     * @param configNearShardHeight configuration previously selected as main for sharding height
     * @return prepared data with real sharding data OR 'default' values meaning sharding can't be executed
     */
    private NextShardHeightResult computeNextShardTimeAndHeight(long lastShardHeight, int currentBlockHeight,
                                                                Optional<HeightConfig> configNearShardHeight) {
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
        log.trace("isTimeForShard() check lastShardHeight = {} configNearShardHeight = {}, currentBlockHeight={}",
            lastShardHeight, configNearShardHeight, currentBlockHeight);
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
                    // trying to compute shard height by division on frequency
                    HeightConfig configHeight = configNearShardHeight.get();
                    int probableShardingHeight = currentBlockHeight - this.propertiesHolder.MAX_ROLLBACK() - randomShardHeightDivergence;
                    log.trace("probableShardingHeight (prev shard): {} = ({} - {} - {})\n{}",
                        probableShardingHeight, currentBlockHeight,  this.propertiesHolder.MAX_ROLLBACK(),
                        randomShardHeightDivergence, configHeight);

                    if ( configHeight.isShardingEnabled()
                        && probableShardingHeight % configHeight.getShardingFrequency() == 0
                        && probableShardingHeight > configHeight.getHeight() ) {
                        log.trace("probable match on {}: ? ({} / {} / {}), currentBlockHeight = {}",
                            probableShardingHeight, configHeight.isShardingEnabled(),
                            probableShardingHeight % configHeight.getShardingFrequency() == 0,
                            probableShardingHeight > configHeight.getHeight(),
                            currentBlockHeight);

                        nextShardHeight = probableShardingHeight;
                    } else {
                        // no fall back is possible here
                        log.debug("unmatched on {}: CHECK logic for sharding config processing! lastShardHeight={}, " +
                                "currentBlockHeight={}, configNearShardHeight={}",
                            probableShardingHeight, lastShardHeight, currentBlockHeight, configNearShardHeight);
                    }
                }
            } else {
                nextShardHeight = lastShardHeight + shardingFrequency;
                howLateWeCanBe = nextShardHeight + randomShardHeightDivergence;
            }
        } else {
            // no sharding
            boolean isPreviousShardDisabled = blockchainConfig.getPreviousConfig().isPresent() &&
                !blockchainConfig.getPreviousConfig().get().isShardingEnabled();
            log.trace("No Sharding case: (?='{}') prev= {}, currentBlockHeight={}",
                isPreviousShardDisabled, blockchainConfig.getPreviousConfig(), currentBlockHeight);
            if (isPreviousShardDisabled) {
                // previous config was disabled
                HeightConfig configHeight = configNearShardHeight.get();
                int probableShardingHeight = currentBlockHeight - this.propertiesHolder.MAX_ROLLBACK() - randomShardHeightDivergence;
                log.trace("probableShardingHeight (no prev shard): {} = ({} - {} - {})\n{}",
                    probableShardingHeight, currentBlockHeight,  this.propertiesHolder.MAX_ROLLBACK(),
                    randomShardHeightDivergence, configHeight);

                if ( configHeight.isShardingEnabled()
                    && probableShardingHeight % configHeight.getShardingFrequency() == 0
                    && probableShardingHeight > configHeight.getHeight() ) {
                    log.trace("probable match on {}: ? ({} / {} / {}), currentBlockHeight = {}",
                        probableShardingHeight, configHeight.isShardingEnabled(),
                        probableShardingHeight % configHeight.getShardingFrequency() == 0,
                        probableShardingHeight > configHeight.getHeight(),
                        currentBlockHeight);

                    nextShardHeight = probableShardingHeight;
                } else {
                    // fall back
                    nextShardHeight = configHeight.getHeight() + shardingFrequency;
                    log.trace("unmatched on {}: currentBlockHeight={}, configHeight.getHeight() = {}, shardingFrequency = {})",
                        probableShardingHeight, currentBlockHeight, configHeight.getHeight(), shardingFrequency);
                }
            } else {
                nextShardHeight = lastShardHeight + shardingFrequency;
                log.trace("No Sharding, Prev DISABLED case: nextShardHeight={}, lastShardHeight = {}, shardingFrequency = {}, currentBlockHeight={}",
                    nextShardHeight, lastShardHeight, shardingFrequency, currentBlockHeight);
            }
            howLateWeCanBe = nextShardHeight + randomShardHeightDivergence;
        }
        int heightGoodToShard = currentBlockHeight - this.propertiesHolder.MAX_ROLLBACK();


        if (heightGoodToShard >= 0) {
            if (heightGoodToShard < howLateWeCanBe) {
                log.warn("isTimeForShard(): Not a shard time at height '{}' blocks! Current currentBlockHeight: {} (howLateWeCanBe={})",
                    heightGoodToShard, currentBlockHeight, howLateWeCanBe);
            } else {
                // real sharding data is prepared here !!
                result.setTimeToDoNextShard(true);
                result.setNextShardHeightValue(nextShardHeight);
                log.debug("isTimeForShard(): Time for sharding is OK. currentBlockHeight: {}, nextShardHeight={}, result={}",
                    currentBlockHeight, nextShardHeight, result);
            }
        } else {
            log.trace("isTimeForShard(): Sharding is not now. lastTrimHeight: {} nextShardHeight: {}", lastShardHeight, nextShardHeight);
        }
        log.debug("isTimeForShard(): Check shard conditions:  heightGoodToShard = {},  currentBlockHeight = {}"
                + ", shardingFrequency = {} Result: {}", heightGoodToShard, currentBlockHeight,
            shardingFrequency, result);
        return result;
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

    /**
     * Select current and previous config and make a decision about which one should be used main for calculation
     * @param lastShardHeight latest shard height from database
     * @param currentBlockHeight current blockchain height
     * @return optional configuration
     */
    private Optional<HeightConfig> getTargetConfig(long lastShardHeight, int currentBlockHeight) {
        log.trace("start getTargetConfig(): lastShardHeight = {}, currentBlockHeight = {}",
            lastShardHeight, currentBlockHeight);
        int heightToShard = currentBlockHeight - (this.propertiesHolder.MAX_ROLLBACK() + randomShardHeightDivergence);
        if (heightToShard <= 0) {
            log.trace("getTargetConfig(): heightToShard = {}, lastShardHeight = {}, currentBlockHeight = {}",
                heightToShard, lastShardHeight, currentBlockHeight);
            return Optional.empty();
        }

        HeightConfig configAtTrimHeight;

        configAtTrimHeight = blockchainConfig.getConfigAtHeight(heightToShard);
        Optional<HeightConfig> previousConfigOptional = blockchainConfig.getPreviousConfigByHeight(heightToShard); // preferred way
//        Optional<HeightConfig> previousConfigOptional = blockchainConfig.getPreviousConfig(); // it was set up previously, work worse
        log.trace("getTargetConfig(): heightToShard={}\nconfigAtTrimHeight = {}\npreviousConfigOptional = {}",
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

        log.trace("getTargetConfig(): [{}],  lastShardHeight = {}, currentBlockHeight = {}, configAtTrimHeight = {}",
            (configAtTrimHeight != null
                && configAtTrimHeight.isShardingEnabled()),
            lastShardHeight, currentBlockHeight, configAtTrimHeight
        );
        if (configAtTrimHeight != null && configAtTrimHeight.isShardingEnabled()) {
            log.debug("getTargetConfig(): result = {}", configAtTrimHeight);
            return Optional.of(configAtTrimHeight);
        }
        log.trace("getTargetConfig(): EMPTY result = {}", configAtTrimHeight);
        return Optional.empty();
    }

    @Data
    public class NextShardHeightResult {
        boolean isTimeToDoNextShard = false;
        long nextShardHeightValue = -1L;
    }

}
