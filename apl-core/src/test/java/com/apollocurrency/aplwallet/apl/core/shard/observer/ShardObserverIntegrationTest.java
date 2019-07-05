/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

@EnableWeld
public class ShardObserverIntegrationTest {
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    HeightConfig heightConfig = mock(HeightConfig.class);

    private final ShardService shardService = mock(ShardService.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class)
            .addBeans(MockBean.of(shardService, ShardService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .build();
    @Inject
    Event<TrimData> trimEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByAsyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        trimEvent.select(new AnnotationLiteral<Async>() {}).fire(new TrimData(100, 100));

        Mockito.verify(heightConfig, times(1)).isShardingEnabled();
    }

    @Test
    void testDoShardBySyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        trimEvent.select(new AnnotationLiteral<Sync>() {}).fire(new TrimData(100, 100));

        Mockito.verify(heightConfig, times(1)).isShardingEnabled();
    }

    private AnnotationLiteral literal(BlockEventType blockEvent) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEvent;
            }
        };
    }
}
