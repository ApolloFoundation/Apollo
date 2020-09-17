/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTransactionProcessingService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.UnconfirmedTxValidationResult;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class PendingBroadcastTask implements Runnable{
    private final GlobalSync globalSync;
    private final TransactionProcessor txProcessor;
    private final MemPool memPool;
    private final TransactionValidator validator;
    private final UnconfirmedTransactionProcessingService processingService;

    @Inject
    public PendingBroadcastTask(GlobalSync globalSync, TransactionProcessor txProcessor, MemPool memPool, TransactionValidator validator, UnconfirmedTransactionProcessingService processingService) {
        this.globalSync = globalSync;
        this.txProcessor = txProcessor;
        this.memPool = memPool;
        this.validator = validator;
        this.processingService = processingService;
    }


    @Override
    public void run() {
        broadcastPendingQueue();
    }

    void broadcastPendingQueue() {
        while (true) {
            if (memPool.pendingBroadcastQueueSize() > 10) {
                globalSync.updateLock();
                try {
                    for (int i = 0; i < 10; i++) {
                        doBroadcastOnePendingTx();
                    }
                } finally {
                    globalSync.updateUnlock();
                }
                ThreadUtils.sleep(20);
            } else {
                doBroadcastOnePendingTx();
                ThreadUtils.sleep(5);
            }
        }
    }

    private void doBroadcastOnePendingTx() {
        if (!memPool.canSafelyAcceptTransactions()) { // do not loose existing transactions
            return;
        }
        try {
            Transaction tx = null;
            try {
                tx = memPool.nextSoftBroadcastTransaction();
            } catch (InterruptedException ignored) {}
            if (tx != null) {
                try {
                    validator.validate(tx);
                } catch (AplException.ValidationException e) {
                    log.debug("Invalid transaction was not broadcasted txId=" + tx.getId(), e);
                    return;
                }
                UnconfirmedTxValidationResult validationResult = processingService.validateBeforeProcessing(tx);
                if (!validationResult.isOk()) {
                    return;
                }
                try {
                    txProcessor.broadcast(tx);
                } catch (AplException.ValidationException e) {
                    log.debug("Failed to broadcast transaction txId=" + tx.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Unknown exception during broadcast transactions ", e);
        }
    }
}
