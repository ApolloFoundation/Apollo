package com.apollocurrency.aplwallet.apl.core.service.state.order.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.AskOrderTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.AskOrder;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCAskOrderPlacementAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 4/13/2020
 */
@ExtendWith(MockitoExtension.class)
class AskOrderServiceImplTest {
    @SuppressWarnings("unchecked")
    private final IteratorToStreamConverter<AskOrder> converter =
        mock(IteratorToStreamConverter.class);
    @Mock
    private DatabaseManager databaseManager;
    @Mock
    private AskOrderTable askOrderTable;
    @Mock
    private Blockchain blockchain;

    private OrderService<AskOrder, CCAskOrderPlacementAttachment> orderService;

    @BeforeEach
    void setUp() {
        this.orderService = Mockito.spy(
            new AskOrderServiceImpl(
                databaseManager,
                askOrderTable,
                blockchain,
                converter
            )
        );
    }

    @Test
    void shouldGetCount() {
        //GIVEN
        final int count = 1;
        when(askOrderTable.getCount()).thenReturn(count);

        //WHEN
        final int countActual = orderService.getCount();

        //THEN
        assertEquals(count, countActual);
    }

    @Test
    void shouldGetOrder() {
        //GIVEN
        long orderId = 1L;
        final AskOrder order = mock(AskOrder.class);
        when(askOrderTable.getAskOrder(orderId)).thenReturn(order);

        //WHEN
        final AskOrder orderActual = orderService.getOrder(orderId);

        //THEN
        assertEquals(order, orderActual);
    }

    @Test
    void shouldGetAll() {
        //GIVEN
        final AskOrder order = mock(AskOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<AskOrder> dbIterator = mock(DbIterator.class);
        when(askOrderTable.getAll(from, to)).thenReturn(dbIterator);
        final Stream<AskOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<AskOrder> streamActual = orderService.getAll(from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetOrdersByAccount() {
        //GIVEN
        final long accountId = 5L;
        final AskOrder order = mock(AskOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<AskOrder> dbIterator = mock(DbIterator.class);
        when(askOrderTable.getManyBy(any(DbClause.LongClause.class), eq(from), eq(to))).thenReturn(dbIterator);
        final Stream<AskOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<AskOrder> streamActual =
            orderService.getOrdersByAccount(accountId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetOrdersByAccountAsset() {
        //GIVEN
        final long accountId = 5L;
        final long assetId = 10L;
        final AskOrder order = mock(AskOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<AskOrder> dbIterator = mock(DbIterator.class);
        when(askOrderTable.getManyBy(any(DbClause.class), eq(from), eq(to))).thenReturn(dbIterator);
        final Stream<AskOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<AskOrder> streamActual =
            orderService.getOrdersByAccountAsset(accountId, assetId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetSortedOrders() {
        //GIVEN
        final long assetId = 10L;
        final AskOrder order = mock(AskOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<AskOrder> dbIterator = mock(DbIterator.class);
        when(askOrderTable.getManyBy(
            any(DbClause.class), eq(from), eq(to), eq(AskOrderServiceImpl.ORDER))
        ).thenReturn(dbIterator);
        final Stream<AskOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<AskOrder> streamActual =
            orderService.getSortedOrders(assetId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetNextOrder() {
        //GIVEN
        final long assetId = 10L;
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        final AskOrder order = mock(AskOrder.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(askOrderTable.getNextOrder(dataSource, assetId)).thenReturn(order);

        //WHEN
        final AskOrder orderActual = orderService.getNextOrder(assetId);

        //THEN
        assertEquals(order, orderActual);
    }

    @Test
    void shouldAddOrder() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final CCAskOrderPlacementAttachment attachment = mock(CCAskOrderPlacementAttachment.class);
        final long txId = 10L;
        when(transaction.getId()).thenReturn(txId);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);
        final AskOrder order = new AskOrder(transaction, attachment, blockchain.getHeight());

        //WHEN
        orderService.addOrder(transaction, attachment);

        //THEN
        verify(askOrderTable).insert(eq(order));
    }

    @Test
    void removeOrder() {
        //GIVEN
        final long orderId = 10L;
        final AskOrder order = mock(AskOrder.class);
        when(askOrderTable.getAskOrder(orderId)).thenReturn(order);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);

        //WHEN
        orderService.removeOrder(orderId);

        //THEN
        verify(askOrderTable).deleteAtHeight(order, height);
    }

    @Test
    void updateQuantityATU() {
        //GIVEN
        final long quantityATU = 300;
        final AskOrder order = mock(AskOrder.class);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);
        doNothing().when(orderService).insertOrDeleteOrder(askOrderTable, quantityATU, order, height);

        //WHEN
        orderService.updateQuantityATU(quantityATU, order);

        //THEN
        verify(orderService).insertOrDeleteOrder(askOrderTable, quantityATU, order, height);
    }
}