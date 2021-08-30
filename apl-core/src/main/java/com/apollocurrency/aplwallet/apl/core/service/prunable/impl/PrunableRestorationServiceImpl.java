/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.prunable.impl;

import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class PrunableRestorationServiceImpl implements PrunableRestorationService {

    private final Set<Long> prunableTransactions = new HashSet<>();

    private final DatabaseManager databaseManager;
    private final BlockchainConfig blockchainConfig;
    private final TransactionProcessor transactionProcessor;
    private final Blockchain blockchain;
    private final TimeService timeService;
    private final PrunableMessageService prunableMessageService;
    private final PrunableLoadingService prunableLoadingService;
    private final PeersService peersService;
    private volatile int lastRestoreTime = 0;

    @Inject
    public PrunableRestorationServiceImpl(DatabaseManager databaseManager,
                                          BlockchainConfig blockchainConfig,
                                          TransactionProcessor transactionProcessor,
                                          Blockchain blockchain,
                                          TimeService timeService,
                                          PrunableMessageService prunableMessageService,
                                          PrunableLoadingService prunableLoadingService, PeersService peersService) {
        this.databaseManager = databaseManager;
        this.blockchainConfig = blockchainConfig;
        this.transactionProcessor = transactionProcessor;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.prunableMessageService = prunableMessageService;
        this.prunableLoadingService = prunableLoadingService;
        this.peersService = peersService;
    }

    @Override
    public int restorePrunedData() {
        int now = timeService.getEpochTime();
        int minTimestamp = Math.max(1, now - blockchainConfig.getMaxPrunableLifetime());
        int maxTimestamp = Math.max(minTimestamp, now - blockchainConfig.getMinPrunableLifetime()) - 1;
        List<PrunableTransaction> transactionList = blockchain.findPrunableTransactions(minTimestamp, maxTimestamp);
        transactionList.forEach(prunableTransaction -> {
            long id = prunableTransaction.getId();
            if ((prunableTransaction.hasPrunableAttachment() && prunableTransaction.getTransactionType().isPruned(id)) ||
                prunableMessageService.isPruned(id, prunableTransaction.hasPrunablePlainMessage(), prunableTransaction.hasPrunableEncryptedMessage())) {
                synchronized (prunableTransactions) {
                    prunableTransactions.add(id);
                }
            }
        });
        if (!prunableTransactions.isEmpty()) {
            lastRestoreTime = 0;
        }

        synchronized (prunableTransactions) {
            return prunableTransactions.size();
        }
    }

    @Override
    public Transaction restorePrunedTransaction(long transactionId) {
        Transaction transaction = blockchain.getTransaction(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        boolean isPruned = false;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableLoadingService.loadPrunable(transaction, appendage, true);
            if ((appendage instanceof Prunable) &&
                !((Prunable) appendage).hasPrunableData()) {
                isPruned = true;
                break;
            }
        }
        if (!isPruned) {
            return transaction;
        }
        List<Peer> peersList = peersService.getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
            !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
        if (peersList.isEmpty()) {
            log.debug("Cannot find any archive peers");
            return null;
        }

        GetTransactionsRequest req = new GetTransactionsRequest(Set.of(transactionId), blockchainConfig.getChain().getChainId());
        for (Peer peer : peersList) {
            if (peer.getState() != PeerState.CONNECTED) {
                peersService.connectPeer(peer);
            }
            if (peer.getState() != PeerState.CONNECTED) {
                continue;
            }
            log.debug("Connected to archive peer " + peer.getHost());
            JSONObject response;
            try {
                //TODO https://firstb.atlassian.net/browse/APL-1633
                response = peer.send(req);
            } catch (PeerNotConnectedException ex) {
                response = null;
            }
            if (response == null) {
                continue;
            }
            JSONArray transactions = (JSONArray) response.get("transactions");
            if (transactions == null || transactions.isEmpty()) {
                continue;
            }
            try {
                List<Transaction> processed = transactionProcessor.restorePrunableData(transactions);
                if (processed.isEmpty()) {
                    continue;
                }
                synchronized (prunableTransactions) {
                    prunableTransactions.remove(transactionId);
                }
                return processed.get(0);
            } catch (AplException.NotValidException e) {
                log.error("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
                peer.blacklist(e);
            }
        }
        return null;
    }

    public Set<Long> getPrunableTransactions() {
        synchronized (prunableTransactions) {
            return prunableTransactions;
        }
    }

    public boolean remove(long transactionId) {
        return prunableTransactions.remove(transactionId);
    }
}
