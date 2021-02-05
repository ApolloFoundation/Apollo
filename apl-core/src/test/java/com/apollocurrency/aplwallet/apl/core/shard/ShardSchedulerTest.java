/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Slf4j
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ShardSchedulerTest {

    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardService shardService;
    @Mock
    private Random random;
    @Mock
    Event trimEvent;
    @Mock
    TimeService timeService;

    private ShardingScheduler shardScheduler;
    @AfterEach
    void tearDown() {
        shardScheduler.shutdown();
    }

    @Test
    void testInit_shardingDisabled() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, true, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);

        shardScheduler.init(2000, 0);

        verifyNoInteractions(shardService,  timeService, blockchainConfig);
        assertTrue(shardScheduler.scheduledShardings().isEmpty(), "Expected no scheduled shards when sharding is disabled");
    }



    @Test
    void testInit_lastShardingFailed() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(new Shard(1L, new byte[32], ShardState.IN_PROGRESS, 2000, new byte[0], new long[3], new int[3], new int[3], new byte[32])).when(shardService).getLastShard();
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        shardScheduler.init(2000, 0);

        verifyNoInteractions(timeService, blockchainConfig);
        verify(trimEvent, times(1)).fire(new TrimConfig(true, false));

        assertTrue(shardScheduler.scheduledShardings().isEmpty(), "Expected no scheduled shards when last shard was failed");
    }

    @Test
    void testInit_scheduleAllNotFinishedShards() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        List<HeightConfig> heightConfigs = List.of(mockHeightConfig(0, true, 10),
            mockHeightConfig(10, true, 5),
            mockHeightConfig(11, false, 11),
            mockHeightConfig(500, true, 1000));
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(0, 2000);
        doAnswer(new Answer() {
            int i;

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                i++;
                if (i == 1) {
                    return 20;
                } else {
                    return 19; // do not allow to start at least one shard
                }

            }
        }).when(timeService).systemTimeMillis();

        shardScheduler.init(2000, 0);

        verifyNoInteractions(timeService, blockchainConfig);
        verify(trimEvent, times(1)).fire(new TrimConfig(true, false));

        assertTrue(shardScheduler.scheduledShardings().isEmpty(), "Expected no scheduled shards when last shard was failed");
    }

    private HeightConfig mockHeightConfig(int height, boolean shardingEnabled, int shardingFrequency) {
        HeightConfig config = mock(HeightConfig.class);
        doReturn(height).when(config).getHeight();
        doReturn(shardingEnabled).when(config).isShardingEnabled();
        doReturn(shardingFrequency).when(config).getShardingFrequency();
        return config;
    }


    void fireBlockPushed(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        shardScheduler.onBlockPushed(block);
    }
}
