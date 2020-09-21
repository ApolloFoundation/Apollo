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
    private final BatchSizeCalculator batchSizeCalculator;

    public PendingBroadcastTask(TransactionProcessor txProcessor, MemPool memPool, BatchSizeCalculator batchSizeCalculator, TransactionValidator validator, UnconfirmedTransactionProcessingService processingService) {
        this.txProcessor = txProcessor;
        this.memPool = memPool;
        this.validator = validator;
        this.processingService = processingService;
        this.batchSizeCalculator = batchSizeCalculator;
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
            }
        }
    }

    void broadcastBatch() {
        int batchSize = batchSize();
        List<Transaction> transactions = collectBatch(batchSize);
        if (transactions.isEmpty()) {
            return;
        }
        batchSizeCalculator.startTiming(batchSize);
        txProcessor.broadcast(transactions);
        batchSizeCalculator.stopTiming();
        ThreadUtils.sleep(50);
    }

    int batchSize() {
        int batchSize = batchSizeCalculator.currentBatchSize();
        log.debug("Load factor {}, batch size {}", memPool.pendingBroadcastQueueLoad(), batchSize);
        return batchSize;
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
        try {
            if (memPool.pendingBroadcastQueueSize() > 0) { // try to not lock
                Transaction tx = memPool.nextSoftBroadcastTransaction();
                validator.validate(tx);
                UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(tx);
                if (!validationResult.isOk()) {
                    return new NextPendingTx(null, true);
                }
                return new NextPendingTx(tx, true);
            } else {
                return new NextPendingTx(null, false);
            }
        } catch (InterruptedException ignored) { // should never happen for blocking queue and one processing thread
            return new NextPendingTx(null, false);
        } catch (AplException.ValidationException e) {
            log.debug("Invalid transaction was not broadcasted ", e);
            return new NextPendingTx(null, true);
        }
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
                    ThreadUtils.sleep(15);
                }
            } catch (AplException.ValidationException e) {
                log.debug("Failed to broadcast transaction txId=" + tx.getTx(), e);
            }
        } catch (Exception e) {
            log.error("Unknown exception during broadcast transactions ", e);
        }
    }
}
