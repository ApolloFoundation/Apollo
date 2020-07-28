/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
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
    private final TransactionProcessor transactionProcessor;
    private final TransactionDao transactionDao;
    private final DatabaseManager databaseManager;

    @Inject
    public FindTransactionServiceImpl(DatabaseManager databaseManager,
                                      TransactionProcessor transactionProcessor,
                                      TransactionDao transactionDao) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.transactionDao = Objects.requireNonNull(transactionDao);
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
    public List<Transaction> getTransactionsByPeriod(final int timeStart, final int timeEnd) {

        Stream<Transaction> unconfirmedTransactionStream = transactionProcessor.getAllUnconfirmedTransactionsStream()
            .filter(transaction -> transaction.getTimestamp() > timeStart && transaction.getTimestamp() < timeEnd)
            .map(unconfirmedTransaction -> unconfirmedTransaction);

        Stream<Transaction> transactionStream = transactionDao.getTransactions((byte) -1, (byte) -1, timeStart, timeEnd,
            0, 0, "ASC", 0, -1);

        return Stream.concat(unconfirmedTransactionStream, transactionStream).collect(Collectors.toUnmodifiableList());
    }
}
