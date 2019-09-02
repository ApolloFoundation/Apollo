/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.peer.PeerHttpServer;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ShardObserverTest {
    public static final int DEFAULT_SHARDING_FREQUENCY = 5_000;
    public static final int NOT_MULTIPLE_SHARDING_FREQUENCY = 4_999;
    public static final int DEFAULT_TRIM_HEIGHT = 100_000;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    ShardMigrationExecutor shardMigrationExecutor;
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    Blockchain blockchain;
    @Mock
    HeightConfig heightConfig;
    @Mock
    ShardDao shardDao;
    @Mock
    ShardRecoveryDao recoveryDao;
    @Mock
    PeerHttpServer peerHttpServer;
    
    PropertiesHolder propertiesHolder = new PropertiesHolder();
    {
        Properties properties = new Properties();
        properties.put("apl.noshardcreate", "false");
        propertiesHolder.init(properties);
    }
    
    private ShardObserver shardObserver;
    private Event firedEvent;

    @BeforeEach
    void setUp() {
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        
//        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
    }

    private void prepare() {
        firedEvent = mock(Event.class);

        shardObserver = new ShardObserver(blockchainProcessor, blockchainConfig,
                shardMigrationExecutor,
                shardDao, recoveryDao, propertiesHolder, blockchain, firedEvent);
    }

    @Test
    void testSkipShardingWhenShardingIsDisabled() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(false).when(heightConfig).isShardingEnabled();

        CompletableFuture<Boolean> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardMigrationExecutor, never()).executeAllOperations();
    }

    @Test
    void testDoNotShardWhenMinRollbackHeightIsNotMultipleOfShardingFrequency() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(NOT_MULTIPLE_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        CompletableFuture<Boolean> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardMigrationExecutor, never()).executeAllOperations();
    }

    @Test
    void testDoNotShardWhenLastTrimHeightIsZero() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();

        CompletableFuture<Boolean> c = shardObserver.tryCreateShardAsync(0, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardMigrationExecutor, never()).executeAllOperations();
    }
    @Disabled // 2 times call to ShardObserver.performSharding() line 117
    @Test
    void testShardSuccessful() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
//        doReturn(new byte[]{1,2}).when(shardMigrationExecutor).calculateHash(DEFAULT_TRIM_HEIGHT);

        boolean created = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE).get();

        assertTrue(created);
        verify(shardMigrationExecutor, times(1)).executeAllOperations();
        verify(firedEvent, times(1)).fire(true);
        verify(firedEvent, times(1)).fire(false);
    }

    @Test
    void testShardWhenShardExecutorThrowAnyException() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(firedEvent).when(firedEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
//        doReturn(new byte[]{1,2}).when(shardMigrationExecutor).calculateHash(DEFAULT_TRIM_HEIGHT);
        doThrow(new RuntimeException()).when(shardMigrationExecutor).executeAllOperations();
        CompletableFuture<Boolean> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertFalse(c.get());
        verify(shardMigrationExecutor, times(1)).executeAllOperations();
        verify(firedEvent, times(1)).fire(true);
        verify(firedEvent, times(1)).fire(false);
    }

    @Test
    void testSkipShardingDuringBlockchainScan() {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        doReturn(true).when(blockchainProcessor).isScanning();

        CompletableFuture<Boolean> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);

        verify(shardMigrationExecutor, never()).executeAllOperations();
        verify(firedEvent, never()).fire(true);
        verify(firedEvent, never()).fire(false);
    }

    @Test
    void testSkipSharding() throws InterruptedException, ExecutionException {
        prepare();
        doReturn(firedEvent).when(firedEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        AtomicBoolean shutdown = new AtomicBoolean(false);
        doAnswer((in) -> {
            while (!shutdown.get()) {
                Thread.sleep(1L);
            }
            return MigrateState.COMPLETED;
        }).when(shardMigrationExecutor).executeAllOperations();

        CompletableFuture<Boolean> shardFuture1 = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNotNull(shardFuture1);

        CompletableFuture<Boolean> shardFuture2 = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT + DEFAULT_SHARDING_FREQUENCY, Integer.MAX_VALUE);

        assertNull(shardFuture2);
        // assertFalse(shardFuture2.isDone());
        shutdown.set(true);

        Boolean result = shardFuture1.get();
        assertTrue(result);

        verify(shardMigrationExecutor, times(1)).executeAllOperations();
    }

    @Test
    void testSkipShardingWhenLastShardHaveSameHeight() throws InterruptedException, ExecutionException {
        prepare();
        doReturn(firedEvent).when(firedEvent).select(new AnnotationLiteral<TrimConfigUpdated>() {});
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        CompletableFuture<Boolean> shardFuture1 = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNotNull(shardFuture1);

        shardFuture1.get();

        doReturn(new Shard(100, DEFAULT_TRIM_HEIGHT)).when(shardDao).getLastShard();

        CompletableFuture<Boolean> shardFuture2 = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);
        CompletableFuture<Boolean> shardFuture3 = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(shardFuture2);
        assertNull(shardFuture3);

        verify(shardMigrationExecutor, times(1)).executeAllOperations();
    }
}
