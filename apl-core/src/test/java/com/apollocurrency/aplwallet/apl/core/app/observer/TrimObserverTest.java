/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

@Slf4j
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TrimObserverTest {
    TrimService trimService = mock(TrimService.class);
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    HeightConfig config = Mockito.mock(HeightConfig.class);
    Random random = new Random(); /*Mockito.mock(Random.class)*/;

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(TrimObserver.class)
            .addBeans(MockBean.of(trimService, TrimService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(random, Random.class))
            .build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    Event<TrimConfig> trimEvent;
    @Inject
    TrimObserver observer;

    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(true).when(config).isShardingEnabled();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
    }

    @Test
    void testOnTrimConfigUpdated() {
        doReturn(5000).when(config).getShardingFrequency();

        assertTrue(observer.isTrimDerivedTables());
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        assertEquals(2, observer.getTrimHeights().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(false, true));

        assertFalse(observer.isTrimDerivedTables());
        assertEquals(0, observer.getTrimHeights().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(true, false));

        assertTrue(observer.isTrimDerivedTables());
    }

    @Test
    void testOnBlockScannedWith5000BlockLogging() {
        Block block = mock(Block.class);
        doReturn(5000).when(block).getHeight();

        blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(block);

        verify(trimService).doTrimDerivedTablesOnBlockchainHeight(5000, false);
    }

    @Test
    void testOnBlockScannedSkipTrim() {
        Block block = mock(Block.class);
        doReturn(6000).when(block).getHeight();
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(false, true));

        blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(block);

        verifyZeroInteractions(trimService);
    }

    @Test
    void testOnBlockScannedSkipTrimWhenBlockHeightIsNotMultipleOfFrequency() {
        Block block = mock(Block.class);
        doReturn(6001).when(block).getHeight();

        blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(block);

        verifyZeroInteractions(trimService);
    }

    @Test
    void testOnBlockPushed() {
        doReturn(5000).when(config).getShardingFrequency();

        fireBlockPushed(4999); // skippped
        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        fireBlockPushed(6001); // skipped
        List<Integer> generatedTrimHeights = observer.getTrimHeights();
        assertEquals(2, generatedTrimHeights.size());
        waitTrim(generatedTrimHeights);
    }

    @Test
    void testOnBlockPushedWithDisabledTrim() throws InterruptedException {
        doReturn(5000).when(config).getShardingFrequency();

        simulateFireBlockPushed(4998);
        simulateFireBlockPushed(4999);
        int randomHeight1 = simulateFireBlockPushed(5000);
        int randomHeight2 = simulateFireBlockPushed(6000);
        assertEquals(2, observer.getTrimHeights().size());
        doAnswer(invocation -> {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(false, false));
            return null;
        }).when(trimService).trimDerivedTables(randomHeight1, true);
        waitTrim(List.of(randomHeight1));
        assertFalse(observer.isTrimDerivedTables());
        Thread.sleep(4000);
        verifyNoMoreInteractions(trimService);
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(true, false));
        waitTrim(List.of(randomHeight2));
    }

    @Test
    void testOnBlockPushedShardingIsDisabledByProp() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(5000).when(config).getShardingFrequency();

        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        List<Integer> generatedTrimHeights = observer.getTrimHeights();
        assertEquals(2, generatedTrimHeights.size());
    }

    @Test
    void testOnBlockPushedShardingIsDisabledByConfig() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(5000).when(config).getShardingFrequency();

        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        List<Integer> generatedTrimHeights = observer.getTrimHeights();
        assertEquals(2, generatedTrimHeights.size());
    }

    @Test
    void testOnBlockPushedShardingIsDisabledByBothPropConfig() {
        doReturn(false).when(config).isShardingEnabled();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        List<Integer> generatedTrimHeights = observer.getTrimHeights();
        assertEquals(2, generatedTrimHeights.size());
    }

    @Test
    void testIncorrectConfigParams() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(999).when(config).getShardingFrequency();

        observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);
        assertThrows(RuntimeException.class, () -> observer.init());
    }

    private void waitTrim(List<Integer> heights) {
        while (true) {
            try {
                Thread.sleep(500);
                for (Integer height : heights) {
                    verify(trimService).trimDerivedTables(height, true);
                }
                break;
            }
            catch (Throwable ignored) {}
        }
    }

    void fireBlockPushed(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.BLOCK_PUSHED)).fire(block);
    }

    private AnnotationLiteral<BlockEvent> literal(BlockEventType blockEventType) {
        return new BlockEventBinding() {
            @Override
            public BlockEventType value() {
                return blockEventType;
            }
        };
    }

    private int simulateFireBlockPushed(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        return observer.onBlockPushed(block);
    }

    @Test
    void testNextShardHeightNormalZeroShards() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback");

        doReturn(5000).when(config).getShardingFrequency();
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, null);

        int nextTrimHeight1 = simulateFireBlockPushed(1000);
        log.debug("nextTrimHeight1 = {}", nextTrimHeight1);
        assertTrue(nextTrimHeight1 < 1000);
    }

    @Test
    void testNextShardHeightLower() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");

        doReturn(5000).when(config).getShardingFrequency();
        int nextTrimHeight1 = simulateFireBlockPushed(1000);
        log.debug("nextTrimHeight1 = {}", nextTrimHeight1);
        assertTrue(nextTrimHeight1 < 1000);
    }

    @Test
    void testNextShardHeightBelow() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);

        doReturn(5000).when(config).getShardingFrequency();
        random = Mockito.mock(Random.class);
        doReturn(12).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);

        int nextTrimHeight1 = simulateFireBlockPushed(6000);
        log.debug("nextTrimHeight1 = {}", nextTrimHeight1);
        assertTrue(nextTrimHeight1 == 5987);
    }

    @Test
    void testNextShardHeightHigher() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(5000).when(config).getShardingFrequency();
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);

        random = Mockito.mock(Random.class);
        doReturn(456).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);

        int nextTrimHeight1 = simulateFireBlockPushed(11000);
        log.debug("nextTrimHeight1 = {}", nextTrimHeight1);
        assertEquals(10543, nextTrimHeight1);
    }

    @Test
    void testNextShardHeightEqual() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(5000).when(config).getShardingFrequency();
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);

        random = Mockito.mock(Random.class);
        doReturn(999).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);

        int nextTrimHeight1 = simulateFireBlockPushed(11000);
        log.debug("nextTrimHeight1 = {}", nextTrimHeight1);
        assertEquals(10000, nextTrimHeight1);
    }

    @Test
    void testShardConfigChangesOnHeight() {
        // emulate changing config during processing
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);
        random = Mockito.mock(Random.class);
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);

        // push block before shard height changes to next config section
        doReturn(false).when(blockchainConfig).isJustUpdated();
        doReturn(50000).when(config).getShardingFrequency();
        doReturn(628).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random increase

        int nextTrimHeight = simulateFireBlockPushed(14000); // pushed block
        log.debug("nextTrimHeight = {}", nextTrimHeight);
        assertEquals(13371, nextTrimHeight);

        // e.g. config has changed at that point
        doReturn(3000).when(config).getShardingFrequency(); // emulate changed config
        doReturn(true).when(blockchainConfig).isJustUpdated();
        HeightConfig previousConfig = Mockito.mock(HeightConfig.class);
        doReturn(5000).when(previousConfig).getShardingFrequency();
        doReturn(Optional.of(previousConfig)).when(blockchainConfig).getPreviousConfig(); // we want read previous config
        doReturn(444).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY); // emulate random increase

        nextTrimHeight = simulateFireBlockPushed(15000);
        log.debug("nextTrimHeight = {}", nextTrimHeight);
        assertEquals(15000, nextTrimHeight);

        doReturn(555).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random increase
        nextTrimHeight = simulateFireBlockPushed(16000);
        log.debug("nextTrimHeight = {}", nextTrimHeight);
        assertEquals(15444, nextTrimHeight);
    }

    @Test
    void testEmulatedRandomGenerator() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);
        random = Mockito.mock(Random.class);
        this.observer = new TrimObserver(trimService, blockchainConfig, propertiesHolder, random);

        doReturn(false).when(blockchainConfig).isJustUpdated();
        doReturn(50000).when(config).getShardingFrequency();
        // here we check logic for RND generation
        doReturn(300).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random increase

        int nextTrimHeight = simulateFireBlockPushed(14000); // pushed block
        log.debug("nextTrimHeight = {}", nextTrimHeight);
        assertEquals(13699, nextTrimHeight);
        verify(random, times(1)).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1);
    }

}