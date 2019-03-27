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
    DatabaseManager databaseManager = mock(DatabaseManager.class);
    HeightConfig heightConfig = mock(HeightConfig.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(ShardObserver.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(shardMigrationExecutor, ShardMigrationExecutor.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class))
            .addBeans(MockBean.of(databaseManager, DatabaseManager.class))
            .build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    ShardObserver shardObserver;

    @Test
    void testDoShardByEvent() throws InterruptedException {
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        CompletionStage<Block> blockCompletionStage = blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fireAsync(mock(Block.class));
        blockCompletionStage.toCompletableFuture().join();

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
