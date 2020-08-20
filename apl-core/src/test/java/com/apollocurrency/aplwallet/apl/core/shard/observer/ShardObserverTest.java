/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ShardObserverTest {
    public static final int DEFAULT_SHARDING_FREQUENCY = 5_000;
    public static final int DEFAULT_TRIM_HEIGHT = 100_000;
    private static final String CONFIG_NAME = "another-list-for-shar-observer-config.json";
    private ChainsConfigLoader chainsConfigLoader;
    private Map<UUID, Chain> loadedChains;
    private Chain chain;

    BlockchainConfig blockchainConfig;
    @Mock
    ShardService shardService;
    @Mock
    BlockDao blockDao;
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class, withSettings().lenient());
    @Mock
    private Random random;
    BlockTestData td = new BlockTestData();
    private ShardObserver shardObserver;
    private BlockchainConfigUpdater blockchainConfigUpdater;

    @BeforeEach
    void setUp() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");
        doReturn(3).when(propertiesHolder).MAX_ROLLBACK(); // max rollback = 3
        doReturn(2).when(random).nextInt(any(Integer.class)); // random shard delay = 2
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, propertiesHolder);
        shardObserver = new ShardObserver(blockchainConfig, shardService, propertiesHolder, random);
        blockchainConfigUpdater = new BlockchainConfigUpdater(blockchainConfig, blockDao);
    }

    @ParameterizedTest
    @MethodSource("supplyTestData")
    void testStartShardingByConfig(int latestShardHeight, int currentBlockHeight,
                                   int targetShardHeight,
                                   boolean isShouldBeCalled,
                                   boolean isSwitchedToNewConfigHeight) {
        // prepare tes data
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        blockchainConfigUpdater.onBlockPopped(block); // simulate setting 'current' and 'previous' config
        if (!isSwitchedToNewConfigHeight) {
            blockchainConfig.resetJustUpdated(); // reset flag set by default inside onBlockPopped(block)
        }

        shardObserver.onBlockPushed(block);

        if (isShouldBeCalled) {
            verify(shardService).tryCreateShardAsync(targetShardHeight, currentBlockHeight);
        } else {
            verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
        }
    }

    /**
     * Height and target Frequency are supplied into unit test method
     * @return height + frequency value for test
     */
    static Stream<Arguments> supplyTestData() {
        return Stream.of(
            // arguments by order:
            // 1. lastShardHeight - simulate previously created shard (height)
            // 2. currentHeightBlockPushed - simulate block to be pushed at that height
            // 3. newShardHeight - we expect shard to be created at that height !!
            // 4. isShardingCalled - check if sharding was really executed
            // 5. isConfigJustUpdate - simulate HeightConfig is updated from previous to next height
            arguments(0, 0, 0, false, false),
            arguments(0, 1, 0, false, false),
            arguments(0, 206, 0, false, false), // sharding delay (6) = max rollback (3) + random shard delay (2) - 1
            arguments(0, 220, 0, false, false),
            arguments(0, 225, 0, false, false),
            arguments(0, 226, 220, true, false),
            arguments(0, 227, 220, true, false),
            arguments(0, 231, 220, true, false),
            arguments(220, 239, 0, false, false),
            arguments(220, 244, 0, false, false),
            arguments(220, 247, 240, true, false),
            arguments(0, 247, 220, true, false), // missed one of previous shards
            arguments(580, 606, 600, true, true), // config has switched recently
            arguments(600, 636, 0, false, false),
            arguments(600, 960, 0, false, false),
            arguments(600, 1506, 1500, true, true), // config has switched recently
            arguments(1000, 2508, 1500, true, true), // config has switched recently
            arguments(1500, 2003, 2000, false, true), // config has switched recently
            arguments(1500, 2008, 2000, true, true), // config has switched recently
            arguments(2000, 2238, 0, false, false),
            arguments(2000, 2999, 0, false, false),
            arguments(2000, 3006, 0, false, true), // config has switched recently
            arguments(2000, 3250, 0, false, true), // config has switched recently
            arguments(2000, 3250, 0, false, false), // simulated that config has NOT been switched recently
            arguments(3750, 4000, 4000, false, false),
            arguments(3750, 4006, 4000, true, false),
            arguments(4000, 4456, 0, false, false),
            arguments(4000, 5006, 0, false, true),
            arguments(4000, 5106, 5100, true, true),
            arguments(4000, 5137, 5100, true, false), // simulate that config has NOT been switched recently
            arguments(5900, 6006, 6000, true, true)
//            arguments(5900, 6006, 6000, true, false) // simulate that config has NOT been switched recently
        );
    }

}
