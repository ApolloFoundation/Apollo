/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockchainProcessorState;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Task to restore prunable data for downloaded blocks
 */

@Slf4j
public class RestorePrunableDataTask implements Runnable {

    private final BlockchainProcessorState blockchainProcessorState;
    private final PeersService peersService;
    private final PrunableRestorationService prunableRestorationService;
    private final BlockchainConfig blockchainConfig;
    private final TransactionProcessor transactionProcessor;

    public RestorePrunableDataTask(BlockchainProcessorState blockchainProcessorState, PeersService peersService, PrunableRestorationService prunableRestorationService,
                                   BlockchainConfig blockchainConfig, TransactionProcessor transactionProcessor) {
        this.blockchainProcessorState = blockchainProcessorState;
        this.peersService = peersService;
        this.prunableRestorationService = prunableRestorationService;
        this.blockchainConfig = blockchainConfig;
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    public void run() {
        Peer peer = null;
        try {
            //
            // Locate an archive peer
            //
            List<Peer> peersList = peersService.getPeers(chkPeer -> chkPeer.providesService(Peer.Service.PRUNABLE) &&
                !chkPeer.isBlacklisted() && chkPeer.getAnnouncedAddress() != null);
            while (!peersList.isEmpty()) {
                Peer chkPeer = peersList.get(ThreadLocalRandom.current().nextInt(peersList.size()));
                if (chkPeer.getState() != PeerState.CONNECTED) {
                    peersService.connectPeer(chkPeer);
                }
                if (chkPeer.getState() == PeerState.CONNECTED) {
                    peer = chkPeer;
                    break;
                }
            }
            if (peer == null) {
                log.debug("Cannot find any archive peers");
                return;
            }
            log.debug("Connected to archive peer " + peer.getHost());
            //
            // Make a copy of the prunable transaction list so we can remove entries
            // as we process them while still retaining the entry if we need to
            // retry later using a different archive peer
            //
            Set<Long> processing;
            Set<Long> prunableTransactions = prunableRestorationService.getPrunableTransactions(); // TODO: YL check correct work with prunables
            synchronized (prunableTransactions) {
                processing = new HashSet<>(prunableTransactions.size());
                processing.addAll(prunableTransactions);
            }
            log.debug("Need to restore " + processing.size() + " pruned data");
            //
            // Request transactions in batches of 100 until all transactions have been processed
            //
            while (!processing.isEmpty()) {
                //
                // Get the pruned transactions from the archive peer
                //
                JSONObject request = new JSONObject();
                JSONArray requestList = new JSONArray();
                synchronized (prunableTransactions) {
                    Iterator<Long> it = processing.iterator();
                    while (it.hasNext()) {
                        long id = it.next();
                        requestList.add(Long.toUnsignedString(id));
                        it.remove();
                        if (requestList.size() == 100)
                            break;
                    }
                }
                request.put("requestType", "getTransactions");
                request.put("transactionIds", requestList);
                request.put("chainId", blockchainConfig.getChain().getChainId());
                JSONObject response;
                try {
                    response = peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId());
                } catch (PeerNotConnectedException ex) {
                    response = null;
                }
                if (response == null) {
                    return;
                }
                //
                // Restore the prunable data
                //
                JSONArray transactions = (JSONArray) response.get("transactions");
                if (transactions == null || transactions.isEmpty()) {
                    return;
                }
                List<Transaction> processed = transactionProcessor.restorePrunableData(transactions);
                //
                // Remove transactions that have been successfully processed
                //
                synchronized (prunableTransactions) {
                    processed.forEach(transaction -> prunableTransactions.remove(transaction.getId()));
                }
            }
            log.debug("Done retrieving prunable transactions from " + peer.getHost());
        } catch (AplException.ValidationException e) {
            log.error("Peer " + peer.getHost() + " returned invalid prunable transaction", e);
            peer.blacklist(e);
        } catch (RuntimeException e) {
            log.error("Unable to restore prunable data", e);
        } finally {
            blockchainProcessorState.setRestoring(false);
            // TODO: YL check correct work with prunables
            log.debug("Remaining " + prunableRestorationService.getPrunableTransactions().size() + " pruned transactions");
        }
    }
}