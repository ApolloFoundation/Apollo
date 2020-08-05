/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FindTransactionServiceImplTest {
    @Mock
    DatabaseManager databaseManager;
    @Mock
    TransactionProcessor transactionProcessor;
    @Mock
    TransactionDao transactionDao;
    @Mock
    UnconfirmedTransactionTable unconfirmedTransactionTable;
    @Mock
    BlockChainInfoService blockChainInfoService;
    @Mock
    TxReceiptMapper txReceiptMapper;

    FindTransactionService findTransactionService;

    static int startTime = Convert2.toEpochTime(1596090615500L);
    static int endTime = Convert2.toEpochTime(1596182761726L);

    @BeforeEach
    void setUp() {

        findTransactionService = new FindTransactionServiceImpl(databaseManager, transactionProcessor, transactionDao, unconfirmedTransactionTable, blockChainInfoService, txReceiptMapper);

    }

    @Test
    void getAllUnconfirmedTransactionsStream() {
        //GIVEN
        UnconfirmedTransaction tr = mock(UnconfirmedTransaction.class);
        Stream<UnconfirmedTransaction> result = List.of(tr).stream();
        doReturn(result).when(unconfirmedTransactionTable).getAllUnconfirmedTransactions();

        //WHEN
        Stream<UnconfirmedTransaction> stream = findTransactionService.getAllUnconfirmedTransactionsStream();

        //THEN
        assertNotNull(stream);
        assertEquals(1, stream.count());
    }

    @Test
    void getAllUnconfirmedTransactionsCount() {
        //GIVEN
        doReturn(11).when(unconfirmedTransactionTable).getCount();

        //WHEN
        long result = findTransactionService.getAllUnconfirmedTransactionsCount();

        //THEN
        assertEquals(11, result);
    }

    @Test
    void findTransaction() {
        //GIVEN
        long transactionId = 111L;
        int height = 8000;
        TransactionalDataSource ds = mock(TransactionalDataSource.class);
        Transaction tx = mock(Transaction.class);
        doReturn(ds).when(databaseManager).getDataSource();
        doReturn(tx).when(transactionDao).findTransaction(transactionId, height, ds);

        //WHEN
        Optional<Transaction> result = findTransactionService.findTransaction(transactionId, height);

        //THEN
        assertNotNull(result.orElseThrow());
    }

    @Test
    void findUnconfirmedTransaction() {
        //GIVEN
        long transactionId = 111L;
        Transaction tx = mock(Transaction.class);
        doReturn(tx).when(transactionProcessor).getUnconfirmedTransaction(transactionId);

        //WHEN
        Optional<Transaction> result = findTransactionService.findUnconfirmedTransaction(transactionId);

        //THEN
        assertNotNull(result.orElseThrow());
    }

    @ParameterizedTest
    @MethodSource("provideAplQueryAndSize")
    void getTransactionsByQuery(AplQueryObject query, int targetSize) {
        //GIVEN
        UnconfirmedTransaction tr1 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(startTime - 1000).when(tr1).getTimestamp();
        UnconfirmedTransaction tr2 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(endTime - 100).when(tr2).getTimestamp();
        UnconfirmedTransaction tr3 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(endTime + 1000).when(tr3).getTimestamp();

        Stream<UnconfirmedTransaction> unconfirmedTransactionStream = List.of(tr1, tr2, tr3).stream();
        doReturn(unconfirmedTransactionStream).when(unconfirmedTransactionTable).getAllUnconfirmedTransactions();

        TxReceipt tx1 = mock(TxReceipt.class);
        TxReceipt tx2 = mock(TxReceipt.class);
        TxReceipt tx3 = mock(TxReceipt.class);
        TxReceipt tx4 = mock(TxReceipt.class);
        List<TxReceipt> txList = List.of(tx1, tx2, tx3, tx4);

        //getTransactions(List<Long> accounts, type, subtype, startTime, endTime, fromHeight, toHeight, String sortOrder, from, to)
        doReturn(txList).when(transactionDao).getTransactions(Collections.emptyList(),
            query.getType(), (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(), query.getFrom(), query.getTo());

        //WHEN
        List<TxReceipt> transactions = findTransactionService.getTransactionsByQuery(query, true);

        //THEN
        assertNotNull(transactions);
        assertEquals(targetSize, transactions.size());
    }

    @ParameterizedTest
    @MethodSource("provideAplQueryAndSize")
    void getTransactionsCountByQuery(AplQueryObject query, int targetSize) {
        //GIVEN
        UnconfirmedTransaction tr1 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(startTime - 1000).when(tr1).getTimestamp();
        UnconfirmedTransaction tr2 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(endTime - 100).when(tr2).getTimestamp();
        UnconfirmedTransaction tr3 = mock(UnconfirmedTransaction.class, withSettings().lenient());
        doReturn(endTime + 1000).when(tr3).getTimestamp();

        Stream<UnconfirmedTransaction> unconfirmedTransactionStream = List.of(tr1, tr2, tr3).stream();
        doReturn(unconfirmedTransactionStream).when(unconfirmedTransactionTable).getAllUnconfirmedTransactions();

        //getTransactions(List<Long> accounts, type, subtype, startTime, endTime, fromHeight, toHeight, String sortOrder, from, to)
        doReturn(4).when(transactionDao).getTransactionsCount(Collections.emptyList(),
            query.getType(), (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(), query.getFrom(), query.getTo());

        //WHEN
        long transactionsCount = findTransactionService.getTransactionsCountByQuery(query, true);

        //THEN
        assertEquals(targetSize, transactionsCount);
    }

    @ParameterizedTest
    @MethodSource("provideAplQueryAndSize")
    void getConfirmedTransactionsCountByQuery(AplQueryObject query, int targetSize) {
        //GIVEN
        //getTransactions(List<Long> accounts, type, subtype, startTime, endTime, fromHeight, toHeight, String sortOrder, from, to)
        doReturn(4).when(transactionDao).getTransactionsCount(Collections.emptyList(),
            query.getType(), (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(), query.getFrom(), query.getTo());

        //WHEN
        long transactionsCount = findTransactionService.getConfirmedTransactionsCountByQuery(query);

        //THEN
        assertEquals(4, transactionsCount);
    }

    /**
     * The query objects and target count of the transactions are supplied into unit test method
     *
     * @return query + size value for test
     */
    static Stream<Arguments> provideAplQueryAndSize() {
        return Stream.of(
            arguments(AplQueryObject.builder().accounts(Collections.emptyList()).type((byte) 0)
                .firstHeight(1000).lastHeight(-1)
                .startTime(startTime).endTime(endTime)//only one unconfirmed transaction
                .order(AplQueryObject.OrderByEnum.ASC)
                .build(), 5),
            arguments(AplQueryObject.builder().accounts(Collections.emptyList()).type((byte) 0)
                .firstHeight(1000).lastHeight(-1)
                .startTime(startTime - 2000).endTime(endTime + 2000)//all unconfirmed transaction
                .order(AplQueryObject.OrderByEnum.ASC)
                .build(), 7),
            arguments(AplQueryObject.builder().accounts(Collections.emptyList()).type((byte) 0)
                .firstHeight(1000).lastHeight(-1)
                .startTime(startTime - 2000).endTime(endTime + 1000)//only one unconfirmed transaction
                .order(AplQueryObject.OrderByEnum.ASC)
                .build(), 6),
            arguments(AplQueryObject.builder().accounts(Collections.emptyList()).type((byte) 0)
                .firstHeight(1000).lastHeight(1500)//no unconfirmed transaction, cause the lastHeight has been set
                .startTime(startTime).endTime(endTime)
                .order(AplQueryObject.OrderByEnum.ASC)
                .build(), 4)
        );
    }
}