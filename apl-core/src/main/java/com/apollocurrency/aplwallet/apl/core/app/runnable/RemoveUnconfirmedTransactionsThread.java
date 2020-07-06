/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveUnconfirmedTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private DatabaseManager databaseManager;
    private UnconfirmedTransactionTable unconfirmedTransactionTable;
    private TransactionProcessor transactionProcessor;
    private TimeService timeService;
    private GlobalSync globalSync;

    public RemoveUnconfirmedTransactionsThread(DatabaseManager databaseManager,
                                               UnconfirmedTransactionTable unconfirmedTransactionTable,
                                               TransactionProcessor transactionProcessor,
                                               TimeService timeService,
                                               GlobalSync globalSync
                                                     ) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.timeService = Objects.requireNonNull(timeService);
        this.globalSync = Objects.requireNonNull(globalSync);
        log.info("Created 'RemoveUnconfirmedTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return;
                }
                List<UnconfirmedTransaction> expiredTransactions = new ArrayList<>();
                try (DbIterator<UnconfirmedTransaction> iterator = unconfirmedTransactionTable.getManyBy(
                    new DbClause.IntClause("expiration", DbClause.Op.LT, timeService.getEpochTime()), 0, -1, "")) {
                    while (iterator.hasNext()) {
                        expiredTransactions.add(iterator.next());
                    }
                }
                if (expiredTransactions.size() > 0) {
                    globalSync.writeLock();
                    try {
                        TransactionalDataSource dataSource = databaseManager.getDataSource();
                        try {
                            dataSource.begin();
                            for (UnconfirmedTransaction unconfirmedTransaction : expiredTransactions) {
                                transactionProcessor.removeUnconfirmedTransaction(unconfirmedTransaction.getTransaction());
                            }
                            dataSource.commit();
                        } catch (Exception e) {
                            log.error(e.toString(), e);
                            dataSource.rollback();
                            throw e;
                        }
                    } finally {
                        globalSync.writeUnlock();
                    }
                }
            } catch (Exception e) {
                log.info("Error removing unconfirmed transactions", e);
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessorImpl.class).get();
        }
        return blockchainProcessor;
    }
}
