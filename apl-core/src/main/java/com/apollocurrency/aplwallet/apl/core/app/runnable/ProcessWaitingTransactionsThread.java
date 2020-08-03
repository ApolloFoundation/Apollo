/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.util.Objects;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessWaitingTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final TransactionProcessor transactionProcessor;

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
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }
}
