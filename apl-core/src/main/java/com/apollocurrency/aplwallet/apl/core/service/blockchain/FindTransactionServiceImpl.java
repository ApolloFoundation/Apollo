/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.model.AplQueryObject;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Find transaction service. It doesn't support the sharding DB, uses only the main DB.*
 *
 * @author andrii.zinchenko@firstbridge.io
 */

@Slf4j
@Singleton
public class FindTransactionServiceImpl implements FindTransactionService {
    private final BlockChainInfoService blockChainInfoService;
    private final UnconfirmedTransactionTable unconfirmedTransactionTable;
    private final TransactionProcessor transactionProcessor;
    private final TransactionDao transactionDao;
    private final DatabaseManager databaseManager;
    private final TxReceiptMapper txReceiptMapper;
    private final IteratorToStreamConverter<UnconfirmedTransaction> streamConverter;

    @Inject
    public FindTransactionServiceImpl(DatabaseManager databaseManager,
                                      TransactionProcessor transactionProcessor,
                                      TransactionDao transactionDao,
                                      UnconfirmedTransactionTable unconfirmedTransactionTable,
                                      BlockChainInfoService blockChainInfoService,
                                      TxReceiptMapper txReceiptMapper) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.transactionDao = Objects.requireNonNull(transactionDao);
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.blockChainInfoService = Objects.requireNonNull(blockChainInfoService);
        this.txReceiptMapper = Objects.requireNonNull(txReceiptMapper);
        this.streamConverter = new IteratorToStreamConverter<>();
    }

    @Override
    public Stream<UnconfirmedTransaction> getAllUnconfirmedTransactionsStream() {
        return streamConverter.convert(unconfirmedTransactionTable.getAll(0, -1));
    }

    @Override
    public long getAllUnconfirmedTransactionsCount() {
        return unconfirmedTransactionTable.getCount();
    }

    @Override
    public Optional<Transaction> findTransaction(long transactionId, int height) {
        return Optional.ofNullable(transactionDao.findTransaction(transactionId, height, databaseManager.getDataSource()));
    }

    @Override
    public Optional<Transaction> findUnconfirmedTransaction(long transactionId) {
        return Optional.ofNullable(transactionProcessor.getUnconfirmedTransaction(transactionId));
    }

    @Override
    public List<TxReceipt> getTransactionsByPeriod(final int timeStart, final int timeEnd, String orderBy) {
        if (timeStart <= 0 || timeEnd <= 0) {
            throw new IllegalArgumentException("Wrong time stamp value: timeStart=" + timeStart + " timeEnd=" + timeEnd);
        }
        Objects.requireNonNull(orderBy);

        Stream<Transaction> unconfirmedTransactionStream = getAllUnconfirmedTransactionsStream()
            .filter(transaction -> transaction.getTimestamp() > timeStart && transaction.getTimestamp() < timeEnd)
            .map(unconfirmedTransaction -> unconfirmedTransaction);

        int height = blockChainInfoService.getHeight();

        Stream<TxReceipt> transactionStream = transactionDao.getTransactions((byte) -1, (byte) -1, timeStart, timeEnd,
            0, 0, orderBy, 0, -1)
            .stream().peek(txReceipt -> {
                txReceipt.setConfirmations(Math.max(0, height - txReceipt.getHeight()));
                txReceipt.setStatus(TxReceipt.StatusEnum.CONFIRMED);
                }
            );

        return Stream.concat(unconfirmedTransactionStream.map(txReceiptMapper), transactionStream)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public long getTransactionsCountByPeriod(int timeStart, int timeEnd, String orderBy) {
        if (timeStart <= 0 || timeEnd <= 0) {
            throw new IllegalArgumentException("Wrong time stamp value: timeStart=" + timeStart + " timeEnd=" + timeEnd);
        }
        Objects.requireNonNull(orderBy);

        long unconfirmedTxCount = getAllUnconfirmedTransactionsStream()
            .filter(transaction -> transaction.getTimestamp() > timeStart && transaction.getTimestamp() < timeEnd)
            .count();

        long txCount = transactionDao.getTransactionsCount((byte) -1, (byte) -1, timeStart, timeEnd,
            0, 0, orderBy, 0, -1);

        return unconfirmedTxCount + txCount;
    }

    @Override
    public List<TxReceipt> getTransactionsByQuery(AplQueryObject query) {
        if (query.getStartTime() <= 0 || query.getEndTime() <= 0) {
            throw new IllegalArgumentException("Wrong time stamp values: timeStart=" + query.getStartTime() + " timeEnd=" + query.getEndTime());
        }
        Stream<Transaction> unconfirmedTransactionStream = null;
        if (query.getLastHeight() <= 0) {
            unconfirmedTransactionStream = getAllUnconfirmedTransactionsStream()
                .filter(transaction -> transaction.getTimestamp() > query.getStartTime() && transaction.getTimestamp() < query.getEndTime())
                .map(unconfirmedTransaction -> unconfirmedTransaction);
        }

        int height = blockChainInfoService.getHeight();

        Stream<TxReceipt> transactionStream = transactionDao.getTransactions((byte) -1, (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(),
            query.getFrom(), query.getTo())
            .stream().peek(txReceipt -> {
                    txReceipt.setConfirmations(Math.max(0, height - txReceipt.getHeight()));
                    txReceipt.setStatus(TxReceipt.StatusEnum.CONFIRMED);
                }
            );

        return unconfirmedTransactionStream != null ?
            Stream.concat(unconfirmedTransactionStream.map(txReceiptMapper), transactionStream)
                .collect(Collectors.toUnmodifiableList())
            : transactionStream.collect(Collectors.toUnmodifiableList());
    }

    @Override
    public long getTransactionsCountByQuery(AplQueryObject query) {
        if (query.getStartTime() <= 0 || query.getEndTime() <= 0) {
            throw new IllegalArgumentException("Wrong time stamp values: timeStart=" + query.getStartTime() + " timeEnd=" + query.getEndTime());
        }
        long unconfirmedTxCount = getAllUnconfirmedTransactionsStream()
            .filter(transaction -> transaction.getTimestamp() > query.getStartTime() && transaction.getTimestamp() < query.getEndTime())
            .count();

        long txCount = transactionDao.getTransactionsCount((byte) -1, (byte) -1,
            query.getStartTime(), query.getEndTime(),
            query.getFirstHeight(), query.getLastHeight(),
            query.getOrder().name(),
            query.getFrom(), query.getTo());

        return unconfirmedTxCount + txCount;
    }
}
