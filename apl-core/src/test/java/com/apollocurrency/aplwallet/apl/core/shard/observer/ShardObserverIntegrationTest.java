/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@EnableWeld
public class ShardObserverIntegrationTest {
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    HeightConfig heightConfig = mock(HeightConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    static final int DEFAULT_SHARDING_FREQUENCY = 100;

    private final ShardService shardService = mock(ShardService.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class)
            .addBeans(MockBean.of(shardService, ShardService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .build();
    @Inject
    Event<TrimData> trimEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByAsyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {}).fireAsync(new TrimData(100, 100, 0));
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {           
        }
        Mockito.verify(heightConfig, times(1)).isShardingEnabled();
    }

    @Test
    void testDoShardBySyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Mockito.doReturn(true).when(blockchainConfig).isEnablePruning();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
        }).fire(new TrimData(100, 100, 0));

        Mockito.verify(heightConfig, times(1)).isShardingEnabled();
    }

}
