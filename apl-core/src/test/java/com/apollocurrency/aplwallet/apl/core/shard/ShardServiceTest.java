/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
public class ShardServiceTest {
    public static final int DEFAULT_TRIM_HEIGHT = 100_000;
    @RegisterExtension
    TemporaryFolderExtension folder = new TemporaryFolderExtension();

    ShardService shardService;
    @Mock
    ShardDao shardDao;
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    Blockchain blockchain;
    @Mock
    DirProvider dirProvider;
    @Mock
    Zip zip;
    @Mock
    DatabaseManager databaseManager;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardRecoveryDao shardRecoveryDao;
    @Mock
    ShardMigrationExecutor shardMigrationExecutor;
    @Mock
    AplAppStatus aplAppStatus;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    Event<TrimConfig> trimEvent;
    @Mock
    Event<DbHotSwapConfig> dbEvent;
    @Mock
    GlobalSync globalSync;
    @Mock
    TrimService trimService;

    @BeforeEach
    void setUp() {
        shardService = new ShardService(shardDao, blockchainProcessor, blockchain, dirProvider, zip, databaseManager, blockchainConfig, shardRecoveryDao, shardMigrationExecutor, aplAppStatus, propertiesHolder, trimEvent, globalSync, trimService, dbEvent);
    }

