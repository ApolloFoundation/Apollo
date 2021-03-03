/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.config.PropertyProducer;
import com.apollocurrency.aplwallet.apl.core.config.TrimEventCommand;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
    WeldInitiator weld = WeldInitiator.from(TrimObserver.class, PropertyProducer.class, TrimConfig.class)
        .addBeans(MockBean.of(trimService, TrimService.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class))
        .build();
    @Inject
    Event<Block> blockEvent;
    @Inject
    Event<TrimEventCommand> trimEvent;
    @Inject
    TrimObserver observer;


    public TrimObserverTest() {
        doReturn(2000).when(propertiesHolder).getIntProperty("apl.maxRollback", 720);
        doReturn(-1).when(propertiesHolder).getIntProperty("apl.trimProcessingDelay", 500);
        doReturn(1000).when(propertiesHolder).getIntProperty("apl.trimFrequency", 1000);
    }

    @BeforeEach
    void setUp() {
        observer.setRandom(random);
    }

    @Test
    void testOnTrimConfigUpdated() {
        assertTrue(observer.trimEnabled());
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        assertEquals(2, observer.getTrimQueue().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimEventCommand(false, true));

        assertFalse(observer.trimEnabled());
        assertEquals(0, observer.getTrimQueue().size());

        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimEventCommand(true, false));

        assertTrue(observer.trimEnabled());
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
        }).fire(new TrimEventCommand(false, true));

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
        AtomicInteger stepCounter = mockBlockchainHeights(List.of(5000, 6000, 6014));

        fireBlockPushed(4999); // skipped
        fireBlockPushed(5000); // accepted
        fireBlockPushed(6000); // accepted
        fireBlockPushed(6001); // skipped

        CompletableFuture.runAsync(() -> {
            stepCounter.set(1);
            ThreadUtils.sleep(100);
            stepCounter.set(2);
            ThreadUtils.sleep(100);
            stepCounter.set(3);
        });
        waitTrim(List.of(5000, 6000));
    }

    @Test
    void testOnBlockPushedWithDisabledTrim() throws InterruptedException {
        doAnswer(invocation -> {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
            }).fire(new TrimEventCommand(false, false));
            return null;
        }).when(trimService).trimDerivedTables(5000);
        AtomicInteger stepCounter = mockBlockchainHeights(List.of(5000, 6000, 7001));

        fireBlockPushed(4998);
        fireBlockPushed(4999);
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        CompletableFuture.runAsync(() -> {
            stepCounter.set(1);
            ThreadUtils.sleep(100);
            stepCounter.set(2);
        });
        waitTrim(List.of(5000));
        assertFalse(observer.trimEnabled());
        fireBlockPushed(7000);
        stepCounter.set(3);
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimEventCommand(true, false));
        waitTrim(List.of(6000, 7000));
    }


    @Test
    void testOnBlockPushed_trimDelayAdjustements() {
        long beginningTime = System.currentTimeMillis();
        doReturn(0).when(random).nextInt(6); // simulating 1s delay
        observer.setTrimConfig(new TrimConfig(6, 1000));
        AtomicInteger stepCounter = mockBlockchainHeights(List.of(100000));

        fireBlockPushed(4998);
        fireBlockPushed(4999);
        fireBlockPushed(5000);
        fireBlockPushed(6000);
        fireBlockPushed(7000);

        assertEquals(List.of(5000, 6000, 7000), observer.getTrimQueue());

        fireBlockPushed(8000);
        fireBlockPushed(9000);
        fireBlockPushed(10000);
        fireBlockPushed(15000);
        fireBlockPushed(16000);
        stepCounter.set(1);

        waitTrim(List.of(5000, 6000, 7000, 8000, 9000, 10000, 15000, 16000));
        // verify at least 4 one second delays between trims, no delays for > 4 trimHeights accumulated
        assertTrue(System.currentTimeMillis() - beginningTime > 4000);
    }

    private AtomicInteger mockBlockchainHeights(List<Integer> heights) {
        AtomicInteger stepCounter = new AtomicInteger();
        ArrayList<Integer> modifiableHeights = new ArrayList<>(heights);
        modifiableHeights.add(0, 0);
        doAnswer(invocationOnMock -> modifiableHeights.get(stepCounter.get())).when(blockchain).getHeight();
        return stepCounter;
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