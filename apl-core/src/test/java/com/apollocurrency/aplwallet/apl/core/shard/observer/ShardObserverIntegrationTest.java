/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

@EnableWeld
public class ShardObserverIntegrationTest {
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    ShardMigrationExecutor shardMigrationExecutor = mock(ShardMigrationExecutor.class);
    BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);
    ShardRecoveryDao recoveryDao = mock(ShardRecoveryDao.class);
    HeightConfig heightConfig = mock(HeightConfig.class);
    ShardDao shardDao = mock(ShardDao.class);
    PropertiesHolder holder = new PropertiesHolder();
    {
        Properties properties = new Properties();
        properties.put("apl.trimDerivedTables", "true");
        properties.put("apl.noshardcreate", "false");
        holder.init(properties);
    }

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class, ShardDao.class, ShardRecoveryDao.class, PropertyProducer.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(shardMigrationExecutor, ShardMigrationExecutor.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class))
            .addBeans(MockBean.of(recoveryDao, ShardRecoveryDao.class))
            .addBeans(MockBean.of(shardDao, ShardDao.class))
            .addBeans(MockBean.of(holder, PropertiesHolder.class))
            .build();
    @Inject
    Event<TrimData> trimEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByAsyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
        trimEvent.select(new AnnotationLiteral<Async>() {}).fire(new TrimData(100, 100, 0));

        Mockito.verify(heightConfig, times(1)).isShardingEnabled();
    }

    @Test
    void testDoShardBySyncEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
        trimEvent.select(new AnnotationLiteral<Async>() {}).fire(new TrimData(100, 100, 0));

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
