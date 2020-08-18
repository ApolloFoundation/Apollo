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
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ShardObserver {

    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

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
        this.randomShardHeightDivergence =  generatePositiveIntBiggerThenZero(Constants.DEFAULT_TRIM_FREQUENCY / 2);
    }

    public void onTrimDoneAsync(@ObservesAsync @TrimEvent TrimData trimData) {
        lastTrimHeight = trimData.getTrimHeight(); // new code
        log.debug("FIRED lastTrimHeight = {}", lastTrimHeight);
//        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight()); // original code
    }

    public void onTrimDone(@Observes @TrimEvent TrimData trimData) {
        lastTrimHeight = trimData.getTrimHeight(); // new code
        log.debug("FIRED Async lastTrimHeight = {}", lastTrimHeight);
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
        long lastShardHeight = getLastShardHeight();
        boolean isTimeForShard = isTimeForShard(lastShardHeight, blockHeight);
        log.debug("onBlockPushed: blockHeight={}, currentConfig={}, isShardingEnabled={}, isTimeForShard={}",
            blockHeight, currentConfig, isShardingEnabled, isTimeForShard);
        if (isShardingEnabled && isTimeForShard) {
            //calculate target shard height
            Optional<HeightConfig> height = getTargetConfig(lastShardHeight, blockHeight);
            if (height.isPresent()) {
                int newShardHeight = lastTrimHeight + height.get().getShardingFrequency();
                tryCreateShardAsync(newShardHeight, blockHeight);
            } else {
                log.debug("Height or calculation was incorrect for : lastShardHeight = {}, blockHeight = {}",
                    lastShardHeight, blockHeight);
            }
        } else {
            log.debug("Sharding is disabled by config ? = {}, isTimeForShard ? ={}", isShardingEnabled, isTimeForShard);
        }
    }


    public CompletableFuture<MigrateState> tryCreateShardAsync(int newShardBlockHeight, int currentBlockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        log.debug("tryCreateShardAsync at currentHeight = {} by targetNewShardHeight = {}",
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

    private boolean isTimeForShard(long lastShardHeight, int blockchainHeight) {
        boolean res = false;

        //Q. can we create shard if we late for entire shard frequency?
        //Q. how much blocks we could be late? (frequency - 2) is OK?
        //Q. Do we count on some other parameters like lastTrimBlockHeight?
        Optional<HeightConfig> configNearShardHeight = getTargetConfig(lastShardHeight, blockchainHeight);
        if (configNearShardHeight.isEmpty()) {
            log.trace("Sharding is not now. lastShardHeight = {} nextShardHeight = {}", lastShardHeight, configNearShardHeight);
            return res;
        }
        int shardingFrequency = configNearShardHeight.get().getShardingFrequency();
        long howLateWeCanBe = shardingFrequency - randomShardHeightDivergence ;
        long nextShardHeight = lastShardHeight + shardingFrequency;
        int howLateWeAre = blockchainHeight - (this.propertiesHolder.MAX_ROLLBACK() + randomShardHeightDivergence);


        if (howLateWeAre >= 0) {
            if (howLateWeAre > howLateWeCanBe) {
                log.warn("We have missed shard for '{}' blocks! blockchainHeight: {}",
                    howLateWeAre, blockchainHeight);
            } else {
                res = true;
                log.debug("Time for sharding is OK. blockchainHeight: {}", blockchainHeight);
            }
        } else {
            log.trace("Sharding is not now. lastTrimHeight: {} nextShardHeight: {}", lastShardHeight, nextShardHeight);
        }
        log.debug("Check shard conditions:  howLateWeAre = {},  blockchainHeight = {}"
                + ", shardingFrequency = {} Result: {}", howLateWeAre, blockchainHeight,
            shardingFrequency, res);
        return res;
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

    private Optional<HeightConfig> getTargetConfig(long lastShardHeight, int blockchainHeight) {
        int heightToShard = blockchainHeight - (this.propertiesHolder.MAX_ROLLBACK() + randomShardHeightDivergence);
        if (lastShardHeight <= 0 || heightToShard <= 0) {
            log.debug("heightToShard = {}, lastShardHeight = {}, blockchainHeight = {}", heightToShard, lastShardHeight, blockchainHeight);
            return Optional.empty();
        }
        HeightConfig configAtTrimHeight = blockchainConfig.getConfigAtHeight(heightToShard);
        log.debug("Check shard conditions: ? [{}],  lastShardHeight = {}, blockchainHeight = {}"
                + ", configAtTrimHeight = {}",
            (configAtTrimHeight != null
                && configAtTrimHeight.isShardingEnabled()),
            lastShardHeight, blockchainHeight, configAtTrimHeight
        );
        if (configAtTrimHeight.isShardingEnabled()) {
            return Optional.of(configAtTrimHeight);
        }
        return Optional.empty();
    }

}
