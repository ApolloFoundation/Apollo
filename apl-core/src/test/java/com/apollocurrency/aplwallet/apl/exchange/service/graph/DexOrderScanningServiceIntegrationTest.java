package com.apollocurrency.aplwallet.apl.exchange.service.graph;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventBinding;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexCandlestickDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.OrderScanDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import java.util.concurrent.CompletableFuture;

import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.dec;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.CandlestickTestUtil.eOrder;
import static com.apollocurrency.aplwallet.apl.exchange.service.graph.DexOrderScanningService.SERVICE_NAME;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@EnableWeld
class DexOrderScanningServiceIntegrationTest {

    Blockchain blockchain = mock(Blockchain.class);
    TaskDispatchManager dispatchManager = mock(TaskDispatchManager.class);
    {
        doReturn(mock(TaskDispatcher.class)).when(dispatchManager).newScheduledDispatcher(SERVICE_NAME);
    }

    private DexOrderDao orderDao = mock(DexOrderDao.class);
    private ScanPerformer scanPerformer = mock(ScanPerformer.class);
    private OrderScanDao orderScanDao = mock(OrderScanDao.class);
    private DexCandlestickDao candlestickDao = mock(DexCandlestickDao.class);
    @Inject
    DexOrderScanningService service;
    @Inject
    Event<Block> blockEvent;
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(DexOrderScanningService.class)
            .addBeans(
                    MockBean.of(orderDao, DexOrderDao.class)
                    , MockBean.of(scanPerformer, ScanPerformer.class)
                    , MockBean.of(orderScanDao, OrderScanDao.class)
                    , MockBean.of(blockchain, Blockchain.class)
                    , MockBean.of(dispatchManager, TaskDispatchManager.class)
                    , MockBean.of(candlestickDao, DexCandlestickDao.class))
            .build();


    @Test
    void testStartBlockchainScan() {
        doReturn(51000).when(blockchain).getHeight();
        doAnswer((d) -> {
            ThreadUtils.sleep(200);
            return 100;
        }).when(scanPerformer).doIteration(DexCurrency.ETH, 1000, 100);
        CompletableFuture.supplyAsync(()-> {
            service.tryScan();
            return null;
        });
        Block block = mock(Block.class);
        doReturn(50900).when(block).getHeight();
        doReturn(8000).when(block).getTimestamp();
        doReturn(eOrder(10L, 10_000, dec("3.22"), 1000, 50000)).when(orderDao).getLastClosedOrderBeforeHeight(DexCurrency.ETH, 50901);

        blockEvent.select(literal(BlockEventType.RESCAN_BEGIN)).fire(block);
        verify(candlestickDao).removeAfterTimestamp(7999);
        verify(scanPerformer).doIteration(DexCurrency.ETH, 1000, 100);
        verify(scanPerformer).saveOrderScan(new OrderScan(DexCurrency.ETH, 10));
        verify(scanPerformer).saveOrderScan(new OrderScan(DexCurrency.PAX, 0));
        verifyNoMoreInteractions(scanPerformer, orderScanDao);
        ThreadUtils.sleep(200); // wait finish of prev tryScan()

        service.tryScan();
        verifyNoMoreInteractions(scanPerformer); // scan is disabled

        blockEvent.select(literal(BlockEventType.RESCAN_END)).fire(block); // enable scan
        doAnswer((d) -> 1).when(scanPerformer).doIteration(DexCurrency.ETH, 1000, 100);
        service.tryScan();

        verify(scanPerformer).doIteration(DexCurrency.PAX, 1000, 100);
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