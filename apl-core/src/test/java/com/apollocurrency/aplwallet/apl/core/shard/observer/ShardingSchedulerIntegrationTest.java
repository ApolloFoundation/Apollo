/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.app.observer.TrimObserver;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.shard.ShardSchedulingConfig;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardingScheduler;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@EnableWeld
public class ShardingSchedulerIntegrationTest {
     ShardService shardService = mock(ShardService.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    TimeService timeService = mock(TimeService.class);
    Event trimEvent = mock(Event.class);
    TrimService trimService = mock(TrimService.class);
    static TrimConfig trimConfig = mockTrimConfig();
    Random random = mock(Random.class);
    Blockchain blockchain = mock(Blockchain.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardingScheduler.class, TrimObserver.class)
        .addBeans(MockBean.of(shardService, ShardService.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(new ShardSchedulingConfig(-1, 50, false, 1000), ShardSchedulingConfig.class))
        .addBeans(MockBean.of(trimService, TrimService.class))
        .addBeans(MockBean.of(trimConfig, TrimConfig.class))
        .addBeans(MockBean.of(random, Random.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .build();

    @Inject
    Event<Block> blockEvent;
    @Inject
    ShardingScheduler scheduler;
    @Inject
    TrimObserver trimObserver;

    static AtomicLong trimObserverTiming = new AtomicLong();


    @Test
    void testDoShardByAsyncEvent() {
        AtomicLong shardSchedulerTiming = new AtomicLong();
        doAnswer(invocationOnMock -> {
            shardSchedulerTiming.set(System.currentTimeMillis());
            ThreadUtils.sleep(10);
            return trimEvent;
        }).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        HeightConfig shardingConfig = mockHeightConfig(2500, true, 1000);
        doReturn(shardingConfig).when(blockchainConfig).getConfigAtHeight(8999);

        fireBlockPushed(10000);

        List<ShardingScheduler.ShardScheduledRecord> shardScheduledRecords = scheduler.scheduledShardings();
        assertEquals(List.of(new ShardingScheduler.ShardScheduledRecord(0, 9000, 10000, 0)), shardScheduledRecords);

        assertTrue(trimObserverTiming.get() - shardSchedulerTiming.get() >= 20, "Incorrect sequence of observers, shardSchedulerTime - " + shardSchedulerTiming.get() + ", trimObserverTime - " + trimObserverTiming.get());
        List<Integer> trimHeights = trimObserver.getTrimQueue();
        assertEquals(List.of(10000), trimHeights);
        assertFalse(trimObserver.trimEnabled());
    }

    private void fireBlockPushed(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fire(block);
    }

    private HeightConfig mockHeightConfig(int height, boolean shardingEnabled, int shardingFrequency) {
        HeightConfig config = mock(HeightConfig.class);
        lenient().doReturn(height).when(config).getHeight();
        doReturn(shardingEnabled).when(config).isShardingEnabled();
        lenient().doReturn(shardingFrequency).when(config).getShardingFrequency();
        return config;
    }

    private AnnotationLiteral<BlockEvent> literal(BlockEventType blockEventType) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEventType;
            }
        };
    }

    static TrimConfig mockTrimConfig() {
        TrimConfig mock = mock(TrimConfig.class);
        doAnswer(invocationOnMock -> {
            ThreadUtils.sleep(10);
            trimObserverTiming.set(System.currentTimeMillis());
            return 1000;
        }).when(mock).getTrimFrequency();
        return mock;
    }
}
