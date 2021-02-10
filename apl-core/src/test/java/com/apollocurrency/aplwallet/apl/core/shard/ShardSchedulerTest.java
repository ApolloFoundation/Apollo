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
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
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
        assertFalse(shardScheduler.createShards());
    }

    @Test
    void testInit_scheduleAllNotFinishedShards() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        List<HeightConfig> heightConfigs = List.of(
            mockHeightConfig(0, true, 10),
            mockHeightConfig(10, true, 5),
            mockHeightConfig(11, false, 11),
            mockHeightConfig(500, true, 1000));
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(0, 1000);
        doAnswer(new Answer() {
            long i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return ++i > 2 ? 19L : 20L;
            }
        }).when(timeService).systemTimeMillis();

        shardScheduler.init(2000, 0);

        verify(trimEvent, times(2)).fire(new TrimConfig(false, false));

        List<ShardingScheduler.ShardScheduledRecord> scheduledRecords = List.of(
            new ShardingScheduler.ShardScheduledRecord(0, 10, 2000, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 1000, 2000, 20));
        assertEquals(shardScheduler.scheduledShardings(), scheduledRecords);
        assertTrue(shardScheduler.createShards());
    }


    @Test
    void testInitFailed_finishAllNotFinishedShards_trimHeightGreaterThanShardHeight() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        List<HeightConfig> heightConfigs = List.of(
            mockHeightConfig(0, true, 10));
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(0, 1000);

        shardScheduler.init(2000, 1000);

        verify(trimEvent, times(1)).fire(new TrimConfig(true, false));
        assertEquals(shardScheduler.scheduledShardings(), List.of());
        assertFalse(shardScheduler.createShards());
    }

    @Test
    void testInit_finishAllNotFinishedShards_afterLastSuccessfulShard() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 720);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(new Shard(1L, new byte[32], ShardState.FULL, 50, new byte[0], new long[3], new int[3], new int[3], new byte[32])).when(shardService).getLastShard();
        List<HeightConfig> heightConfigs = List.of(
            mockHeightConfig(49, true, 50), // done
            mockHeightConfig(50, true, 51), // new shard at 51 height
            mockHeightConfig(51, true, 52),  // shard at 52
            mockHeightConfig(52, true, 5),  // shards at 55, 60
            mockHeightConfig(64, true, 10), // shards at 70, 80
            mockHeightConfig(81, false, 2), // no shards
            mockHeightConfig(99, true, 100) // shard at 100
            );
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(50, 100);
        doAnswer(new Answer() {
            long i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return ++i > 7 ? 19L : 20L;
            }
        }).when(timeService).systemTimeMillis();

        shardScheduler.init(820, 51);

        verify(trimEvent, times(7)).fire(new TrimConfig(false, false)); // 6 shards

        List<ShardingScheduler.ShardScheduledRecord> scheduledRecords = List.of(
            new ShardingScheduler.ShardScheduledRecord(0, 51, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 52, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 55, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 60, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 70, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 80, 820, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 100, 820, 20)
            );
        assertEquals(shardScheduler.scheduledShardings(), scheduledRecords);
        assertTrue(shardScheduler.createShards());
    }

    @Test
    void testInit_noHeightConfigs() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 800);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        List<HeightConfig> heightConfigs = List.of();
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(0, 1200);

        assertThrows(IllegalStateException.class, () -> shardScheduler.init(2000, 0));
    }



    @Test
    void testInit_trySharding() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 1000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(new Shard(1L, new byte[32], ShardState.FULL, 100, new byte[0], new long[3], new int[3], new int[3], new byte[32])).when(shardService).getLastShard();
        List<HeightConfig> heightConfigs = List.of(
            mockHeightConfig(50, true, 50),
            mockHeightConfig(200, false, 100)
        );
        doReturn(heightConfigs).when(blockchainConfig).getAllActiveConfigsBetweenHeights(100, 1000);
        doAnswer(new Answer() {
            long i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return ++i > 2 ? 19L : 20L;
            }
        }).when(timeService).systemTimeMillis();

        shardScheduler.init(2000, 75);

        verify(trimEvent, times(2)).fire(new TrimConfig(false, false)); // 6 shards

        List<ShardingScheduler.ShardScheduledRecord> scheduledRecords = List.of(
            new ShardingScheduler.ShardScheduledRecord(0, 150, 2000, 20),
            new ShardingScheduler.ShardScheduledRecord(0, 200, 2000, 20)
        );
        assertEquals(shardScheduler.scheduledShardings(), scheduledRecords);
        assertTrue(shardScheduler.createShards());

        // prepare for triggering trySharding
        CompletableFuture shardingProcess = mock(CompletableFuture.class);
        doReturn(MigrateState.COMPLETED).when(shardingProcess).join();
        doReturn(shardingProcess).when(shardService).tryCreateShardAsync(150, 2000);
        doReturn(shardingProcess).when(shardService).tryCreateShardAsync(200, 2000);
        doAnswer(new Answer() {
            int i;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return ++i == 1 ? new Shard(2L, new byte[32], ShardState.FULL, 150, new byte[0], new long[3], new int[3], new int[3], new byte[32]) :
                    new Shard(3L, new byte[32], ShardState.FULL, 200, new byte[0], new long[3], new int[3], new int[3], new byte[32]);
            }
        }).when(shardService).getLastShard();
        // Start sharding, scheduled time has come
        doReturn(20L).when(timeService).systemTimeMillis();

        while (!shardScheduler.scheduledShardings().isEmpty()) {
            ThreadUtils.sleep(50);
        }
        verify(trimEvent, times(1)).fire(new TrimConfig(true, false));
    }

    @Test
    void testOnBlockPushed_trySharding() {
        ShardSchedulingConfig schedulingConfig = new ShardSchedulingConfig(10, 20, false, 2000);
        shardScheduler = new ShardingScheduler(trimEvent, shardService, blockchainConfig, schedulingConfig, timeService);
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(new Shard(1L, new byte[32], ShardState.FULL, 100, new byte[0], new long[3], new int[3], new int[3], new byte[32])).when(shardService).getLastShard();
        doReturn(mockHeightConfig(500, true, 1000)).when(blockchainConfig).getConfigAtHeight(2999);
        shardScheduler.setRandom(random);
        doReturn(30L).when(timeService).systemTimeMillis();
        doReturn(-10).when(random).nextInt(11); // cheat on delay (make it zero for a better test speed)

        fireBlockPushed(5000);

        verify(trimEvent, times(1)).fire(new TrimConfig(false, false));
        assertEquals(shardScheduler.scheduledShardings(), List.of(new ShardingScheduler.ShardScheduledRecord(0, 3000, 5000, 30)));

        CompletableFuture shardingProcess = mock(CompletableFuture.class);
        doReturn(MigrateState.COMPLETED).when(shardingProcess).join();
        doReturn(shardingProcess).when(shardService).tryCreateShardAsync(3000, 5000);
        doReturn(new Shard(2L, new byte[32], ShardState.FULL, 3000, new byte[0], new long[3], new int[3], new int[3], new byte[32])).when(shardService).getLastShard();

        shardScheduler.scheduleBackgroundShardingTask(0);

        // wait finishing of sharding
        while (!shardScheduler.scheduledShardings().isEmpty()) {
            ThreadUtils.sleep(50);
        }
        verify(trimEvent, times(1)).fire(new TrimConfig(true, false));
    }



    private HeightConfig mockHeightConfig(int height, boolean shardingEnabled, int shardingFrequency) {
        HeightConfig config = mock(HeightConfig.class);
        lenient().doReturn(height).when(config).getHeight();
        doReturn(shardingEnabled).when(config).isShardingEnabled();
        lenient().doReturn(shardingFrequency).when(config).getShardingFrequency();
        return config;
    }


    void fireBlockPushed(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        shardScheduler.onBlockPushed(block);
    }
}
