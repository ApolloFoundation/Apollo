/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.prunable.impl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Prunable;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

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
    private final PeersService peersService;
    private volatile int lastRestoreTime = 0;

    @Inject
    public PrunableRestorationServiceImpl(DatabaseManager databaseManager,
                                          BlockchainConfig blockchainConfig,
                                          TransactionProcessor transactionProcessor,
                                          Blockchain blockchain,
                                          TimeService timeService,
                                          PrunableMessageService prunableMessageService,
                                          PeersService peersService) {
        this.databaseManager = databaseManager;
        this.blockchainConfig = blockchainConfig;
        this.transactionProcessor = transactionProcessor;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.prunableMessageService = prunableMessageService;
        this.peersService = peersService;
    }

    @Override
    public int restorePrunedData() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.begin()) {
            int now = timeService.getEpochTime();
            int minTimestamp = Math.max(1, now - blockchainConfig.getMaxPrunableLifetime());
            int maxTimestamp = Math.max(minTimestamp, now - blockchainConfig.getMinPrunableLifetime()) - 1;
            List<PrunableTransaction> transactionList =
                blockchain.findPrunableTransactions(con, minTimestamp, maxTimestamp);
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
            dataSource.commit();
        } catch (SQLException e) {
            dataSource.rollback();
            throw new RuntimeException(e.toString(), e);
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
        for (AbstractAppendix appendage : transaction.getAppendages(true)) {
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
        JSONObject json = new JSONObject();
        JSONArray requestList = new JSONArray();
        requestList.add(Long.toUnsignedString(transactionId));
        json.put("requestType", "getTransactions");
        json.put("transactionIds", requestList);
        json.put("chainId", blockchainConfig.getChain().getChainId());
        JSONStreamAware request = JSON.prepareRequest(json);
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
                response = peer.send(request, blockchainConfig.getChain().getChainId());
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
