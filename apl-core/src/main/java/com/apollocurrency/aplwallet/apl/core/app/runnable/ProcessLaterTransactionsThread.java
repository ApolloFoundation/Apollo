/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.util.BatchSizeCalculator;
import lombok.extern.slf4j.Slf4j;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessLaterTransactionsThread implements Runnable {

    private final TransactionProcessor processor;
    private final BatchSizeCalculator batchSizeCalculator;

    public ProcessLaterTransactionsThread(TransactionProcessor processor) {
        this.processor = processor;
        this.batchSizeCalculator = new BatchSizeCalculator(1000, 16, 1024);
        log.info("Created 'ProcessLaterTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            batchSizeCalculator.doTimedOp(batchSize -> {
                try {
                    processor.processDelayedTxs(batchSize);
                } catch (Exception e) {
                    log.info("Error processing unconfirmed transactions", e);
                }
            });
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

}
