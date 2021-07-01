/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Remove not valid and expired transactions from the mempool
 */
@Slf4j
public class RemoveUnconfirmedTransactionsThread implements Runnable {

    private final DatabaseManager databaseManager;
    private final TransactionProcessor transactionProcessor;
    private final TimeService timeService;
    private final MemPool memPool;
    private final BatchSizeCalculator batchSizeCalculator;

    public RemoveUnconfirmedTransactionsThread(DatabaseManager databaseManager,
                                               TransactionProcessor transactionProcessor,
                                               TimeService timeService,
                                               MemPool memPool) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.memPool = memPool;
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.timeService = Objects.requireNonNull(timeService);
        this.batchSizeCalculator = new BatchSizeCalculator(100, 5, 1024);
        log.info("Created 'RemoveUnconfirmedTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                removeExpiredTransactions();
                batchSizeCalculator.doTimedOp(this::removeNotValidTransactions);
            } catch (Exception e) {
                log.info("Error removing unconfirmed transactions", e);
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void removeNotValidTransactions(int number) {
        List<Transaction> notValidTxs = new ArrayList<>();
        DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(),
            () -> CollectionUtil.forEach(memPool.getAllStream(0, number - 1), e -> {
            if (!transactionProcessor.isSufficientlyValidTransaction(e)) {
                transactionProcessor.removeUnconfirmedTransaction(e);
                notValidTxs.add(e);
            }
        }));
        if (!notValidTxs.isEmpty()) {
            log.info("Removed not valid txs: [{}]", notValidTxs.stream().map(Transaction::getId).map(Objects::toString).collect(Collectors.joining(",")));
        }
    }

    private void removeExpiredTransactions() {
        int epochTime = timeService.getEpochTime();
        int expiredTransactionsCount = memPool.getExpiredCount(epochTime);
        if (expiredTransactionsCount > 0) {
            log.info("Found {} unc txs to remove", expiredTransactionsCount);
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            DbTransactionHelper.executeInTransaction(dataSource, () -> CollectionUtil.forEach(memPool.getExpiredStream(epochTime), e -> transactionProcessor.removeUnconfirmedTransaction(e.getTransactionImpl())));
        }
    }
}
