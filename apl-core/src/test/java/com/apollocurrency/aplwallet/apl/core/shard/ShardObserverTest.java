/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShardObserverTest {
    public static final int DEFAULT_SHARDING_FREQUENCY = 5_000;
    public static final int NOT_MULTIPLE_SHARDING_FREQUENCY = 4_999;
    public static final int DEFAULT_MIN_ROLLBACK_HEIGHT = 100_000;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardMigrationExecutor shardMigrationExecutor;
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    DatabaseManager databaseManager;
    @Mock
    TransactionalDataSource transactionalDataSource;
    @Mock
    HeightConfig heightConfig;
    private ShardObserver shardObserver;

    @BeforeEach
    void setUp() {
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        shardObserver = new ShardObserver(blockchainProcessor, blockchainConfig, databaseManager, shardMigrationExecutor);
    }

    @Test
    void testSkipShardingWhenShardingIsDisabled() {
        doReturn(false).when(heightConfig).isShardingEnabled();

        boolean created = shardObserver.tryCreateShard();

        assertFalse(created);
        verify(blockchainProcessor, never()).getMinRollbackHeight();
    }

    @Test
    void testDoNotShardWhenMinRollbackHeightIsNotMultipleOfShardingFrequency() {
        doReturn(DEFAULT_MIN_ROLLBACK_HEIGHT).when(blockchainProcessor).getMinRollbackHeight();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(NOT_MULTIPLE_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        boolean created = shardObserver.tryCreateShard();

        assertFalse(created);
        verify(databaseManager, never()).getDataSource();
    }

    @Test
    void testDoNotShardWhenMinRollbackHeightIsZero() {
        doReturn(0).when(blockchainProcessor).getMinRollbackHeight();
        doReturn(true).when(heightConfig).isShardingEnabled();

        boolean created = shardObserver.tryCreateShard();

        assertFalse(created);
        verify(databaseManager, never()).getDataSource();
    }

    @Test
    void testShardSuccessful() {
        doReturn(transactionalDataSource).when(databaseManager).getDataSource();
        doReturn(DEFAULT_MIN_ROLLBACK_HEIGHT).when(blockchainProcessor).getMinRollbackHeight();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        boolean created = shardObserver.tryCreateShard();

        assertTrue(created);
        verify(transactionalDataSource, times(1)).begin();
        verify(transactionalDataSource, times(1)).commit();
    }

}
