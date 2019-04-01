/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletionStage;
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

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class, ShardDao.class, ShardRecoveryDao.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(shardMigrationExecutor, ShardMigrationExecutor.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class))
            .addBeans(MockBean.of(recoveryDao, ShardRecoveryDao.class))
            .addBeans(MockBean.of(shardDao, ShardDao.class))
            .build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByEvent() {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(mock(Block.class));

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
