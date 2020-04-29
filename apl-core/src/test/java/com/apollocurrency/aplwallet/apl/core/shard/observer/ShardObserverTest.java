/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.BlockDao;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.env.config.ShardingSettings;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class ShardObserverTest {
    public static final int DEFAULT_SHARDING_FREQUENCY = 5_000;
    public static final int NOT_MULTIPLE_SHARDING_FREQUENCY = 4_999;
    public static final int DEFAULT_TRIM_HEIGHT = 100_000;
    @Mock
    BlockchainConfigUpdater blockchainConfigUpdater;
    @Mock
    ShardService shardService;
    Optional<ShardingSettings> shardingSettings;
    @Mock
    PropertiesHolder propertiesHolder;
    private ShardObserver shardObserver;

    @BeforeEach
    void setUp() {
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate", false);
//        Mockito.doReturn(4072*1024*1024L).when(mock(Runtime.class)).totalMemory(); // give it more then 3 GB
    }

    private void prepare() {
        shardObserver = new ShardObserver(blockchainConfigUpdater, shardService, propertiesHolder);
    }

    @Test
    void testSkipShardingWhenShardingIsDisabled() {
        prepare();
        shardingSettings = Optional.of (new ShardingSettings(false, DEFAULT_SHARDING_FREQUENCY));
        doReturn(shardingSettings).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(DEFAULT_TRIM_HEIGHT);

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

    @Test
    void testDoNotShardWhenMinRollbackHeightIsNotMultipleOfShardingFrequency() {
        prepare();
        shardingSettings = Optional.of (new ShardingSettings(true, NOT_MULTIPLE_SHARDING_FREQUENCY));
        doReturn(shardingSettings).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(DEFAULT_TRIM_HEIGHT);

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

    @Test
    void testDoNotShardWhenLastTrimHeightIsZero() {
        prepare();
        shardingSettings = Optional.of (new ShardingSettings(true, NOT_MULTIPLE_SHARDING_FREQUENCY));
        doReturn(shardingSettings).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(0);

        CompletableFuture<MigrateState> c = shardObserver.tryCreateShardAsync(0, Integer.MAX_VALUE);

        assertNull(c);
        verify(shardService, never()).tryCreateShardAsync(anyInt(), anyInt());
    }

    @Test
    void testShardSuccessful() throws ExecutionException, InterruptedException {
        prepare();
        shardingSettings = Optional.of (new ShardingSettings(true, DEFAULT_SHARDING_FREQUENCY));
        doReturn(shardingSettings).when(blockchainConfigUpdater).getShardingSettingsByTrimHeight(DEFAULT_TRIM_HEIGHT);

        CompletableFuture<MigrateState> completableFuture = Mockito.mock(CompletableFuture.class);
        when(completableFuture.get()).thenReturn(MigrateState.COMPLETED);
        doReturn(completableFuture).when(shardService).tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        CompletableFuture<MigrateState> state = shardObserver.tryCreateShardAsync(DEFAULT_TRIM_HEIGHT, Integer.MAX_VALUE);

        assertNotNull(state);
        assertNotNull(state.get());
        verify(shardService, times(1)).tryCreateShardAsync(anyInt(), anyInt());
    }

}