    @Test
    void testShardWhenShardExecutorThrowAnyException() throws ExecutionException, InterruptedException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        doThrow(new RuntimeException()).when(shardMigrationExecutor).executeAllOperations();
        CompletableFuture<MigrateState> c = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertEquals(MigrateState.FAILED, c.get());
        verify(shardMigrationExecutor).executeAllOperations();
        verify(trimEvent, after(250).times(2)).fire((any(TrimConfig.class))); //wait 250 ms to make sure, that next completable future task was performed as well as sharding task
    }

    @Test
    void testSkipShardingDuringBlockchainScan() {

        doReturn(true).when(blockchainProcessor).isScanning();

        CompletableFuture<MigrateState> c = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);

        verify(shardMigrationExecutor, never()).executeAllOperations();
        verify(trimEvent, never()).fire(new TrimConfig(true, false));
        verify(trimEvent, never()).fire(new TrimConfig(false, false));
    }


    @Test
    void testSkipSharding() throws InterruptedException, ExecutionException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
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
    void testShardingWhenNoShardCreateSet() throws ExecutionException, InterruptedException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);

        CompletableFuture<MigrateState> c = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertEquals(MigrateState.FAILED, c.get());

        verifyZeroInteractions(shardMigrationExecutor);
        verify(trimEvent, after(250).times(2)).fire(any(TrimConfig.class)); //wait 250 ms to make sure, that next completable future task was performed as well as sharding task
    }

    @Test
    void testSkipShardingWhenLastShardHaveSameHeight() {
        doReturn(new Shard(100, DEFAULT_TRIM_HEIGHT)).when(shardDao).getLastShard();

        CompletableFuture<MigrateState> shardFuture2 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);
        CompletableFuture<MigrateState> shardFuture3 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(shardFuture2);
        assertNull(shardFuture3);

        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testCreateShardWhenLastShardHeightLessThanCurrentHeight() throws ExecutionException, InterruptedException {
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        doReturn(new Shard(100, DEFAULT_TRIM_HEIGHT)).when(shardDao).getLastShard();
        CompletableFuture<MigrateState> shardFuture1 = shardService.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT + 1, Integer.MAX_VALUE);
        assertNotNull(shardFuture1);
        assertNull(shardFuture1.get());
        verify(shardMigrationExecutor).executeAllOperations();
    }

    private void mockBackupExists() throws IOException {
        Chain chain = mock(Chain.class);
        UUID chainId = UUID.randomUUID();
        doReturn(chainId).when(chain).getChainId();
        doReturn(folder.getRoot().toPath()).when(dirProvider).getDbDir();
        doReturn(chain).when(blockchainConfig).getChain();
        folder.newFile("BACKUP-BEFORE-apl-blockchain-shard-1-chain-" + chainId + ".zip");
    }

    @Test
    void testSkipResetToShardWhenShardingProcessWasStartedWithoutCompleteableFuture() throws IOException {
        mockBackupExists();
        shardService.setSharding(true);

        boolean reset = shardService.reset(1L);

        assertFalse(reset);
        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testSkipResetWhenShardBackupNotExists() throws IOException {
        Chain chain = mock(Chain.class);
        doReturn(UUID.randomUUID()).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(folder.newFolder().toPath()).when(dirProvider).getDbDir();

        boolean reset = shardService.reset(1);

        assertFalse(reset);
        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testReset() throws IOException {
        mockBackupExists();
        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        boolean reset = shardService.reset(1);

        assertTrue(reset);
        verifySuccessfulReset(firedEvent);
    }

    @Test
    void testResetWithCancellingShardingProcess() throws IOException, InterruptedException {
        mockBackupExists();
        doReturn(trimEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();
        AtomicBoolean shardingStarted = new AtomicBoolean(false);
        doAnswer((d) -> {
            shardingStarted.set(true);
            while (true) {
                Thread.sleep(10);
            }
        }).when(shardMigrationExecutor).executeAllOperations();
        CompletableFuture<MigrateState> shardProcess = shardService.tryCreateShardAsync(4000, 6000);
        assertNotNull(shardProcess);

        while (!shardingStarted.get()) {
            Thread.sleep(10);
        }
        boolean reset = shardService.reset(1L);
        assertTrue(reset);
        assertTrue(shardProcess.isDone());
        assertFalse(shardService.isSharding());

    }

    @Test
    void testResetWaitingTrim() throws IOException, ExecutionException, InterruptedException {
        mockBackupExists();
        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();
        Event firedEvent = mock(Event.class);
        doReturn(firedEvent).when(trimEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {
        });
        doReturn(true).when(trimService).isTrimming();
        CompletableFuture<Void> trimFuture = CompletableFuture.supplyAsync(() -> {
            ThreadUtils.sleep(250);
            doReturn(false).when(trimService).isTrimming();
            return null;
        });
        boolean reset = shardService.reset(1);
        trimFuture.get();

        assertTrue(reset);
        verifySuccessfulReset(firedEvent);
    }

    private void verifySuccessfulReset(Event<TrimConfig> event) {
        verify(databaseManager).getDataSource();
        verify(globalSync).writeLock();
        verify(globalSync).writeUnlock();
        verify(dbEvent).fire(new DbHotSwapConfig(1));
        verify(event).fire(new TrimConfig(false, true));
        verify(event).fire(new TrimConfig(true, false));
        verify(blockchainProcessor).suspendBlockchainDownloading();
        verify(blockchainProcessor).resumeBlockchainDownloading();
        verify(databaseManager).shutdown();
        verify(zip).extract(anyString(), anyString(), anyBoolean());
    }

    @Test
    void testRecoverWhenShardingWasNotEnabled() {
        doReturn(mock(HeightConfig.class)).when(blockchainConfig).getCurrentConfig();

        shardService.recoverSharding();

        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testRecoverWhenRecoveryIsNull() {
        HeightConfig config = mock(HeightConfig.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(config).isShardingEnabled();
        shardService.recoverSharding();

        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testRecoverWhenRecoveryIsCompleted() {
        HeightConfig config = mock(HeightConfig.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(config).isShardingEnabled();
        doReturn(new ShardRecovery(MigrateState.COMPLETED)).when(shardRecoveryDao).getLatestShardRecovery();

        shardService.recoverSharding();

        verifyZeroInteractions(shardMigrationExecutor);
    }

    @Test
    void testRecover() {
        HeightConfig config = mock(HeightConfig.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(config).isShardingEnabled();
        doReturn(new ShardRecovery(MigrateState.MAIN_DB_BACKUPED)).when(shardRecoveryDao).getLatestShardRecovery();
        doReturn(new Shard(1L, new byte[32], ShardState.FULL, 100, new byte[32], new long[0], new int[0], new int[0], new byte[32])).when(shardDao).getLastShard();

        shardService.recoverSharding();

        verify(shardMigrationExecutor).executeAllOperations();
    }

    @Test
    void testRecoverWhenDbWasRestoredFromBackupButShardingDisabledInCmd() {
        HeightConfig config = mock(HeightConfig.class);
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(config).isShardingEnabled();
        ShardRecovery recovery = new ShardRecovery(MigrateState.INIT);
        recovery.setShardRecoveryId(2L);
        doReturn(recovery).when(shardRecoveryDao).getLatestShardRecovery();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
        doReturn(new Shard(1L, 2000)).when(shardDao).getLastShard();

        shardService.recoverSharding();

        verify(shardDao).hardDeleteShard(1L);
        verify(shardRecoveryDao).hardDeleteShardRecovery(2L);
        verifyZeroInteractions(shardMigrationExecutor);
    }


    @Test
    void testGetAllShards() {
        doReturn(List.of()).when(shardDao).getAllShard();
        List<Shard> allShards = shardService.getAllShards();

        assertEquals(List.of(), allShards);
        verify(shardDao).getAllShard();
    }


    @Test
    void testAllCompletedShards() {
        doReturn(List.of()).when(shardDao).getAllCompletedShards();

        List<Shard> allShards = shardService.getAllCompletedShards();

        assertEquals(List.of(), allShards);
        verify(shardDao).getAllCompletedShards();
    }

    @Test
    void testAllCompletedOrArchivedShards() {
        List<Shard> expected = List.of(new Shard(1L, 20), new Shard(2L, 30));
        doReturn(expected).when(shardDao).getAllCompletedOrArchivedShards();

        List<Shard> allShards = shardService.getAllCompletedOrArchivedShards();

        assertEquals(expected, allShards);
        verify(shardDao).getAllCompletedOrArchivedShards();
    }
}
