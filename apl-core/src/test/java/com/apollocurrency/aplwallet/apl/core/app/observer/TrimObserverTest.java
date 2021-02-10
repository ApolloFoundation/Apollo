/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Slf4j
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TrimObserverTest {
    TrimService trimService = mock(TrimService.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    Random random = Mockito.mock(Random.class);
    Blockchain blockchain = mock(Blockchain.class);
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(TrimObserver.class, PropertyProducer.class)
        .addBeans(MockBean.of(trimService, TrimService.class))
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


    public TrimObserverTest() {
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);
        doReturn(-1).when(propertiesHolder).getIntProperty("apl.trimProcessingDelay", 500);
        doReturn(1000).when(propertiesHolder).getIntProperty("apl.trimFrequency", 1000);
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    void testOnTrimConfigUpdated() {
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

        verify(trimService).trimDerivedTables(5000);
    }

    @Test
    void testOnBlockScannedSkipTrim() {
        Block block = mock(Block.class);
        doReturn(6000).when(block).getHeight();
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(false, true));

        blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(block);

        verifyNoInteractions(trimService);
    }

    @Test
    void testOnBlockScannedSkipTrimWhenBlockHeightIsNotMultipleOfFrequency() {
        Block block = mock(Block.class);
        doReturn(6001).when(block).getHeight();

        blockEvent.select(literal(BlockEventType.BLOCK_SCANNED)).fire(block);

        verifyNoInteractions(trimService);
    }

    @Test
    void testOnBlockPushed() {

        fireBlockPushed(4999); // skippped
        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        fireBlockPushed(6001); // skipped
        CompletableFuture.runAsync(() -> {
            doReturn(5000).when(blockchain).getHeight();
            ThreadUtils.sleep(100);
            doReturn(6000).when(blockchain).getHeight();
            ThreadUtils.sleep(100);
            doReturn(6014).when(blockchain).getHeight();
        });
        waitTrim(List.of(5000, 6000));
    }

    @Test
    void testOnBlockPushedWithDisabledTrim() throws InterruptedException {
        doAnswer(invocation -> {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
            }).fire(new TrimConfig(false, false));
            return null;
        }).when(trimService).trimDerivedTables(5000);

        fireBlockPushed(4998);
        fireBlockPushed(4999);
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        CompletableFuture.runAsync(() -> {
            doReturn(5000).when(blockchain).getHeight();
            ThreadUtils.sleep(100);
            doReturn(6000).when(blockchain).getHeight();
        });
        waitTrim(List.of(5000));
        assertFalse(observer.isTrimDerivedTablesEnabled());
        fireBlockPushed(7000);
        doReturn(7001).when(blockchain).getHeight();
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(true, false));
        waitTrim(List.of(6000, 7000));
    }

    private void waitTrim(List<Integer> heights) {
        log.trace("WAIT for heights = [{}]", heights.size());
        while (true) {
            try {
                Thread.sleep(10);
                for (Integer height : heights) {
                    verify(trimService).trimDerivedTables(height);
                }
                Thread.sleep(10);
                break;
            } catch (Throwable ignored) {
            }
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