/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.exchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Stream;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.impl.ExchangeServiceImpl;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.ExchangeTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    private ExchangeService service;
    private ExchangeTestData td;
    private BlockTestData blockTestData;
    @Mock
    private ExchangeTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<Exchange> exchangeIteratorToStreamConverter;

    @BeforeEach
    void setUp() {
        td = new ExchangeTestData();
        blockTestData = new BlockTestData();
        service = spy(new ExchangeServiceImpl(table, blockChainInfoService, exchangeIteratorToStreamConverter));
    }

    @Test
    void addExchange() {
        //GIVEN
        Transaction tr = mock(Transaction.class);
        doReturn(1L).when(tr).getId();
        doReturn(blockTestData.BLOCK_11).when(blockChainInfoService).getLastBlock();
        doNothing().when(table).insert(isA(Exchange.class));

        //WHEN
        service.addExchange(tr, 1L, 1L, 1L, 1L, 2L, /*blockTestData.BLOCK_11, */10L);

        //THEN
        verify(table).insert(isA(Exchange.class));
    }

    @Test
    void getAllExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAll(anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getAllExchangesStream(anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getAll(anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getCount() {
        //GIVEN
        doReturn(3).when(table).getCount();

        //WHEN
        int result = service.getCount();
        assertEquals(3, result);

        //THEN
        verify(table).getCount();
    }

    @Test
    void getCurrencyExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getCurrencyExchangesStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getLastExchanges() {
        //GIVEN
        doReturn(td.ALL_EXCHANGE_ORDERED_BY_DBID).when(table).getLastExchanges(
            new long[]{td.EXCHANGE_0.getCurrencyId(), td.EXCHANGE_1.getCurrencyId(), td.EXCHANGE_2.getCurrencyId()});

        //WHEN
        List<Exchange> result = service.getLastExchanges(
            new long[]{td.EXCHANGE_0.getCurrencyId(), td.EXCHANGE_1.getCurrencyId(), td.EXCHANGE_2.getCurrencyId()});
        assertNotNull(result);
        assertEquals(6, result.size());

        //THEN
        verify(table).getLastExchanges(new long[]{td.EXCHANGE_0.getCurrencyId(), td.EXCHANGE_1.getCurrencyId(), td.EXCHANGE_2.getCurrencyId()});
        verify(exchangeIteratorToStreamConverter, never()).apply(any(DbIterator.class));
    }

    @Test
    void getAccountExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAccountExchanges(anyLong(), anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getAccountExchangesStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getAccountExchanges(anyLong(), anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getAccountCurrencyExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAccountCurrencyExchanges(anyLong(), anyLong(), anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getAccountCurrencyExchangesStream(anyLong(), anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getAccountCurrencyExchanges(anyLong(), anyLong(), anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getExchangesStream(anyLong());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getOfferExchangesStream() {
        //GIVEN
        DbIterator<Exchange> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        doReturn(Stream.of(td.EXCHANGE_0, td.EXCHANGE_1, td.EXCHANGE_2)).when(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));

        //WHEN
        Stream<Exchange> result = service.getOfferExchangesStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(3, result.count());

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(exchangeIteratorToStreamConverter).apply(any(DbIterator.class));
    }

    @Test
    void getExchangeCount() {
        //GIVEN
        doReturn(3).when(table).getCount(any(DbClause.LongClause.class));

        //WHEN
        int result = service.getExchangeCount(anyLong());
        assertEquals(3, result);

        //THEN
        verify(table).getCount(any(DbClause.LongClause.class));
    }
}