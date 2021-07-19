/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.runnable.GetMoreBlocksJob;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;
import com.apollocurrency.aplwallet.apl.core.exception.AplFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionNotFoundException;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  Verifies downloaded failed transactions against these transaction's statuses received from peers
 * @author Andrii Boiarskyi
 * @see GetMoreBlocksJob
 * @see FailedTransactionVerificationConfig
 * @see com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl
 * @since 1.48.4
 */
@Slf4j
@Singleton
public class FailedTransactionVerificationService {
    private static final String LAST_VERIFIED_BLOCK_KEY = "lastFailedTransactionsVerificationBlock";

    private final ExecutorService executor;
    private final Blockchain blockchain;
    private final PeersService peersService;
    private final OptionDAO optionDAO;
    private final TaskDispatchManager taskDispatchManager;
    private final FailedTransactionVerificationConfig config;

    @Setter
    private volatile int failedTransactionsPerRequest = 100;

    private volatile Result lastVerificationResult;

    @Inject
    public FailedTransactionVerificationService(
        Blockchain blockchain,
        PeersService peersService,
        OptionDAO optionDAO, TaskDispatchManager taskDispatchManager, FailedTransactionVerificationConfig config) {
        this.optionDAO = optionDAO;
        this.taskDispatchManager = taskDispatchManager;
        if (config.isEnabled()) {
            this.executor = new ThreadPoolExecutor(1, config.getThreads(),
                30_000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("GetFailedTransactionsPeerSendingPool", true));
        } else {
            this.executor = null;
        }
        this.blockchain = blockchain;
        this.peersService = peersService;
        this.config = config;
        init(); // cannot be called during @PostConstruct because TaskDispatchManager will be already launched
    }


    public void onBlockPopped(@Observes @BlockEvent(BlockEventType.BLOCK_POPPED) Block poppedBlock) {
        if (getLastVerifiedBlockHeight() >= poppedBlock.getHeight()) {
            log.info("Failed transaction verification gone higher than current blockchain height, will set to {} height for popped off block {}", poppedBlock.getHeight() - 1, poppedBlock.getStringId());
            saveLastVerifiedBlockHeight(poppedBlock.getHeight() - 1);
        }
    }

    public Optional<Result> getLastVerificationResult() {
        return Optional.ofNullable(lastVerificationResult);
    }

    /**
     * Verifies failed transaction by the given id
     * @param id transaction id to verify
     * @return empty {@link Result} when transaction is not failed or filled with verification results
     * @throws AplFeatureNotEnabledException when failed transaction acceptance is not enabled or transaction verification was disabled by node config
     * @throws AplTransactionNotFoundException when transaction by given id was not found in a blockchain
     */
    public synchronized Result verifyTransaction(long id) {
        if (!config.isEnabled()) {
            throw new AplFeatureNotEnabledException("Failed transactions processing", "txId= " + Long.toUnsignedString(id));
        }
        Transaction transaction = blockchain.getTransaction(id);
        if (transaction == null) {
            throw new AplTransactionNotFoundException(id, "Verify transaction op");
        }
        if (!transaction.isFailed()) {
            return new Result();
        }
        Verifier verifier = new Verifier(Map.of(id, transaction.getErrorMessage().orElseThrow(() -> new AplCoreContractViolationException("Failed transaction should have error message, id = " + transaction.getStringId()))));
        return verifier.verify();
    }

