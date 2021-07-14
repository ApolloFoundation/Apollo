/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  Verifies downloaded failed transactions against these transaction's statuses received from peers
 * @author Andrii Boiarskyi
 * @see GetMoreBlocksJob
 * @see com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl
 * @since 1.48.4
 */
@Slf4j
@Singleton
public class FailedTransactionsVerificationService {
    private static final String LAST_VERIFIED_BLOCK_KEY = "lastFailedTransactionsVerificationBlock";

    private final ExecutorService executor;
    private final Blockchain blockchain;
    private final PeersService peersService;
    private final BlockchainConfig blockchainConfig;
    private final OptionDAO optionDAO;
    private final TaskDispatchManager taskDispatchManager;

    private final int numberOfFailedTransactionConfirmations;
    @Setter
    private volatile int failedTransactionsPerRequest = 100;
    private List<Peer> connectedPublicPeers;
    @Getter
    private volatile boolean backgroundVerificationEnabled = true;

    @Inject
    public FailedTransactionsVerificationService(
        Blockchain blockchain,
        PeersService peersService,
        BlockchainConfig blockchainConfig,
        OptionDAO optionDAO, TaskDispatchManager taskDispatchManager,
        @Property(name = "apl.numberOfFailedTransactionConfirmations", defaultValue = "3")
            int numberOfFailedTransactionConfirmations,
        @Property(name = "apl.numberOfFailedTransactionsProcessingThreads", defaultValue = "10")
            int numberOfFailedTransactionsProcessingThreads
        ) {
        this.blockchainConfig = blockchainConfig;
        this.optionDAO = optionDAO;
        this.taskDispatchManager = taskDispatchManager;
        this.numberOfFailedTransactionConfirmations = numberOfFailedTransactionConfirmations;
        this.executor = new ThreadPoolExecutor(1, numberOfFailedTransactionsProcessingThreads,
            30_000L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(),
            new NamedThreadFactory("GetFailedTransactionsPeerSendingPool", true));
        this.blockchain = blockchain;
        this.peersService = peersService;
        init(); // cannot be called during @PostConstruct because TaskDispatchManager will be already launched
    }

