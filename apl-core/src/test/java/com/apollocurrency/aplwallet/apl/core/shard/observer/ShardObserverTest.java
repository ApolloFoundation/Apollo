/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
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
    ShardService shardService;
    @Mock
    HeightConfig heightConfig;
    @Mock
    PropertiesHolder propertiesHolder;
    private ShardObserver shardObserver;
    private Event firedEvent;

    @BeforeEach
    void setUp() {
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
//        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
    }

    private void prepare() {

        shardObserver = new ShardObserver(blockchainConfig, shardService, propertiesHolder);
    }

    @Test
    void testSkipShardingWhenShardingIsDisabled() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(false).when(heightConfig).isShardingEnabled();

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

    @Test
    void testDoNotShardWhenMinRollbackHeightIsNotMultipleOfShardingFrequency() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(NOT_MULTIPLE_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

    @Test
    void testDoNotShardWhenLastTrimHeightIsZero() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(NOT_MULTIPLE_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(0, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }
    @Disabled // 2 times call to ShardObserver.performSharding() line 117
    @Test
    void testShardSuccessful() throws ExecutionException, InterruptedException {
        prepare();
        doReturn(true).when(heightConfig).isShardingEnabled();
        doReturn(DEFAULT_SHARDING_FREQUENCY).when(heightConfig).getShardingFrequency();
        CompletableFuture<MigrateState> completableFuture = new CompletableFuture<>();
        doReturn(completableFuture).when(shardService).tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        MigrateState state = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE).get();

        assertNull(state);
        verify(shardService, times(1)).tryCreateShardAsync(anyInt(), anyInt());
//        verify(shardMigrationExecutor, times(1)).executeAllOperations();
//        verify(firedEvent, times(1)).fire(true);
//        verify(firedEvent, times(1)).fire(false);
    }

}
