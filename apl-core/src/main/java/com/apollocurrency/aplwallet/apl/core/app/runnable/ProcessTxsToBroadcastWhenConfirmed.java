/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
//TODO make soft broadcast
public class ProcessTxsToBroadcastWhenConfirmed implements Runnable {

    private final MemPool memPool;
    private final TimeService timeService;
    private final Blockchain blockchain;

    public ProcessTxsToBroadcastWhenConfirmed(MemPool memPool,
                                              TimeService timeService,
                                              Blockchain blockchain) {
        this.memPool = Objects.requireNonNull(memPool);
        this.timeService = Objects.requireNonNull(timeService);
        this.blockchain = Objects.requireNonNull(blockchain);
        log.info("Created 'ProcessTxsToBroadcastWhenConfirmed' instance");
    }

    @Override
    public void run() {
        List<Transaction> txsToDelete = new ArrayList<>();
        memPool.getAllBroadcastWhenConfirmedTransactions().forEach((tx, uncTx) -> {
            try {
                int epochTime = timeService.getEpochTime();
                if (uncTx.getExpiration() < epochTime || tx.getExpiration() < epochTime) {
                    log.debug("Remove expired tx {}, unctx {}", tx.getId(), uncTx.getId());
                    txsToDelete.add(tx);
                } else if (!hasTransaction(uncTx)) {
                    try {
                        memPool.softBroadcast(uncTx);
                    } catch (AplException.ValidationException e) {
                        log.debug("Unable to broadcast invalid unctx {}, reason {}", tx.getId(), e.getMessage());
                        txsToDelete.add(tx);
                    }
                } else if (blockchain.hasTransaction(uncTx.getId())) {
                    if (!hasTransaction(tx)) {
                        try {
                            boolean broadcasted = memPool.softBroadcast(tx);
                            if (!broadcasted) {
                                return;
                            }
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
        memPool.removeBroadcastedWhenConfirmedTransaction(txsToDelete);
    }

    private boolean hasTransaction(Transaction tx) {
        return memPool.getUnconfirmedTransaction(tx.getId()) != null || blockchain.hasTransaction(tx.getId());
    }
}
