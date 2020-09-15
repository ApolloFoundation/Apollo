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
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@EnableWeld
public class ShardObserverIntegrationTest {
    static final int DEFAULT_SHARDING_FREQUENCY = 100;
    private final ShardService shardService = mock(ShardService.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    HeightConfig heightConfig;
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    Random random = mock(Random.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class)
        .addBeans(MockBean.of(shardService, ShardService.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(random, Random.class))
        .build();
    @Inject
    Event<TrimData> trimEvent;

    @Test
    void testDoShardByAsyncEvent() {
        heightConfig = mock(HeightConfig.class);
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(DEFAULT_SHARDING_FREQUENCY);
        CompletableFuture c = mock(CompletableFuture.class);
        doReturn(c).when(shardService).tryCreateShardAsync(DEFAULT_SHARDING_FREQUENCY, Integer.MAX_VALUE);
        doReturn(2).when(random).nextInt(any(Integer.class)); // random shard delay = 2

        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
        }).fireAsync(new TrimData(100, Integer.MAX_VALUE, 0));
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        verify(heightConfig, never()).isShardingEnabled();
        verify(blockchainConfig, never()).getConfigAtHeight(DEFAULT_SHARDING_FREQUENCY);
        verify(shardService, never()).tryCreateShardAsync(DEFAULT_SHARDING_FREQUENCY, Integer.MAX_VALUE);
    }

    @Test
    void testDoShardBySyncEvent() {
        heightConfig = mock(HeightConfig.class);
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        doReturn(heightConfig).when(blockchainConfig).getConfigAtHeight(DEFAULT_SHARDING_FREQUENCY);
        doReturn(2).when(random).nextInt(any(Integer.class)); // random shard delay = 2

        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
        }).fire(new TrimData(100, 100, 0));

        verify(heightConfig, never()).isShardingEnabled();
        verify(blockchainConfig, never()).getConfigAtHeight(DEFAULT_SHARDING_FREQUENCY);
    }

}
