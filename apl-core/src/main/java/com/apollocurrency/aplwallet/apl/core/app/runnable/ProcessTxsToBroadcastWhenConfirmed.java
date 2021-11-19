/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ProcessTxsToBroadcastWhenConfirmed implements Runnable {

    private final TimeService timeService;
    private final TransactionProcessor processor;
    private final Blockchain blockchain;
    private final MemPool memPool;

    public ProcessTxsToBroadcastWhenConfirmed(TransactionProcessor processor,
                                              MemPool memPool,
                                              TimeService timeService,
                                              Blockchain blockchain) {
        this.processor = Objects.requireNonNull(processor);
        this.timeService = Objects.requireNonNull(timeService);
        this.blockchain = Objects.requireNonNull(blockchain);
        this.memPool = memPool;
        log.info("Created 'ProcessTxsToBroadcastWhenConfirmed' instance");
    }

    @Override
    public void run() {
        List<Transaction> txsToDelete = new ArrayList<>();
        memPool.getAllBroadcastWhenConfirmed().forEach((tx, uncTx) -> {
            try {
                int epochTime = timeService.getEpochTime();
                if (uncTx.getExpiration() < epochTime || tx.getExpiration() < epochTime) {
                    log.debug("Remove expired tx {}, unctx {}", tx.getId(), uncTx.getId());
                    txsToDelete.add(tx);
                } else if (!hasTransaction(uncTx)) {
                    try {
                        processor.broadcast(uncTx);
                    } catch (AplTransactionValidationException e) {
                        log.debug("Unable to broadcast invalid unctx {}, reason {}", tx.getId(), e.getMessage());
                        txsToDelete.add(tx);
                    }
                } else if (blockchain.hasTransaction(uncTx.getId())) {
                    if (!hasTransaction(tx)) {
                        try {
                            processor.broadcast(tx);
                        } catch (AplTransactionValidationException e) {
                            log.debug("Unable to broadcast invalid tx {}, reason {}", tx.getId(), e.getMessage());
                        }
                    }
                    txsToDelete.add(tx);
                }
            } catch (Throwable e) {
                log.error("Unknown error during broadcasting {}", tx.getId());
            }
        });
        memPool.removeBroadcastedWhenConfirmed(txsToDelete);
    }

    private boolean hasTransaction(Transaction tx) {
        return memPool.contains(tx.getId()) || blockchain.hasTransaction(tx.getId());
    }
}
