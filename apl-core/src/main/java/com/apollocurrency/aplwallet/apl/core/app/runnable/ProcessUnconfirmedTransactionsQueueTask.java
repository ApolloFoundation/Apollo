/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ProcessUnconfirmedTransactionsQueueTask implements Runnable {
    private final MemPool memPool;
    private final BatchSizeCalculator batchSizeCalculator;
    private final TransactionValidator validator;
    private final UnconfirmedTransactionProcessingService processingService;
    private final DatabaseManager databaseManager;

    public ProcessUnconfirmedTransactionsQueueTask(MemPool memPool, TransactionValidator validator, UnconfirmedTransactionProcessingService processingService, DatabaseManager databaseManager) {
        this.memPool = Objects.requireNonNull(memPool);
        this.validator = validator;
        this.processingService = processingService;
        this.databaseManager = databaseManager;
        this.batchSizeCalculator = new BatchSizeCalculator(1000, 2, 1024);
        log.info("Created 'ProcessUnconfirmedTransactionsQueueTask' instance");
    }

    @Override
    public void run() {
        try {
            broadcastBatch();

        } catch (Throwable e) {
            log.error("Error during processing unconfirmed transaction from queue ", e);
        }
    }

    void broadcastBatch() {
        DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), ()-> {
            int number = batchSizeCalculator.currentBatchSize();
            List<UnconfirmedTransaction> transactions = collectBatch(number);
            if (!transactions.isEmpty()) {
                batchSizeCalculator.startTiming(System.currentTimeMillis(), number);
                try {
                    log.debug("Pending processing batch size {}, transactions {}", number, transactions.size());
                    for (UnconfirmedTransaction transaction : transactions) {
                        log.trace("Processing transaction {}", transaction.getId());
                        UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(transaction);
                        if (validationResult.isOk()) {
                            try {
                                validator.validateSignatureWithTxFeeLessStrict(transaction);
                                validator.validateLightly(transaction);
                                boolean added = memPool.addProcessed(transaction);
                                if (!added) {

                                    log.warn("Unable to add new unconfirmed transaction {}, mempool is full", transaction.getId());
                                }
                            } catch (AplException.ValidationException e) {
                                log.trace("Invalid transaction {}, reason {}", transaction.getId(), e.getMessage());
                            }
                        }
                    }
                } finally {
                    batchSizeCalculator.stopTiming(System.currentTimeMillis());
                }
            }
        });
    }

    private List<UnconfirmedTransaction> collectBatch(int number) {
        List<UnconfirmedTransaction> collectedTxs = new ArrayList<>();
        int allowedBatch = calculateAllowedBatch(number);
        if (allowedBatch == 0) { // do not loose existing transactions
            return collectedTxs;
        }
        int collected = 0;
        while (collected < allowedBatch) {
            TxRes tx = nextTxFromPendingQueue();
            if (tx.hasTransaction()) {
                collected++;
                collectedTxs.add(tx.getTx());
            } else if (!tx.hasNext) {
                break;
            }
        }
        return collectedTxs;
    }

    private int calculateAllowedBatch(int desirableBatch) {
        int pendingTxCount = memPool.processingQueueSize();
        if(pendingTxCount > 0) {
            // memPool.canSafelyAccept() make call to the db.
            return Math.min(memPool.canSafelyAccept(), Math.min(desirableBatch, pendingTxCount));
        } else {
            return pendingTxCount;
        }
    }

    TxRes nextTxFromPendingQueue() {
        if (memPool.processingQueueSize() > 0) { // try to not lock
            UnconfirmedTransaction tx = memPool.nextProcessingTx();
            return new TxRes(tx, true);
        } else {
            return new TxRes(null, false);
        }
    }

    @Data
    private static class TxRes {
        private final UnconfirmedTransaction tx;
        private final boolean hasNext;

        public boolean hasTransaction() {
            return tx != null;
        }
    }
}
