/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import javax.enterprise.inject.spi.CDI;
import java.util.Collections;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.UnconfirmedTransactionTable;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final TransactionProcessor transactionProcessor;
    private final UnconfirmedTransactionTable unconfirmedTransactionTable;
    private final BlockchainConfig blockchainConfig;
    private final PeersService peers;

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
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

}
