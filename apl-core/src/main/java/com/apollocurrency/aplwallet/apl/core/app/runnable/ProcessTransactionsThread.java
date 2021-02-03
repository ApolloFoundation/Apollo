/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.p2p.request.GetUnconfirmedTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.respons.GetUnconfirmedTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetUnconfirmedTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessTransactionsThread implements Runnable {

    private BlockchainProcessor blockchainProcessor;
    private final TransactionProcessor transactionProcessor;
    private final MemPool memPool;
    private final BlockchainConfig blockchainConfig;
    private final PeersService peers;
    private final TransactionDTOConverter dtoConverter;

    public ProcessTransactionsThread(TransactionProcessor transactionProcessor,
                                     MemPool memPool,
                                     BlockchainConfig blockchainConfig,
                                     PeersService peers,
                                     TransactionTypeFactory transactionTypeFactory) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.memPool = Objects.requireNonNull(memPool);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.peers = Objects.requireNonNull(peers);
        this.dtoConverter = new TransactionDTOConverter(transactionTypeFactory);
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
                GetUnconfirmedTransactionsRequest request = new GetUnconfirmedTransactionsRequest(blockchainConfig.getChain().getChainId());

                List<String> exclude = new ArrayList<>();
                memPool.getAllProcessedIds().forEach(
                    transactionId -> exclude.add(Long.toUnsignedString(transactionId)));
                Collections.sort(exclude);

                request.setExclude(exclude);

                GetUnconfirmedTransactionsResponse response = peer.send(request, new GetUnconfirmedTransactionsResponseParser());

                if (response == null || CollectionUtil.isEmpty(response.unconfirmedTransactions)) {
                    return;
                }

                try {
                    List<Transaction> transactions = response.unconfirmedTransactions
                        .stream()
                        .map(dtoConverter::convert)
                        .collect(Collectors.toList());

                    log.trace("Will process {} txs from peer {}", transactions.size(), peer.getAnnouncedAddress());

                    transactionProcessor.processPeerTransactions(transactions);
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
