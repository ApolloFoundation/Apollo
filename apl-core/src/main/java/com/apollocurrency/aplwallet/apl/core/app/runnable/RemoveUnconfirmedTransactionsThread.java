/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.TransactionHelper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.util.Objects;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class RemoveUnconfirmedTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final DatabaseManager databaseManager;
    private final TransactionProcessor transactionProcessor;
    private final TimeService timeService;
    private final MemPool memPool;

    public RemoveUnconfirmedTransactionsThread(DatabaseManager databaseManager,
                                               TransactionProcessor transactionProcessor,
                                               TimeService timeService,
                                               MemPool memPool) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.memPool = memPool;
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.timeService = Objects.requireNonNull(timeService);
        log.info("Created 'RemoveUnconfirmedTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return;
                }
                removeExpiredTransactions();
                removeNotValidTransactions();
            } catch (Exception e) {
                log.info("Error removing unconfirmed transactions", e);
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void removeNotValidTransactions() {
        TransactionHelper.executeInTransaction(databaseManager.getDataSource(), () -> {
            memPool.getAllProcessedStream().limit(100).forEach(e -> {
                if (!transactionProcessor.isFullyValidTransaction(e)) {
                    transactionProcessor.removeUnconfirmedTransaction(e);
                }
            });
        });
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

    void removeExpiredTransactions() {
        int epochTime = timeService.getEpochTime();
        int expiredTransactionsCount = memPool.countExpiredTxs(epochTime);
        if (expiredTransactionsCount > 0) {
            log.trace("Found {} unc txs to remove", expiredTransactionsCount);
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            TransactionHelper.executeInTransaction(dataSource, () -> memPool.getExpiredTxsStream(epochTime).forEach(e-> transactionProcessor.removeUnconfirmedTransaction(e.getTransaction())));
        }
    }
}
