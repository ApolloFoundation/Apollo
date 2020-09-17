/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.Vetoed;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Vetoed
public class PendingBroadcastTask implements Runnable {
    private final TransactionProcessor txProcessor;
    private final MemPool memPool;
    private final TransactionValidator validator;
    private final UnconfirmedTransactionProcessingService processingService;
    private final int maxBatchBroadcastSize;
    private final int minBatchBroadcastSize;
    private final static double batchSizeScaleStep = 0.01;
    private final static double batchSizeScaleCoef = 1.15;


    public PendingBroadcastTask(TransactionProcessor txProcessor, MemPool memPool, TransactionValidator validator, UnconfirmedTransactionProcessingService processingService) {
        this.txProcessor = txProcessor;
        this.memPool = memPool;
        this.validator = validator;
        this.processingService = processingService;
        maxBatchBroadcastSize = 1024;
        minBatchBroadcastSize = 40;
    }


    @Override
    public void run() {
        broadcastPendingQueue();
    }

    void broadcastPendingQueue() {
        while (true) {
            if (memPool.pendingBroadcastQueueLoad() > 0.05) {
                broadcastBatch();
            } else {
                doBroadcastOnePendingTx();
                ThreadUtils.sleep(5);
            }
        }
    }

    void broadcastBatch() {
        int batchSize = batchSize();
        List<Transaction> transactions = collectBatch(batchSize);
        txProcessor.broadcast(transactions);
        ThreadUtils.sleep(20);
    }

    int batchSize() {
        double load = memPool.pendingBroadcastQueueLoad();
        double factor = load / batchSizeScaleStep;
        int batchSize = minBatchBroadcastSize;
        batchSize = (int) (batchSize * Math.pow(batchSizeScaleCoef, factor));
        log.debug("Load factor {}, batch size {}", load, batchSize);
        return Math.min(batchSize, maxBatchBroadcastSize);
    }

    private List<Transaction> collectBatch(int number) {
        List<Transaction> collectedTxs = new ArrayList<>();
        if (!memPool.canSafelyAcceptTransactions()) { // do not loose existing transactions
            return collectedTxs;
        }
        int collected = 0;
        while (collected < number) {
            NextPendingTx tx = nextValidTxFromPendingQueue();
            if (tx.hasTransaction()) {
                collected++;
                collectedTxs.add(tx.getTx());
            } else if (!tx.hasNext) {
                break;
            }
        }
        return collectedTxs;
    }

    NextPendingTx nextValidTxFromPendingQueue() {
        Transaction tx = null;
        try {
            tx = memPool.nextSoftBroadcastTransaction();
        } catch (InterruptedException ignored) {
        }
        if (tx != null) {
            try {
                validator.validate(tx);
            } catch (AplException.ValidationException e) {
                log.debug("Invalid transaction was not broadcasted txId=" + tx.getId(), e);
                return new NextPendingTx(null, true);
            }
            UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(tx);
            if (!validationResult.isOk()) {
                return new NextPendingTx(null, true);
            }
            return new NextPendingTx(tx, true);
        }
        return new NextPendingTx(null, false);
    }

    @Data
    private static class NextPendingTx {
        private final Transaction tx;
        private final boolean hasNext;

        public boolean hasTransaction() {
            return tx != null;
        }
    }

    void doBroadcastOnePendingTx() {
        if (!memPool.canSafelyAcceptTransactions()) { // do not loose existing transactions
            return;
        }
        try {
            NextPendingTx tx = nextValidTxFromPendingQueue();
            try {
                if (tx.hasTransaction()) {
                    txProcessor.broadcast(tx.getTx());
                }
            } catch (AplException.ValidationException e) {
                log.debug("Failed to broadcast transaction txId=" + tx.getTx(), e);
            }
        } catch (Exception e) {
            log.error("Unknown exception during broadcast transactions ", e);
        }
    }
}
