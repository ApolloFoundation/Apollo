package com.apollocurrency.aplwallet.apl.core.service.state.exchange;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.exchange.ExchangeRequestTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.impl.ExchangeRequestServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeBuyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemExchangeSell;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.ExchangeRequestTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRequestServiceTest {

    ExchangeRequestService service;
    ExchangeRequestTestData td;
    BlockTestData blockTestData;
    @Mock
    private ExchangeRequestTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<ExchangeRequest> exchangeRequestIteratorToStreamConverter;

    @BeforeEach
    void setUp() {
        td = new ExchangeRequestTestData();
        blockTestData = new BlockTestData();
        service = new ExchangeRequestServiceImpl(table, blockChainInfoService, exchangeRequestIteratorToStreamConverter);
    }

    @Test
    void getAllExchangeRequestsStream() {
        //GIVEN
        DbIterator<ExchangeRequest> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAll(anyInt(), anyInt());
        Stream<ExchangeRequest> expected = Stream.of(td.EXCHANGE_REQUEST_6, td.EXCHANGE_REQUEST_5, td.EXCHANGE_REQUEST_4);
        doReturn(expected).when(exchangeRequestIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<ExchangeRequest> result = service.getAllExchangeRequestsStream(anyInt(), anyInt());
        assertNotNull(result);

        //THEN
        verify(table).getAll(anyInt(), anyInt());
        verify(exchangeRequestIteratorToStreamConverter).apply(dbIt);
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
    void getExchangeRequest() {
        //GIVEN
        doReturn(td.EXCHANGE_REQUEST_3).when(table).get(any(DbKey.class));
        //WHEN
        ExchangeRequest result = service.getExchangeRequest(anyLong());
        assertNotNull(result);
        assertEquals(td.EXCHANGE_REQUEST_3, result);
        //THEN
        verify(table).get(any(DbKey.class));
    }

    @Test
    void getCurrencyExchangeRequestsStream() {
        //GIVEN
        DbIterator<ExchangeRequest> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        Stream<ExchangeRequest> expected = Stream.of(td.EXCHANGE_REQUEST_6, td.EXCHANGE_REQUEST_5, td.EXCHANGE_REQUEST_4);
        doReturn(expected).when(exchangeRequestIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<ExchangeRequest> result = service.getCurrencyExchangeRequestsStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(exchangeRequestIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountExchangeRequestsStream() {
        //GIVEN
        DbIterator<ExchangeRequest> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        Stream<ExchangeRequest> expected = Stream.of(td.EXCHANGE_REQUEST_6, td.EXCHANGE_REQUEST_5, td.EXCHANGE_REQUEST_4);
        doReturn(expected).when(exchangeRequestIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<ExchangeRequest> result = service.getAccountExchangeRequestsStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(exchangeRequestIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountCurrencyExchangeRequestsStream() {
        //GIVEN
        DbIterator<ExchangeRequest> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(
            any(),
            anyInt(), anyInt());
        Stream<ExchangeRequest> expected = Stream.of(td.EXCHANGE_REQUEST_6, td.EXCHANGE_REQUEST_5, td.EXCHANGE_REQUEST_4);
        doReturn(expected).when(exchangeRequestIteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<ExchangeRequest> result = service.getAccountCurrencyExchangeRequestsStream(
            td.EXCHANGE_REQUEST_6.getAccountId(), td.EXCHANGE_REQUEST_6.getCurrencyId(), 0, Integer.MAX_VALUE);
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(
            any(),
            anyInt(), anyInt());
        verify(exchangeRequestIteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void addExchangeRequest() {
        Transaction transaction = mock(Transaction.class);
        MonetarySystemExchangeBuyAttachment attachment = mock(MonetarySystemExchangeBuyAttachment.class);
        doReturn(blockTestData.BLOCK_10).when(blockChainInfoService).getLastBlock();

        //WHEN
        service.addExchangeRequest(transaction, attachment);

        //THEN
        verify(table).insert(any(ExchangeRequest.class));
    }

    @Test
    void testAddExchangeRequest() {
        Transaction transaction = mock(Transaction.class);
        MonetarySystemExchangeSell attachment = mock(MonetarySystemExchangeSell.class);
        doReturn(blockTestData.BLOCK_10).when(blockChainInfoService).getLastBlock();

        //WHEN
        service.addExchangeRequest(transaction, attachment);

        //THEN
        verify(table).insert(any(ExchangeRequest.class));
    }
}