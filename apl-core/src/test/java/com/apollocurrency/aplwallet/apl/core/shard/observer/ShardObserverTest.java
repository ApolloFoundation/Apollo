/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
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
    @Mock
    PropertiesHolder propertiesHolder;
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
    void testSkipShardingWhenShardingIsDisabled(int latestShardHeight, int currentBlockHeight,
                                                /*int shardFrequency, */int targetShardHeight,
                                                boolean isShouldBeCalled) {
//        prepare();
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastShard();
//        doReturn(false).when(heightConfig).isShardingEnabled();
//        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(DEFAULT_TRIM_HEIGHT);
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);

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
            // lastShardHeight, currentHeightBlockPushed, newShardHeight, isCalled
            arguments(0, 0, 0, false),
            arguments(0, 1, 0, false)
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
