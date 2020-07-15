/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ProcessTxsToBroadcastWhenConfirmed implements Runnable {

    private final TransactionProcessor transactionProcessor;
    private final UnconfirmedTransactionTable unconfirmedTransactionTable;
    private final TimeService timeService;
    private final Blockchain blockchain;

    public ProcessTxsToBroadcastWhenConfirmed(TransactionProcessor transactionProcessor,
                                              UnconfirmedTransactionTable unconfirmedTransactionTable,
                                              TimeService timeService,
                                              Blockchain blockchain) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.timeService = Objects.requireNonNull(timeService);
        this.blockchain = Objects.requireNonNull(blockchain);
        log.info("Created 'ProcessTxsToBroadcastWhenConfirmed' instance");
    }

    @Override
    public void run() {
        List<Transaction> txsToDelete = new ArrayList<>();
        unconfirmedTransactionTable.getTxToBroadcastWhenConfirmed().forEach((tx, uncTx) -> {
            try {
                if (uncTx.getExpiration() < timeService.getEpochTime() || tx.getExpiration() < timeService.getEpochTime()) {
                    log.debug("Remove expired tx {}, unctx {}", tx.getId(), uncTx.getId());
                    txsToDelete.add(tx);
                } else if (!hasTransaction(uncTx)) {
                    try {
                        transactionProcessor.broadcast(uncTx);
                    } catch (AplException.ValidationException e) {
                        log.debug("Unable to broadcast invalid unctx {}, reason {}", tx.getId(), e.getMessage());
                        txsToDelete.add(tx);
                    }
                } else if (blockchain.hasTransaction(uncTx.getId())) {
                    if (!hasTransaction(tx)) {
                        try {
                            transactionProcessor.broadcast(tx);
                        } catch (AplException.ValidationException e) {
                            log.debug("Unable to broadcast invalid tx {}, reason {}", tx.getId(), e.getMessage());
                        }
                    }
                    txsToDelete.add(tx);
                }
            } catch (Throwable e) {
                log.error("Unknown error during broadcasting {}", tx.getId());
            }
        });
        txsToDelete.forEach(unconfirmedTransactionTable.getTxToBroadcastWhenConfirmed()::remove);
    }

    private boolean hasTransaction(Transaction tx) {
        return transactionProcessor.getUnconfirmedTransaction(tx.getId()) != null || blockchain.hasTransaction(tx.getId());
    }
}