    private void init() {
        int lastVerifiedBlockHeight = getLastVerifiedBlockHeight();
        Optional<Integer> failedTxsActivationHeightOpt = blockchainConfig.getFailedTransactionsAcceptanceActivationHeight();
        backgroundVerificationEnabled = failedTxsActivationHeightOpt.isPresent();
        if (lastVerifiedBlockHeight == 0 && backgroundVerificationEnabled) {
            Integer failedTxsActivationHeight = failedTxsActivationHeightOpt.get();
            log.info("Failed transactions verification will be started from {} height", failedTxsActivationHeight);
            saveLastVerifiedBlockHeight(failedTxsActivationHeight);
        }
        if (backgroundVerificationEnabled) {
            TaskDispatcher taskDispatcher = taskDispatchManager.newScheduledDispatcher(getClass().getSimpleName());
            taskDispatcher.schedule(Task.builder()
                .name("FailedTransactionVerificationTask")
                .task(this::verifyFailedTransactionsCatchingExceptions)
                .initialDelay(60_000)
                .delay(10_000)
                .build());
        }
        log.info("Background failed transactions verification is {} from height {}", backgroundVerificationEnabled ? "ENABLED" : "DISABLED", getLastVerifiedBlockHeight());
    }

    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block poppedBlock) {
        if (getLastVerifiedBlockHeight() >= poppedBlock.getHeight()) {
            log.info("Failed transaction verification gone higher than current blockchain height, will set to {} height for popped off block {}", poppedBlock.getHeight() - 1, poppedBlock.getStringId());
            saveLastVerifiedBlockHeight(poppedBlock.getHeight() - 1);
        }
    }

    private void verifyFailedTransactionsCatchingExceptions() {
        try {
            verifyFailedTransactions();
        } catch (RuntimeException e) {
            log.error("Error during failed transactions verification", e);
        }
    }

    private void verifyFailedTransactions() {
        connectedPublicPeers = Collections.synchronizedList(new ArrayList<>(peersService.getPublicPeers(PeerState.CONNECTED, true)));
        int lastVerifiedBlockHeight = getLastVerifiedBlockHeight();
        int blockLimit = Constants.MAX_AUTO_ROLLBACK * 2;
        List<Block> downloadedBlocks = blockchain.getBlocksAfter(lastVerifiedBlockHeight, blockLimit);
        if (downloadedBlocks.size() < blockLimit) {
            log.debug("Not enough blocks to validate failed transactions, got only {} after height {}, required at least {}", downloadedBlocks.size(), lastVerifiedBlockHeight, blockLimit);
            return;
        }
        Map<Long, VerifiedError> failedTxs = downloadedBlocks.stream()
            .limit(blockLimit / 2) // process only half of blocks to avoid duplicate processing for small forks
            .flatMap(e -> e.getTransactions().stream())
            .filter(e -> e.getErrorMessage().isPresent())
            .collect(Collectors.toConcurrentMap(Transaction::getId, e -> new VerifiedError(e.getErrorMessage().get())));
        if (failedTxs.isEmpty()) {
            log.info("No failed transactions downloaded to validate starting from height {} to current height {} ", lastVerifiedBlockHeight, blockchain.getHeight());
            return;
        }
        log.info("Selected {} failed transactions to verify failure cause, [{}]", failedTxs.size(), failedTxToString(failedTxs));
        List<GetTransactionsRequest> requests = getTransactionsRequestDivided(new ArrayList<>(failedTxs.keySet()));
        List<Future<?>> verificationJobs = new ArrayList<>();
        for (GetTransactionsRequest request : requests) {
            Future<?> transactionVerificationJob = executor.submit(() -> verifyForRequest(failedTxs, request));
            verificationJobs.add(transactionVerificationJob);
        }
        for (Future<?> verificationJob : verificationJobs) {
            try {
                verificationJob.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new AplCoreLogicException(e.toString(), e);
            }
        }
    }

    public int getLastVerifiedBlockHeight() {
        String value = optionDAO.get(LAST_VERIFIED_BLOCK_KEY);
        if (value == null) {
            return 0;
        } else {
            return Integer.parseInt(value);
        }
    }

    private void saveLastVerifiedBlockHeight(int height) {
        optionDAO.set(LAST_VERIFIED_BLOCK_KEY, String.valueOf(height));
    }

    private String failedTxToString(Map<Long, VerifiedError> failedTxs) {
        return failedTxs.entrySet()
            .stream()
            .map(e -> Long.toUnsignedString(e.getKey()) + ":" + e.getValue().getError())
            .collect(Collectors.joining(","));
    }

    private Set<Long> verifyForRequest(Map<Long, VerifiedError> failedTxs, GetTransactionsRequest request) {
        Set<Peer> alreadyUsedPeers = new HashSet<>();
        while (true) {
            Set<Long> notFullyConfirmedTxs = request.getTransactionIds()
                .stream()
                .filter(e -> failedTxs.get(e).count < numberOfFailedTransactionConfirmations)
                .collect(Collectors.toSet());
            if (notFullyConfirmedTxs.isEmpty()) {
                log.info("Transaction's statuses are verified for request: {}", request);
                return Set.of();
            }
            GetTransactionsRequest correctedRequest = request.clone();
            correctedRequest.setTransactionIds(notFullyConfirmedTxs);
            Optional<PeerGetTransactionsResponse> peerResponseOptional = sendToAnother(correctedRequest, alreadyUsedPeers);
            if (peerResponseOptional.isEmpty()) {
                log.warn("Not enough peers to get failed transaction statuses, connected peers {}, already used peers [{}], request: {}", connectedPublicPeers.size(), alreadyUsedPeers.stream().map(Peer::getHostWithPort).collect(Collectors.joining(",")), correctedRequest);
                return notFullyConfirmedTxs;
            }
            GetTransactionsResponse response = peerResponseOptional.get().getResponse();
            Peer peer = peerResponseOptional.get().getPeer();
            alreadyUsedPeers.add(peer);
            Set<Long> requestedIds = new HashSet<>(correctedRequest.getTransactionIds());
            if (response.getTransactions().size() > requestedIds.size()) {
                log.error("Possibly malicious {} peer detected: received too many transactions {} instead of {} ", peer.getHostWithPort(), response.getTransactions().size(), requestedIds.size());
                peer.blacklist("Too many transactions supplied for failed transactions validation");
                connectedPublicPeers.remove(peer);
                continue;
            }
            if (response.getTransactions().size() < requestedIds.size()) {
                Set<Long> receivedIds = response.getTransactions()
                    .stream()
                    .map(UnconfirmedTransactionDTO::getTransaction)
                    .map(Long::parseUnsignedLong)
                    .collect(Collectors.toSet());
                requestedIds.removeAll(receivedIds);
                log.debug("Peer {} is missing {} transactions for validation: {}", peer.getHostWithPort(), requestedIds.size(), requestedIds);
            }
            for (TransactionDTO tx : response.getTransactions()) {
                VerifiedError verifiedError = failedTxs.get(Long.parseUnsignedLong(tx.getTransaction()));
                if (verifiedError == null) {
                    log.error("Possibly malicious {} peer detected at height {}: {} is not expected transaction from GetTransactions request: {}", peer.getHostWithPort(), blockchain.getHeight(), tx.getTransaction(), correctedRequest);
                    peer.blacklist("Peer returned not expected transaction: " + tx.getTransaction());
                    connectedPublicPeers.remove(peer);
                    break;
                }
                if (!verifiedError.verify(tx.getErrorMessage())) {
                    log.warn("Blockchain inconsistency may occur. Transaction's {} validation & execution results into an error message '{}', " +
                        "which does not match to {} peer's result '{}'", tx.getTransaction(), verifiedError.getError(), tx.getErrorMessage(), peer.getHost());
                }
            }
        }
    }


    @Data
    private static class VerifiedError {
        private String error;
        private int count;

        public VerifiedError(String error) {
            this.error = error;
        }

        public synchronized boolean verify(String error) {
            boolean errEquals = this.error.equals(error);
            if (errEquals) {
                count++;
            }
            return errEquals;
        }
    }

    private Optional<PeerGetTransactionsResponse> sendToAnother(GetTransactionsRequest request, Set<Peer> excludedPeers) {
        HashSet<Peer> excludedPeersCopy = new HashSet<>(excludedPeers);
        while (true) {
            Optional<Peer> additionalPeerOpt = selectConnectedPeer(excludedPeersCopy);
            if (additionalPeerOpt.isEmpty()) {
                break;
            }
            Peer peer = additionalPeerOpt.get();
            try {
                GetTransactionsResponse response = peer.send(request, new GetTransactionsResponseParser());
                if (response == null) {
                    excludedPeersCopy.add(peer);
                    continue;
                }
                return Optional.of(new PeerGetTransactionsResponse(peer, response));
            } catch (PeerNotConnectedException e) {
                connectedPublicPeers.remove(peer);
                excludedPeersCopy.add(peer);
            }
        }
        return Optional.empty();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class PeerGetTransactionsResponse {
        private Peer peer;
        private GetTransactionsResponse response;
    }

    private List<GetTransactionsRequest> getTransactionsRequestDivided(List<Long> ids) {
        List<GetTransactionsRequest> requests = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += failedTransactionsPerRequest) {
            HashSet<Long> requestTransactionIds = new HashSet<>(ids.subList(i, Math.min(i + failedTransactionsPerRequest, ids.size())));
            GetTransactionsRequest request = new GetTransactionsRequest(requestTransactionIds, blockchainConfig.getChain().getChainId());
            requests.add(request);
        }
        return requests;
    }

    private Optional<Peer> selectConnectedPeer(Set<Peer> exclude) {
        Random random = new Random();
        ArrayList<Peer> connectedNotExcludedPeers = new ArrayList<>(connectedPublicPeers);
        connectedNotExcludedPeers.removeAll(exclude);
        if (connectedNotExcludedPeers.isEmpty()) {
            return Optional.empty();
        }
        Peer selectedPeer = connectedNotExcludedPeers.get(random.nextInt(connectedNotExcludedPeers.size()));
        return Optional.ofNullable(selectedPeer);
    }

}
