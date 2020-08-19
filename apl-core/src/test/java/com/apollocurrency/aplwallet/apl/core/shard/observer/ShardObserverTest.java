/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

//    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardService shardService;
//    @Mock
//    HeightConfig heightConfig;
//    @Mock
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class, withSettings().lenient());
    @Mock
    private Random random;
    BlockTestData td = new BlockTestData();
    private ShardObserver shardObserver;

    @BeforeEach
    void setUp() {
        chainsConfigLoader = new ChainsConfigLoader(CONFIG_NAME);
        loadedChains = chainsConfigLoader.load();
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");
        doReturn(3).when(propertiesHolder).MAX_ROLLBACK();
        doReturn(2).when(random).nextInt(any(Integer.class));
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, propertiesHolder);
        shardObserver = new ShardObserver(blockchainConfig, shardService, propertiesHolder, random);
    }

    private void prepare() {
//        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
//        assertNotNull(chain);
/*
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, propertiesHolder);
        shardObserver = new ShardObserver(blockchainConfig, shardService, propertiesHolder, random);
*/
    }

    @ParameterizedTest
    @MethodSource("supplyTestData")
    void testStartShardingByConfig(int latestShardHeight, int currentBlockHeight,
                                   int targetShardHeight,
                                   boolean isShouldBeCalled,
                                   boolean isSwitchedToNewConfigHeight) {
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        //
        if (isSwitchedToNewConfigHeight) {
            HeightConfig previousConfig = blockchainConfig.getConfigAtHeight(latestShardHeight);
            blockchainConfig.setCurrentConfig(previousConfig);
            HeightConfig newCurrentConfig = blockchainConfig.getConfigAtHeight(currentBlockHeight);
            blockchainConfig.setCurrentConfig(newCurrentConfig);
        }

        shardObserver.onBlockPushed(block);
//        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);
//        assertNull(c);

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
            // 2. currentHeightBlockPushed - simulate current block height
            // 3. newShardHeight - expected new shard height to be created
            // 4. isShardingCalled - check if sharding was really executed
            // 5. isConfigJustUpdate - simulate updating HeightConfig from previous to next height
            arguments(0, 0, 0, false, false),
            arguments(0, 1, 0, false, false),
            arguments(0, 206, 0, false, false),
            arguments(0, 220, 0, false, false),
            arguments(0, 225, 0, false, false),
            arguments(0, 226, 220, true, false),
            arguments(0, 227, 220, true, false),
            arguments(0, 231, 220, true, false),
            arguments(220, 239, 0, false, false),
            arguments(220, 244, 0, false, false),
            arguments(220, 247, 240, true, false),
            arguments(0, 247, 220, true, false), // missed one of previous shards
            arguments(580, 606, 600, true, true),
            arguments(600, 636, 0, false, false),
            arguments(600, 960, 0, false, false),
            arguments(600, 1506, 1500, true, true),
            arguments(1000, 2508, 1500, true, true),
            arguments(1500, 2003, 2000, false, true),
            arguments(1500, 2008, 2000, true, true)
        );
    }

/*
//    @Test
    void testDoNotShardWhenMinRollbackHeightIsNotMultipleOfShardingFrequency() {
        prepare();
//        doReturn(true).when(heightConfig).isShardingEnabled();
//        doReturn(NOT_MULTIPLE_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
//        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(DEFAULT_TRIM_HEIGHT);

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

//    @Test
    void testDoNotShardWhenLastTrimHeightIsZero() {
        prepare();
//        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(0);

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(0, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

//    @Test
    void testShardSuccessful() throws ExecutionException, InterruptedException {
        prepare();
//        doReturn(true).when(heightConfig).isShardingEnabled();
//        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
//        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(DEFAULT_TRIM_HEIGHT);
        CompletableFuture<MigrateState> completableFuture = mock(CompletableFuture.class);
        when(completableFuture.get()).thenReturn(MigrateState.COMPLETED);
        int height = DEFAULT_TRIM_HEIGHT+DEFAULT_SHARDING_FREQUENCY/3;
        Shard lastShard=new Shard();
        lastShard.setShardHeight(DEFAULT_TRIM_HEIGHT-DEFAULT_SHARDING_FREQUENCY);
//        when(shardService.getLastShard()).thenReturn(lastShard); // temp removed
        doReturn(completableFuture).when(shardService).tryCreateShardAsync(DEFAULT_TRIM_HEIGHT,height);

        CompletableFuture<MigrateState> state = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, height);

        assertNotNull(state);
        assertNotNull(state.get());
        verify(shardService, times(1)).tryCreateShardAsync(anyInt(), anyInt());
    }
*/

}
