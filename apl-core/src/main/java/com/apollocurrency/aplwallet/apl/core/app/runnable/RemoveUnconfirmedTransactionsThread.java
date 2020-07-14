/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class RemoveUnconfirmedTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final DatabaseManager databaseManager;
    private final UnconfirmedTransactionTable unconfirmedTransactionTable;
    private final TransactionProcessor transactionProcessor;
    private final TimeService timeService;
    private final GlobalSync globalSync;

    public RemoveUnconfirmedTransactionsThread(DatabaseManager databaseManager,
                                               UnconfirmedTransactionTable unconfirmedTransactionTable,
                                               TransactionProcessor transactionProcessor,
                                               TimeService timeService,
                                               GlobalSync globalSync) {
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
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }
}
