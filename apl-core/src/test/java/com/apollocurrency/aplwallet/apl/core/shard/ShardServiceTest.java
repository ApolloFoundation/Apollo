/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
@ExtendWith(MockitoExtension.class)
public class ShardServiceTest {
    public static final int DEFAULT_TRIM_HEIGHT = 100_000;

    ShardService shardService;
    @Mock ShardDao shardDao;
    @Mock BlockchainProcessor blockchainProcessor;
    @Mock Blockchain blockchain;
    @Mock DirProvider dirProvider;
    @Mock Zip zip;
    @Mock DatabaseManager databaseManager;
    @Mock BlockchainConfig blockchainConfig;
    @Mock ShardRecoveryDao shardRecoveryDao;
    @Mock ShardMigrationExecutor shardMigrationExecutor;
    @Mock AplAppStatus aplAppStatus;
    @Mock PropertiesHolder propertiesHolder;
    @Mock Event<Boolean> trimEvent;
    @Mock GlobalSync globalSync;

    @BeforeEach
    void setUp() {
        shardService = new ShardService(shardDao, blockchainProcessor, blockchain, dirProvider, zip, databaseManager, blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync);
    }

    @Test
    void testShardWhenShardExecutorThrowAnyException() throws ExecutionException, InterruptedException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doThrow(new RuntimeException()).when(shardMigrationExecutor).executeAllOperations();
        CompletableFuture<MigrateState> c = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertEquals(MigrateState.FAILED, c.get());
        verify(shardMigrationExecutor, times(1)).executeAllOperations();
        verify(trimEvent, times(1)).fire(true);
        verify(trimEvent, times(1)).fire(false);
    }

    @Test
    void testSkipShardingDuringBlockchainScan() {

        doReturn(true).when(blockchainProcessor).isScanning();

        CompletableFuture<MigrateState> c = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);

        verify(shardMigrationExecutor, never()).executeAllOperations();
        verify(trimEvent, never()).fire(true);
        verify(trimEvent, never()).fire(false);
    }

    @Test
    void testSkipSharding() throws InterruptedException, ExecutionException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        AtomicBoolean shutdown = new AtomicBoolean(false);
        doAnswer((in) -> {
            while (!shutdown.get()) {
                Thread.sleep(1L);
            }
            return MigrateState.COMPLETED;
        }).when(shardMigrationExecutor).executeAllOperations();

        CompletableFuture<MigrateState> shardFuture1 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNotNull(shardFuture1);

        CompletableFuture<MigrateState> shardFuture2 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT + 1, Integer.MAX_VALUE);

        assertNull(shardFuture2);
        // assertFalse(shardFuture2.isDone());
        shutdown.set(true);

        MigrateState result = shardFuture1.get();
        assertEquals(MigrateState.COMPLETED, result);

        verify(shardMigrationExecutor, times(1)).executeAllOperations();
    }

    @Test
    void testSkipShardingWhenLastShardHaveSameHeight() throws InterruptedException, ExecutionException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});

        CompletableFuture<MigrateState> shardFuture1 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNotNull(shardFuture1);

        shardFuture1.get();

        doReturn(new Shard(100, DEFAULT_TRIM_HEIGHT)).when(shardDao).getLastShard();

        CompletableFuture<MigrateState> shardFuture2 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);
        CompletableFuture<MigrateState> shardFuture3 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(shardFuture2);
        assertNull(shardFuture3);

        verify(shardMigrationExecutor, times(1)).executeAllOperations();
    }

}
