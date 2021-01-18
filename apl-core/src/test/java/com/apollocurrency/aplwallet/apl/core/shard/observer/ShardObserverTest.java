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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ShardObserverTest {
    private static final String CONFIG_NAME = "another-list-for-shar-observer-config.json";
    private static final String CONFIG_NAME_TN3 = "tn3-shard-observer-config.json";
    private static final String CONFIG_NAME_MAIN = "mainnet-config.json";
    private static final String CONFIG_NAME_TN2 = "tn2-shard-observer-config.json";
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

    private void prepareAndInit(String configName, int maxRollback, int randomValue) {
        chainsConfigLoader = new ChainsConfigLoader(configName);
        loadedChains = chainsConfigLoader.load();
        chain = loadedChains.get(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"));
        assertNotNull(chain);
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.maxPrunableLifetime");
        doReturn(5000).when(propertiesHolder).getIntProperty("apl.minPrunableLifetime");
        doReturn(maxRollback).when(propertiesHolder).MAX_ROLLBACK();
        doReturn(randomValue).when(random).nextInt(any(Integer.class));
        assertNotNull(chain.getBlockchainProperties());
        blockchainConfig = new BlockchainConfig(chain, propertiesHolder);
        shardObserver = new ShardObserver(blockchainConfig, shardService, propertiesHolder, random);
        blockchainConfigUpdater = new BlockchainConfigUpdater(blockchainConfig, blockDao);
    }

    @ParameterizedTest
    @MethodSource("supplyTestData")
    void testStartShardingByConfig(int latestShardHeight, int currentBlockHeight,
                                   int targetShardHeight,
                                   boolean isShouldBeCalled) {
        // prepare components :
        // max rollback = 3
        // random shard delay = (2 + 1) = 3
        prepareAndInit(CONFIG_NAME, 3, 2);
        // prepare tes data
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastCompletedOrArchivedShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        blockchainConfigUpdater.onBlockPopped(block); // simulate setting 'current' and 'previous' config

        shardObserver.onBlockPushed(block);

        if (isShouldBeCalled) {
            verify(shardService, times(1)).tryCreateShardAsync(targetShardHeight, currentBlockHeight);
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
            arguments(0, 0, 0, false),
            arguments(0, 1, 0, false),
            arguments(0, 206, 0, false), // sharding delay (6) = max rollback (3) + random shard delay (2 + 1)
            arguments(0, 220, 0, false),
            arguments(0, 225, 0, false),
            arguments(0, 226, 220, true),
            arguments(0, 227, 220, false),
            arguments(220, 239, 0, false),
            arguments(220, 244, 0, false),
            arguments(220, 246, 240, true),
            arguments(0, 246, 240, true), // missed one of previous shard, (rollback 3 + divergence 3)
            arguments(580, 606, 600, true),
            arguments(600, 636, 0, false),
            arguments(600, 960, 0, false),
            arguments(600, 1506, 1500, true),
            arguments(1000, 2506, 2500, true),
            arguments(1500, 2003, 2000, false),
            arguments(1500, 2006, 2000, true),
            arguments(2000, 2238, 0, false),
            arguments(2000, 2999, 0, false),
            arguments(2000, 3006, 0, false),
            arguments(2000, 3250, 0, false),
            arguments(2000, 3250, 0, false),
            arguments(3750, 4000, 4000, false),
            arguments(3750, 4006, 4000, true),
            arguments(4000, 4456, 0, false),
            arguments(4000, 5006, 0, false),
            arguments(4000, 5106, 5100, true),
            arguments(4000, 5137, 5100, true),
            arguments(5900, 6006, 6000, true),
            arguments(5900, 6006, 6000, true),
            arguments(5800, 6006, 5900, true),
            arguments(5400, 6006, 5500, true),
            arguments(5900, 6006, 6000, true),
            arguments(5900, 6006, 6000, true),
            arguments(6000, 7005, 0, false),
            arguments(6000, 7005, 0, false),
            arguments(6000, 7006, 7000, true),
            arguments(6000, 7016, 7000, true),
            arguments(6000, 7916, 7000, true),
            arguments(7000, 8007, 8000, true)
        );
    }

    @ParameterizedTest
    @MethodSource("supplyTestDataTN3")
    void testStartShardingByTN3(int latestShardHeight,
                                int currentBlockHeight,
                                int targetShardHeight,
                                boolean isShouldBeCalled,
                                int randomMockValue) {
        // prepare components
        prepareAndInit(CONFIG_NAME_TN3, 2000, randomMockValue);
        // prepare tes data
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastCompletedOrArchivedShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        blockchainConfigUpdater.onBlockPopped(block); // simulate setting 'current' and 'previous' config

        shardObserver.onBlockPushed(block);

        if (isShouldBeCalled) {
            verify(shardService).tryCreateShardAsync(targetShardHeight, currentBlockHeight);
        } else {
            verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
        }
    }

    static Stream<Arguments> supplyTestDataTN3() {
        return Stream.of(
            // arguments by order:
            // 1. lastShardHeight - simulate previously created shard (height)
            // 2. currentHeightBlockPushed - simulate block to be pushed at that height
            // 3. newShardHeight - we expect shard to be created at that height !!
            // 4. isShardingCalled - check if sharding was really executed
            // 5. mocked random Value - simulate random divergence
            arguments(0, 100, 0, false, 143),
            arguments(0, 4144, 2000, true, 143),
            arguments(6000, 10144, 8000, true, 143),
            arguments(6000, 10144, 8000, true, 143),
            arguments(8000, 12273, 10000, true, 272),
            arguments(8000, 12274, 10000, false, 272),
            arguments(10000, 22144, 20000, true, 143),
            arguments(20000, 32144, 30000, true, 143),
            arguments(30000, 42144, 40000, true, 143)
        );
    }

    @ParameterizedTest
    @MethodSource("supplyTestDataMainNet")
    void testStartShardingByMainNet(int latestShardHeight, int currentBlockHeight,
                                int targetShardHeight,
                                boolean isShouldBeCalled,
                                int randomMockValue) {
        // prepare components
        prepareAndInit(CONFIG_NAME_MAIN, 21000, randomMockValue);
        // prepare tes data
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastCompletedOrArchivedShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        blockchainConfigUpdater.onBlockPopped(block); // simulate setting 'current' and 'previous' config

        shardObserver.onBlockPushed(block);

        if (isShouldBeCalled) {
            verify(shardService).tryCreateShardAsync(targetShardHeight, currentBlockHeight);
        } else {
            verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
        }
    }

    static Stream<Arguments> supplyTestDataMainNet() {
        return Stream.of(
            // arguments by order:
            // 1. lastShardHeight - simulate previously created shard (height)
            // 2. currentHeightBlockPushed - simulate block to be pushed at that height
            // 3. newShardHeight - we expect shard to be created at that height !!
            // 4. isShardingCalled - check if sharding was really executed
            // 5. mocked random Value - simulate random divergence
            arguments(0, 2247000, 0, false, 323),
            arguments(0, 2247001, 0, false, 323),
            arguments(0, 2271324, 2250000, true, 323),
            arguments(0, 2271217, 2250000, true, 216),
            arguments(0, 2271060, 2250000, true, 59)
        );
    }

    @ParameterizedTest
    @MethodSource("supplyTestDataTN2")
    void testStartShardingByTN2(int latestShardHeight,
                                int currentBlockHeight,
                                int targetShardHeight,
                                boolean isShouldBeCalled,
                                int randomMockValue) {
        // prepare components
        prepareAndInit(CONFIG_NAME_TN2, 2000, randomMockValue);
        // prepare tes data
        Shard shard = mock(Shard.class);
        doReturn(latestShardHeight).when(shard).getShardHeight();
        doReturn(shard).when(shardService).getLastCompletedOrArchivedShard();
        Block block = td.BLOCK_1;
        block.setHeight(currentBlockHeight);
        blockchainConfigUpdater.onBlockPopped(block); // simulate setting 'current' and 'previous' config

        shardObserver.onBlockPushed(block);

        if (isShouldBeCalled) {
            verify(shardService).tryCreateShardAsync(targetShardHeight, currentBlockHeight);
        } else {
            verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
        }
    }

    static Stream<Arguments> supplyTestDataTN2() {
        return Stream.of(
            // arguments by order:
            // 1. lastShardHeight - simulate previously created shard (height)
            // 2. currentHeightBlockPushed - simulate block to be pushed at that height
            // 3. newShardHeight - we expect shard to be created at that height !!
            // 4. isShardingCalled - check if sharding was really executed
            // 5. mocked random Value - simulate random divergence
            arguments(200000, 204076, 202000, false, 78),
            arguments(200000, 204078, 202000, false, 78),
            arguments(200000, 204079, 202000, true, 78), // only one exact match for division on frequency will trigger
            arguments(204000, 208079, 206000, true, 78),
            arguments(200000, 204100, 202000, false, 78)
        );
    }


}
