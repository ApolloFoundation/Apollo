/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.env.config.ShardingSettings;
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

import static com.apollocurrency.aplwallet.apl.core.shard.observer.ShardObserverTest.DEFAULT_TRIM_HEIGHT;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.Optional;

@EnableWeld
public class ShardObserverIntegrationTest {
    static final int DEFAULT_SHARDING_FREQUENCY = 100;
    private final ShardService shardService = mock(ShardService.class);
    private final BlockDao blockDao = mock(BlockDao.class);
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    BlockchainConfigUpdater blockchainConfigUpdater = mock(BlockchainConfigUpdater.class);

    Optional<ShardingSettings> heightConfig;
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class)
        .addBeans(MockBean.of(shardService, ShardService.class))
        .addBeans(MockBean.of(blockDao, BlockDao.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchainConfigUpdater, BlockchainConfigUpdater.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .build();
    @Inject
    Event<TrimData> trimEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByAsyncEvent() {
        heightConfig = Optional.of(new ShardingSettings(false, DEFAULT_SHARDING_FREQUENCY));

        Mockito.doReturn(heightConfig).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(DEFAULT_SHARDING_FREQUENCY);
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
        }).fireAsync(new TrimData(DEFAULT_SHARDING_FREQUENCY, DEFAULT_SHARDING_FREQUENCY + 100, 0));
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        Mockito.verify(blockchainConfigUpdater, times(1)).getShardingSettingsByTrimHeight(DEFAULT_SHARDING_FREQUENCY);
    }

    @Test
    void testDoShardBySyncEvent() {
        heightConfig = Optional.of(new ShardingSettings(true, DEFAULT_SHARDING_FREQUENCY));
        Mockito.doReturn(heightConfig).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(DEFAULT_SHARDING_FREQUENCY);
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        //Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
        }).fire(new TrimData(100, 100, 0));

        Mockito.verify(blockchainConfigUpdater, times(1)).getShardingSettingsByTrimHeight(DEFAULT_SHARDING_FREQUENCY);
    }

}
