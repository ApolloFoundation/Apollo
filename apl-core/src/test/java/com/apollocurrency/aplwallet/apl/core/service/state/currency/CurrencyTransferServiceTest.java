/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTransferTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyTransferServiceImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyTransfer;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencyTransferTestData;
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
class CurrencyTransferServiceTest {

    CurrencyTransferService service;
    CurrencyTransferTestData td;
    BlockTestData blockTestData;
    @Mock
    private CurrencyTransferTable table;
    @Mock
    private BlockChainInfoService blockChainInfoService;
    @Mock
    private IteratorToStreamConverter<CurrencyTransfer> iteratorToStreamConverter;

    @BeforeEach
    void setUp() {
        td = new CurrencyTransferTestData();
        service = new CurrencyTransferServiceImpl(table, blockChainInfoService, iteratorToStreamConverter);
    }

    @Test
    void getAllTransfersStream() {
        //GIVEN
        DbIterator<CurrencyTransfer> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAll(anyInt(), anyInt());
        Stream<CurrencyTransfer> expected = Stream.of(td.TRANSFER_1, td.TRANSFER_2, td.TRANSFER_3);
        doReturn(expected).when(iteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<CurrencyTransfer> result = service.getAllTransfersStream(anyInt(), anyInt());
        assertNotNull(result);

        //THEN
        verify(table).getAll(anyInt(), anyInt());
        verify(iteratorToStreamConverter).apply(dbIt);
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
    void getCurrencyTransfersStream() {
        //GIVEN
        DbIterator<CurrencyTransfer> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        Stream<CurrencyTransfer> expected = Stream.of(td.TRANSFER_3, td.TRANSFER_2, td.TRANSFER_1);
        doReturn(expected).when(iteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<CurrencyTransfer> result = service.getCurrencyTransfersStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getManyBy(any(DbClause.LongClause.class), anyInt(), anyInt());
        verify(iteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountCurrencyTransfersStream() {
        //GIVEN
        DbIterator<CurrencyTransfer> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAccountCurrencyTransfers(anyLong(), anyInt(), anyInt());
        Stream<CurrencyTransfer> expected = Stream.of(td.TRANSFER_3, td.TRANSFER_2, td.TRANSFER_1);
        doReturn(expected).when(iteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<CurrencyTransfer> result = service.getAccountCurrencyTransfersStream(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getAccountCurrencyTransfers(anyLong(), anyInt(), anyInt());
        verify(iteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getAccountCurrencyTransfersStream2() {
        //GIVEN
        DbIterator<CurrencyTransfer> dbIt = mock(DbIterator.class);
        doReturn(dbIt).when(table).getAccountCurrencyTransfers(anyLong(), anyLong(), anyInt(), anyInt());
        Stream<CurrencyTransfer> expected = Stream.of(td.TRANSFER_3, td.TRANSFER_2, td.TRANSFER_1);
        doReturn(expected).when(iteratorToStreamConverter).apply(dbIt);

        //WHEN
        Stream<CurrencyTransfer> result = service.getAccountCurrencyTransfersStream(anyLong(), anyLong(), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(expected, result);

        //THEN
        verify(table).getAccountCurrencyTransfers(anyLong(), anyLong(), anyInt(), anyInt());
        verify(iteratorToStreamConverter).apply(dbIt);
    }

    @Test
    void getTransferCount() {
        //GIVEN
        doReturn(3).when(table).getCount(any(DbClause.LongClause.class));
        //WHEN
        int result = service.getTransferCount(anyLong());
        assertEquals(3, result);
        //THEN
        verify(table).getCount(any(DbClause.LongClause.class));
    }

    @Test
    void addTransfer() {
        blockTestData = new BlockTestData();
        Transaction transaction = mock(Transaction.class);
        MonetarySystemCurrencyTransfer attachment = mock(MonetarySystemCurrencyTransfer.class);
        doReturn(blockTestData.BLOCK_10).when(blockChainInfoService).getLastBlock();

        //WHEN
        service.addTransfer(transaction, attachment);

        //THEN
        verify(table).insert(any(CurrencyTransfer.class));
    }
}