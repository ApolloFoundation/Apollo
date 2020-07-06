/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RebroadcastTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private TimeService timeService;
    private UnconfirmedTransactionTable unconfirmedTransactionTable;
    private PeersService peers;
    private Blockchain blockchain;

    public RebroadcastTransactionsThread(TimeService timeService,
                                         UnconfirmedTransactionTable unconfirmedTransactionTable,
                                         PeersService peers) {
        this.timeService = Objects.requireNonNull(timeService);
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.peers = Objects.requireNonNull(peers);
    }

    @Override
    public void run() {
        try {
            try {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return;
                }
                List<Transaction> transactionList = new ArrayList<>();
                int curTime = timeService.getEpochTime();
                for (Transaction transaction : unconfirmedTransactionTable.getBroadcastedTransactions()) {
                    if (transaction.getExpiration() < curTime || lookupBlockchain().hasTransaction(transaction.getId())) {
                        unconfirmedTransactionTable.getBroadcastedTransactions().remove(transaction);
                    } else if (transaction.getTimestamp() < curTime - 30) {
                        transactionList.add(transaction);
                    }
                }

                if (transactionList.size() > 0) {
                    peers.sendToSomePeers(transactionList);
                }

            } catch (Exception e) {
                log.info("Error in transaction re-broadcasting thread", e);
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

    private Blockchain lookupBlockchain() {
        if (blockchain == null) {
            blockchain = CDI.current().select(Blockchain.class).get();
        }
        return blockchain;
    }
}
