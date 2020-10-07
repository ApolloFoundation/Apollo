/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessLaterTransactionsThread implements Runnable {

    private final TransactionProcessor processor;

    public ProcessLaterTransactionsThread(TransactionProcessor processor) {
        this.processor = processor;
        log.info("Created 'ProcessLaterTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                processor.processDelayedTxs(100);
            } catch (Exception e) {
                log.info("Error processing unconfirmed transactions", e);
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

}
