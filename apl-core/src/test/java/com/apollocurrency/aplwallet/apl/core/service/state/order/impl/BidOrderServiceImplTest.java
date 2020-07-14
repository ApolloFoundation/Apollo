package com.apollocurrency.aplwallet.apl.core.service.state.order.impl;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.order.BidOrderTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.order.BidOrder;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ColoredCoinsBidOrderPlacement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 4/13/2020
 */
@ExtendWith(MockitoExtension.class)
class BidOrderServiceImplTest {
    @SuppressWarnings("unchecked")
    private final IteratorToStreamConverter<BidOrder> converter =
        mock(IteratorToStreamConverter.class);
    @Mock
    private DatabaseManager databaseManager;
    @Mock
    private BidOrderTable bidOrderTable;
    @Mock
    private Blockchain blockchain;

    private OrderService<BidOrder, ColoredCoinsBidOrderPlacement> orderService;

    @BeforeEach
    void setUp() {
        this.orderService = spy(
            new BidOrderServiceImpl(
                databaseManager,
                bidOrderTable,
                blockchain,
                converter
            )
        );
    }

    @Test
    void shouldGetCount() {
        //GIVEN
        final int count = 1;
        when(bidOrderTable.getCount()).thenReturn(count);

        //WHEN
        final int countActual = orderService.getCount();

        //THEN
        assertEquals(count, countActual);
    }

    @Test
    void shouldGetOrder() {
        //GIVEN
        long orderId = 1L;
        final BidOrder order = mock(BidOrder.class);
        when(bidOrderTable.getBidOrder(orderId)).thenReturn(order);

        //WHEN
        final BidOrder orderActual = orderService.getOrder(orderId);

        //THEN
        assertEquals(order, orderActual);
    }

    @Test
    void shouldGetAll() {
        //GIVEN
        final BidOrder order = mock(BidOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<BidOrder> dbIterator = mock(DbIterator.class);
        when(bidOrderTable.getAll(from, to)).thenReturn(dbIterator);
        final Stream<BidOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<BidOrder> streamActual = orderService.getAll(from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetOrdersByAccount() {
        //GIVEN
        final long accountId = 5L;
        final BidOrder order = mock(BidOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<BidOrder> dbIterator = mock(DbIterator.class);
        when(bidOrderTable.getManyBy(any(DbClause.LongClause.class), eq(from), eq(to))).thenReturn(dbIterator);
        final Stream<BidOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<BidOrder> streamActual =
            orderService.getOrdersByAccount(accountId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetOrdersByAccountAsset() {
        //GIVEN
        final long accountId = 5L;
        final long assetId = 10L;
        final BidOrder order = mock(BidOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<BidOrder> dbIterator = mock(DbIterator.class);
        when(bidOrderTable.getManyBy(any(DbClause.class), eq(from), eq(to))).thenReturn(dbIterator);
        final Stream<BidOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<BidOrder> streamActual =
            orderService.getOrdersByAccountAsset(accountId, assetId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetSortedOrders() {
        //GIVEN
        final long assetId = 10L;
        final BidOrder order = mock(BidOrder.class);
        int from = 0;
        int to = 100;
        @SuppressWarnings("unchecked") final DbIterator<BidOrder> dbIterator = mock(DbIterator.class);
        when(bidOrderTable.getManyBy(
            any(DbClause.class), eq(from), eq(to), eq(BidOrderServiceImpl.ORDER))
        ).thenReturn(dbIterator);
        final Stream<BidOrder> streamExpected = Stream.of(order);
        when(converter.convert(dbIterator)).thenReturn(streamExpected);

        //WHEN
        final Stream<BidOrder> streamActual =
            orderService.getSortedOrders(assetId, from, to);

        //THEN
        assertEquals(streamExpected, streamActual);
    }

    @Test
    void shouldGetNextOrder() {
        //GIVEN
        final long assetId = 10L;
        final TransactionalDataSource dataSource = mock(TransactionalDataSource.class);
        final BidOrder order = mock(BidOrder.class);
        when(databaseManager.getDataSource()).thenReturn(dataSource);
        when(bidOrderTable.getNextOrder(dataSource, assetId)).thenReturn(order);

        //WHEN
        final BidOrder orderActual = orderService.getNextOrder(assetId);

        //THEN
        assertEquals(order, orderActual);
    }

    @Test
    void shouldAddOrder() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final ColoredCoinsBidOrderPlacement attachment = mock(ColoredCoinsBidOrderPlacement.class);
        final long txId = 10L;
        when(transaction.getId()).thenReturn(txId);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);
        final BidOrder order = new BidOrder(transaction, attachment, blockchain.getHeight());

        //WHEN
        orderService.addOrder(transaction, attachment);

        //THEN
        verify(bidOrderTable).insert(eq(order));
    }

    @Test
    void removeOrder() {
        //GIVEN
        final long orderId = 10L;
        final BidOrder order = mock(BidOrder.class);
        when(bidOrderTable.getBidOrder(orderId)).thenReturn(order);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);

        //WHEN
        orderService.removeOrder(orderId);

        //THEN
        verify(bidOrderTable).deleteAtHeight(order, height);
    }

    @Test
    void updateQuantityATU() {
        //GIVEN
        final long quantityATU = 300;
        final BidOrder order = mock(BidOrder.class);
        final int height = 1040;
        when(blockchain.getHeight()).thenReturn(height);
        doNothing().when(orderService).insertOrDeleteOrder(bidOrderTable, quantityATU, order, height);

        //WHEN
        orderService.updateQuantityATU(quantityATU, order);

        //THEN
        verify(orderService).insertOrDeleteOrder(bidOrderTable, quantityATU, order, height);
    }
}