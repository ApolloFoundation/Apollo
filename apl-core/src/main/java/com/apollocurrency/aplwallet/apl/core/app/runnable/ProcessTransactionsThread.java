/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.Collections;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@Slf4j
public class ProcessTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private TransactionProcessor transactionProcessor;
    private UnconfirmedTransactionTable unconfirmedTransactionTable;
    private BlockchainConfig blockchainConfig;
    private PeersService peers;

    public ProcessTransactionsThread(TransactionProcessor transactionProcessor,
                                     UnconfirmedTransactionTable unconfirmedTransactionTable,
                                     BlockchainConfig blockchainConfig,
                                     PeersService peers) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.unconfirmedTransactionTable = Objects.requireNonNull(unconfirmedTransactionTable);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.peers = Objects.requireNonNull(peers);
        log.info("Created 'ProcessTransactionsThread' instance");
    }

    @Override
    public void run() {
        try {
            try {
                if (lookupBlockchainProcessor().isDownloading()) {
                    return;
                }
                Peer peer = peers.getAnyPeer(PeerState.CONNECTED, true);
                if (peer == null) {
                    return;
                }
                JSONObject request = new JSONObject();
                request.put("requestType", "getUnconfirmedTransactions");
                JSONArray exclude = new JSONArray();
                unconfirmedTransactionTable.getAllUnconfirmedTransactionIds().forEach(
                    transactionId -> exclude.add(Long.toUnsignedString(transactionId)));
                Collections.sort(exclude);
                request.put("exclude", exclude);
                request.put("chainId", blockchainConfig.getChain().getChainId());
                JSONObject response = peer.send(JSON.prepareRequest(request), blockchainConfig.getChain().getChainId());
                if (response == null) {
                    return;
                }
                JSONArray transactionsData = (JSONArray) response.get("unconfirmedTransactions");
                if (transactionsData == null || transactionsData.size() == 0) {
                    return;
                }
                try {
                    transactionProcessor.processPeerTransactions(transactionsData);
                } catch (AplException.NotValidException | RuntimeException e) {
                    peer.blacklist(e);
                }
            } catch (Exception e) {
                log.info("Error processing unconfirmed transactions", e);
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
