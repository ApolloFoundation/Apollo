/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
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

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@Slf4j
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TrimObserverTest {
    TrimService trimService = mock(TrimService.class);
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    HeightConfig config = Mockito.mock(HeightConfig.class);
    Random random = Mockito.mock(Random.class);
    Blockchain blockchain = mock(Blockchain.class);
    {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(12).doReturn(809).when(random).nextInt(Constants.DEFAULT_TRIM_FREQUENCY - 1); // emulate random
    }

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(TrimObserver.class)
            .addBeans(MockBean.of(trimService, TrimService.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(random, Random.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class))
            .build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    Event<TrimConfig> trimEvent;
    @Inject
    TrimObserver observer;

    @BeforeEach
    void setUp() {
        doReturn(true).when(config).isShardingEnabled();
        doReturn(true).when(propertiesHolder).getBooleanProperty("apl.noshardcreate");
    }

    @Test
    void testOnTrimConfigUpdated() {
        doReturn(5000).when(config).getShardingFrequency();

        assertTrue(observer.isTrimDerivedTablesEnabled());
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        assertEquals(2, observer.getTrimHeights().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(false, true));

        assertFalse(observer.isTrimDerivedTablesEnabled());
        assertEquals(0, observer.getTrimHeights().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(true, false));

        assertTrue(observer.isTrimDerivedTablesEnabled());
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
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(false, true));

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
//        waitTrim(List.of(5000, 5190)); // doesn't work, test hangs here
    }

    @Test
    void testOnBlockPushedWithDisabledTrim() throws InterruptedException {
        doReturn(5000).when(config).getShardingFrequency();

        fireBlockPushed(4998);
        fireBlockPushed(4999);
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        assertEquals(2, observer.getTrimHeights().size());
        doAnswer(invocation -> {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
            }).fire(new TrimConfig(false, false));
            return null;
        }).when(trimService).trimDerivedTables(5000, true);
//        waitTrim(List.of(5000)); // doesn't work, test hangs here
//        assertFalse(observer.isTrimDerivedTables());
        Thread.sleep(4000);
        verify(trimService, times(1)).isTrimming();
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(true, false));
//        waitTrim(List.of(5190));
    }

    private void waitTrim(List<Integer> heights) {
        log.trace("WAIT for heights = [{}]", heights.size());
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

}