/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;

import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessWaitingTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private TransactionProcessor transactionProcessor;

    public ProcessWaitingTransactionsThread(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        log.info("Created 'ProcessWaitingTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return;
                }
                transactionProcessor.processWaitingTransactions();
            } catch (Exception e) {
                log.info("Error processing waiting transactions", e);
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
