/*
 * Copyright (c)  2019-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.p2p.request.GetUnconfirmedTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetUnconfirmedTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetUnconfirmedTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.rest.converter.TransactionDTOConverter;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class makes lookup of BlockchainProcessor
 */
@Slf4j
public class ProcessTransactionsThread implements Runnable {

    public static final int REMOVED_TXS_FETCH_LIMIT = 2000;
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
                                     TransactionBuilderFactory builderFactory) {
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor);
        this.memPool = Objects.requireNonNull(memPool);
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.peers = Objects.requireNonNull(peers);
        this.dtoConverter = new TransactionDTOConverter(builderFactory);
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
                List<String> exclude = Stream.concat(memPool.getAllIds().stream(), memPool.getAllRemoved(REMOVED_TXS_FETCH_LIMIT).stream())
                    .sorted(Long::compareTo)
                    .map(Long::toUnsignedString)
                    .collect(Collectors.toList());
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
                } catch (AplCoreLogicException  e) {
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