    public int getLastVerifiedBlockHeight() {
        String value = optionDAO.get(LAST_VERIFIED_BLOCK_KEY);
        if (value == null) {
            return 0;
        } else {
            return Integer.parseInt(value);
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    @EqualsAndHashCode
    public static class Result {
        private final Map<Long, VerifiedTransaction> verified;
        private final Map<Long, VerifiedTransaction> notVerified;

        public Result(Map<Long, VerifiedTransaction> failedTxs) {
            this.verified = failedTxs.entrySet()
                .stream()
                .filter(e -> e.getValue().isVerified())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.notVerified = failedTxs.entrySet()
                .stream()
                .filter(e -> !e.getValue().isVerified())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        public boolean isVerified(long id) {
            return this.verified.get(id) != null;
        }

        public Set<Long> allVerified() {
            return new HashSet<>(verified.keySet());
        }

        public Set<Long> allNotVerified() {
            return new HashSet<>(notVerified.keySet());
        }

        public Optional<VerifiedTransaction> get(long id) {
            if (verified.containsKey(id)) {
                return Optional.of(verified.get(id));
            }
            return Optional.ofNullable(notVerified.get(id));
        }

        public Result() {
            this.notVerified = Map.of();
            this.verified = Map.of();
        }

        public boolean isEmpty() {
            return verified.isEmpty() && notVerified.isEmpty();
        }
    }


    @AllArgsConstructor
    public static class VerifiedTransaction {
        @Getter
        private final long id;
        private final VerifiedError error;

        public int getCount() {
            return error.getCount();
        }

        public boolean verify(String error) {
            return this.error.verify(error);
        }

        public String getError() {
            return error.getError();
        }

        public boolean isVerified() {
            return error.isVerified();
        }
    }

    private void init() {
        if (config.isEnabled()) {
            TaskDispatcher taskDispatcher = taskDispatchManager.newScheduledDispatcher(getClass().getSimpleName());
            taskDispatcher.schedule(Task.builder()
                .name("FailedTransactionVerificationTask")
                .task(this::verifyFailedTransactionsCatchingExceptions)
                .initialDelay(60_000)
                .delay(10_000)
                .build());
        }
        log.info("Background failed transactions verification is {} from height {}", config.isEnabled() ? "ENABLED" : "DISABLED", getLastVerifiedBlockHeight());
    }

    private void verifyFailedTransactionsCatchingExceptions() {
        try {
            verifyFailedTransactions();
        } catch (RuntimeException e) {
            log.error("Error during failed transactions verification", e);
        }
    }

    private synchronized void verifyFailedTransactions() {
        int fromHeight = Math.max(config.getFailedTxsActivationHeight().orElseThrow(()-> new IllegalStateException("")), getLastVerifiedBlockHeight());
        int blockLimit = Constants.MAX_AUTO_ROLLBACK * 2;
        if (blockchain.getHeight() < fromHeight + blockLimit) {
            log.debug("Not enough blocks to validate failed transactions, minimum height for verification is {}, but current is {}", fromHeight + blockLimit, blockchain.getHeight());
            return;
        }
        List<Block> downloadedBlocks = blockchain.getBlocksAfter(fromHeight, blockLimit);
        Map<Long, String> txErrorMap = downloadedBlocks.stream()
            .limit(blockLimit / 2) // process only half of blocks to avoid duplicate processing for small forks
            .flatMap(e -> e.getTransactions().stream())
            .filter(e -> e.getErrorMessage().isPresent())
            .collect(Collectors.toMap(Transaction::getId, e -> e.getErrorMessage().get()));
        if (txErrorMap.isEmpty()) {
            log.info("No failed transactions downloaded to validate starting from height {} to current height {} ", fromHeight, blockchain.getHeight());
            return;
        }
        log.info("Selected {} failed transactions to verify failure cause, [{}]", txErrorMap.size(), failedTxToString(txErrorMap));
        Verifier verifier = new Verifier(txErrorMap);
        lastVerificationResult = verifier.verify();
        saveLastVerifiedBlockHeight(fromHeight + blockLimit / 2);
    }

    private void saveLastVerifiedBlockHeight(int height) {
        optionDAO.set(LAST_VERIFIED_BLOCK_KEY, String.valueOf(height));
    }

    private String failedTxToString(Map<Long, String> failedTxs) {
        return failedTxs.entrySet()
            .stream()
            .map(e -> Long.toUnsignedString(e.getKey()) + ":" + e.getValue())
            .collect(Collectors.joining(","));
    }

    private class Verifier {
        private final List<Peer> connectedPublicPeers;
        private final Map<Long, VerifiedTransaction> failedTxs;

        public Verifier(Map<Long, String> failedTxs) {
            this.failedTxs = failedTxs.entrySet()
                .stream()
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e->
                    new VerifiedTransaction(e.getKey(), new VerifiedError(e.getValue()))));
            this.connectedPublicPeers = Collections.synchronizedList(new ArrayList<>(peersService.getPublicPeers(PeerState.CONNECTED, true)));
        }

        /**
         * Verify all received failed transactions against the results received from peers
         * returning list of fully verified transactions
         * @return results of the transaction's verification including fully verified and partially verified transactions
         */
        public Result verify() {
            List<GetTransactionsRequest> requests = getTransactionsRequestDivided(new ArrayList<>(failedTxs.keySet()));
            log.debug("Created {} getTransactions requests for {} failed txs verification", requests.size(), failedTxs.size());
            List<Future<?>> verificationJobs = new ArrayList<>();
            for (GetTransactionsRequest request : requests) {
                Future<?> transactionVerificationJob = executor.submit(() -> verifyForRequest(request));
                verificationJobs.add(transactionVerificationJob);
            }
            for (Future<?> verificationJob : verificationJobs) {
                try {
                    verificationJob.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new AplCoreLogicException(e.toString(), e);
                }
            }
            return new Result(failedTxs);
        }

        private void verifyForRequest(GetTransactionsRequest request) {
            Set<Peer> alreadyUsedPeers = new HashSet<>();
            while (true) {
                Set<Long> notFullyVerifiedTxs = request.getTransactionIds()
                    .stream()
                    .filter(e -> !failedTxs.get(e).isVerified())
                    .collect(Collectors.toSet());
                if (notFullyVerifiedTxs.isEmpty()) {
                    log.info("Transaction's statuses are verified for request: {}", request);
                    return;
                }
                GetTransactionsRequest correctedRequest = request.clone();
                correctedRequest.setTransactionIds(notFullyVerifiedTxs);
                Optional<PeerGetTransactionsResponse> peerResponseOptional = sendToAnother(correctedRequest, alreadyUsedPeers);
                if (peerResponseOptional.isEmpty()) {
                    log.warn("Not enough peers to get failed transaction statuses, connected peers {}, already used peers [{}], request: {}, not fully verified txs: {}", connectedPublicPeers.size(),
                        alreadyUsedPeers.stream().map(Peer::getHostWithPort).collect(Collectors.joining(",")), correctedRequest, notFullyVerifiedTxs);
                    return;
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
                    VerifiedTransaction verifiedTx = failedTxs.get(Long.parseUnsignedLong(tx.getTransaction()));
                    if (verifiedTx == null) {
                        log.error("Possibly malicious {} peer detected at height {}: {} is not expected transaction from GetTransactions request: {}", peer.getHostWithPort(), blockchain.getHeight(), tx.getTransaction(), correctedRequest);
                        peer.blacklist("Peer returned not expected transaction: " + tx.getTransaction());
                        connectedPublicPeers.remove(peer);
                        break;
                    }
                    if (!verifiedTx.verify(tx.getErrorMessage())) {
                        log.warn("Blockchain inconsistency may occur. Transaction's {} validation & execution results into an error message '{}', " +
                            "which does not match to {} peer's result '{}'", tx.getTransaction(), verifiedTx.getError(), tx.getErrorMessage(), peer.getHostWithPort());
                    }
                }
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

        private List<GetTransactionsRequest> getTransactionsRequestDivided(List<Long> ids) {
            List<GetTransactionsRequest> requests = new ArrayList<>();
            for (int i = 0; i < ids.size(); i += failedTransactionsPerRequest) {
                HashSet<Long> requestTransactionIds = new HashSet<>(ids.subList(i, Math.min(i + failedTransactionsPerRequest, ids.size())));
                GetTransactionsRequest request = new GetTransactionsRequest(requestTransactionIds, config.getChainId());
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


    @Data
    private class VerifiedError {
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

        public synchronized boolean isVerified() {
            return count >= config.getConfirmations();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class PeerGetTransactionsResponse {
        private Peer peer;
        private GetTransactionsResponse response;
    }
}
