/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TrimObserverTest {
    TrimService trimService = mock(TrimService.class);
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(TrimObserver.class)
            .addBeans(MockBean.of(trimService, TrimService.class)
            ).build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    Event<TrimConfig> trimEvent;
    @Inject
    TrimObserver observer;

    @Test
    void testOnTrimConfigUpdated() {
        assertTrue(observer.isTrimDerivedTables());
        fireBlockAccepted(5000);
        fireBlockAccepted(6000);
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(false, true));

        assertFalse(observer.isTrimDerivedTables());
        assertEquals(0, observer.getTrimHeights().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(true, false));

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
    void testOnBlockAccepted() {
        fireBlockAccepted(4999);
        fireBlockAccepted(5000);
        fireBlockAccepted(6000);
        fireBlockAccepted(6001);
        waitTrim(List.of(5000, 6000));
    }

    @Test
    void testOnBlockAcceptedWithDisabledTrim() throws InterruptedException {
        fireBlockAccepted(4998);
        fireBlockAccepted(4999);
        fireBlockAccepted(5000);
        fireBlockAccepted(6000);
        doAnswer(invocation -> {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
            }).fire(new TrimConfig(false, false));
            return null;
        }).when(trimService).trimDerivedTables(5000, true);
        waitTrim(List.of(5000));
        assertFalse(observer.isTrimDerivedTables());
        Thread.sleep(4000);
        verifyNoMoreInteractions(trimService);
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(true, false));
        waitTrim(List.of(6000));
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

    void fireBlockAccepted(int height) {
        Block block = mock(Block.class);
        doReturn(height).when(block).getHeight();
        blockEvent.select(literal(BlockEventType.AFTER_BLOCK_ACCEPT)).fire(block);
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