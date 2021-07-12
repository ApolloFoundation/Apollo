/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetCumulativeDifficultyRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetMilestoneBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetCumulativeDifficultyResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetMilestoneBlockIdsResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetNextBlockIdsResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.app.GetNextBlocksTask;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreLogicException;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.BlockchainProcessorState;
import com.apollocurrency.aplwallet.apl.core.model.PeerBlock;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetCumulativeDifficultyResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetMilestoneBlockIdsResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetNextBlockIdsResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetNextBlocksResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
public class GetMoreBlocksJob implements Runnable {

    private final BlockchainProcessor blockchainProcessor;
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final PeersService peersService;
    private final GlobalSync globalSync;
    private final TimeService timeService;
    private final PrunableRestorationService prunableRestorationService;
    private final ExecutorService networkService;
    private final TransactionProcessor transactionProcessor;
    private final Integer defaultNumberOfForkConfirmations;
    private final int numberOfFailedTransactionConfirmations;
    private final BlockchainProcessorState blockchainProcessorState;
    private final GetCumulativeDifficultyRequest getCumulativeDifficultyRequest;
    private final GetNextBlocksResponseParser getNextBlocksResponseParser;
    private final BlockSerializer blockSerializer;
    private final GetTransactionsResponseParser getTransactionsResponseParser;
    @Setter
    private volatile int failedTransactionsPerRequest = 100;

    private boolean peerHasMore;
    private List<Peer> connectedPublicPeers;
    private List<Long> chainBlockIds;
    private long totalTime = 1;
    private int totalBlocks;


    public GetMoreBlocksJob(BlockchainProcessor blockchainProcessor, BlockchainProcessorState blockchainProcessorState,
                            BlockchainConfig blockchainConfig, Blockchain blockchain, PeersService peersService,
                            GlobalSync globalSync, TimeService timeService, PrunableRestorationService prunableRestorationService,
                            ExecutorService networkService, PropertiesHolder propertiesHolder,
                            TransactionProcessor transactionProcessor,
                            GetNextBlocksResponseParser getNextBlocksResponseParser,
                            BlockSerializer blockSerializer, GetTransactionsResponseParser getTransactionsResponseParser) {
        this.blockchainProcessor = blockchainProcessor;
        this.blockchainProcessorState = blockchainProcessorState;

        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.peersService = peersService;
        this.globalSync = globalSync;
        this.timeService = timeService;
        this.prunableRestorationService = prunableRestorationService;
        this.networkService = networkService;
        this.transactionProcessor = transactionProcessor;
        this.defaultNumberOfForkConfirmations = propertiesHolder.getIntProperty("apl.numberOfForkConfirmations");
        this.getNextBlocksResponseParser = getNextBlocksResponseParser;
        this.getCumulativeDifficultyRequest = new GetCumulativeDifficultyRequest(blockchainConfig.getChain().getChainId());
        this.blockSerializer = blockSerializer;
        this.getTransactionsResponseParser = getTransactionsResponseParser;
        this.numberOfFailedTransactionConfirmations = propertiesHolder.getIntProperty("apl.numberOfFailedTransactionConfirmations", 3);
    }

    @Override
    public void run() {
        try {
            //
            // Download blocks until we are up-to-date
            //
            while (true) {
                if (!blockchainProcessorState.isGetMoreBlocks()) {
                    return;
                }
                int chainHeight = blockchain.getHeight();
                downloadPeer();
                log.trace("Is finished BCH download ? ({}), h1={}, h2={}, isDownloading={}",
                    blockchain.getHeight() == chainHeight, chainHeight,
                    blockchain.getHeight(), blockchainProcessorState.isDownloading());
                if (blockchain.getHeight() == chainHeight) {
                    if (blockchainProcessorState.isDownloading()) {
                        log.info("Finished blockchain download");
                        blockchainProcessorState.setDownloading(false);
                    }
                    break;
                }
            }
            //
            // Restore prunable data
            //
            int now = timeService.getEpochTime();
            if (!blockchainProcessorState.isRestoring()
                && !prunableRestorationService.getPrunableTransactions().isEmpty()
                && now - blockchainProcessorState.getLastRestoreTime() > 60 * 60) {
                blockchainProcessorState.setRestoring(true);
                blockchainProcessorState.setLastRestoreTime(now);
                networkService.submit(new RestorePrunableDataTask(
                        blockchainProcessorState, peersService, prunableRestorationService,
                        blockchainConfig, transactionProcessor
                    )
                );
            }
        } catch (Throwable t) {
            log.error("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    }

    private void downloadPeer() {
        try {
            long startTime = System.currentTimeMillis();
            int numberOfForkConfirmations = blockchain.getHeight() > Constants.LAST_CHECKSUM_BLOCK - Constants.MAX_AUTO_ROLLBACK ?
                defaultNumberOfForkConfirmations : Math.min(1, defaultNumberOfForkConfirmations);
            connectedPublicPeers = peersService.getPublicPeers(PeerState.CONNECTED, true);
            if (connectedPublicPeers.size() <= numberOfForkConfirmations) {
                log.trace("downloadPeer connected = {} <= numberOfForkConfirmations = {}",
                    connectedPublicPeers.size(), numberOfForkConfirmations);
                return;
            }
            peerHasMore = true;
            final Peer peer = peersService.getWeightedPeer(connectedPublicPeers);
            if (peer == null) {
                log.debug("Can not find weighted peer");
                return;
            }

            GetCumulativeDifficultyResponse response = peer.send(getCumulativeDifficultyRequest, new GetCumulativeDifficultyResponseParser());
            if (response == null) {
                log.debug("Null response wile getCumulativeDifficultyRequest from peer {}", peer.getHostWithPort());
                return;
            }
            Block lastBlock = blockchain.getLastBlock();
            BigInteger curCumulativeDifficulty = lastBlock.getCumulativeDifficulty();
            BigInteger peerCumulativeDifficulty = response.getCumulativeDifficulty();
            if (peerCumulativeDifficulty == null) {
                return;
            }
            if (peerCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
                return;
            }
            if (response.getBlockchainHeight() != null) {
                blockchainProcessorState.setLastBlockchainFeeder(peer);
                blockchainProcessorState.setLastBlockchainFeederHeight(response.getBlockchainHeight().intValue());
            }
            if (peerCumulativeDifficulty.equals(curCumulativeDifficulty)) {
                return;
            }

            long commonMilestoneBlockId = blockchain.getShardInitialBlock().getId();

            if (blockchain.getHeight() > 0) {
                commonMilestoneBlockId = getCommonMilestoneBlockId(peer);
            }
            if (commonMilestoneBlockId == 0 || !peerHasMore) {
                return;
            }

            chainBlockIds = getBlockIdsAfterCommon(peer, commonMilestoneBlockId, false);
            if (chainBlockIds.size() < 2 || !peerHasMore) {
                if (commonMilestoneBlockId == blockchain.getShardInitialBlock().getId()) {
                    log.info("Cannot load blocks after genesis block {} from peer {}, perhaps using different Genesis block",
                        commonMilestoneBlockId, peer.getAnnouncedAddress());
                }
                return;
            }

            final long commonBlockId = chainBlockIds.get(0);
            final Block commonBlock = blockchain.getBlock(commonBlockId);
            if (commonBlock == null || blockchain.getHeight() - commonBlock.getHeight() >= Constants.MAX_AUTO_ROLLBACK) {
                if (commonBlock != null) {
                    log.debug("Peer {} advertised chain with better difficulty, but the last common block is at height {}, peer info - {}", peer.getAnnouncedAddress(), commonBlock.getHeight(), peer);
                }
                return;
            }

            if (!blockchainProcessorState.isDownloading() && blockchainProcessorState.getLastBlockchainFeederHeight() - commonBlock.getHeight() > 10) {
                log.info("Blockchain download in progress, set blockchain state isDownloading=true.");
                blockchainProcessorState.setDownloading(true);
            }
// TODO Maybe better to find another sync solution
            globalSync.updateLock();
            try {
                if (peerCumulativeDifficulty.compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                    return;
                }
                long lastBlockId = blockchain.getLastBlock().getId();
                downloadBlockchain(peer, commonBlock, commonBlock.getHeight());
                verifyFailedTransactions(commonBlock);
                if (blockchain.getHeight() - commonBlock.getHeight() <= 10) {
                    return;
                }

                int confirmations = 0;
                for (Peer otherPeer : connectedPublicPeers) {
                    if (confirmations >= numberOfForkConfirmations) {
                        break;
                    }
                    if (peer.getHost().equals(otherPeer.getHost())) {
                        continue;
                    }
                    chainBlockIds = getBlockIdsAfterCommon(otherPeer, commonBlockId, true);
                    if (chainBlockIds.isEmpty()) {
                        continue;
                    }
                    long otherPeerCommonBlockId = chainBlockIds.get(0);
                    if (otherPeerCommonBlockId == blockchain.getLastBlock().getId()) {
                        confirmations++;
                        continue;
                    }
                    Block otherPeerCommonBlock = blockchain.getBlock(otherPeerCommonBlockId);
                    if (blockchain.getHeight() - otherPeerCommonBlock.getHeight() >= Constants.MAX_AUTO_ROLLBACK) {
                        continue;
                    }
                    BigInteger otherPeerCumulativeDifficulty = response.getCumulativeDifficulty();
                    GetCumulativeDifficultyResponse otherPeerResponse = peer.send(getCumulativeDifficultyRequest, new GetCumulativeDifficultyResponseParser());
                    if (otherPeerResponse == null || otherPeerCumulativeDifficulty == null) {
                        continue;
                    }
                    if (otherPeerCumulativeDifficulty.compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                        continue;
                    }
                    log.debug("Found a peer with better difficulty: {}", otherPeer.getHostWithPort());
                    downloadBlockchain(otherPeer, otherPeerCommonBlock, commonBlock.getHeight());
                }
                log.debug("Got " + confirmations + " confirmations");

                if (blockchain.getLastBlock().getId() != lastBlockId) {
                    long time = System.currentTimeMillis() - startTime;
                    totalTime += time;
                    int numBlocks = blockchain.getHeight() - commonBlock.getHeight();
                    totalBlocks += numBlocks;
                    log.info("Downloaded " + numBlocks + " blocks in "
                        + time / 1000 + " s, " + (totalBlocks * 1000) / totalTime + " per s, "
                        + totalTime * (blockchainProcessorState.getLastBlockchainFeederHeight() - blockchain.getHeight()) / ((long) totalBlocks * 1000 * 60) + " min left");
                } else {
                    log.debug("Did not accept peer's blocks, back to our own fork");
                }
            } finally {
                globalSync.updateUnlock();
                blockchainProcessorState.setDownloading(false);
                log.debug("Set blockchain state isDownloading=false.");
            }
        } catch (Exception e) {
            log.info("Error in blockchain download thread", e);
        }
    }

    /**
     * Get first mutual block which peer has in blockchain and we have in blockchain
     *
     * @param peer node which will supply us with possible mutual blocks
     * @return id of the first mutual block
     */
    private long getCommonMilestoneBlockId(Peer peer) {

        String lastMilestoneBlockId = null;

        while (true) {
            GetMilestoneBlockIdsRequest milestoneBlockIdsRequest = new GetMilestoneBlockIdsRequest(blockchainConfig.getChain().getChainId());
            if (lastMilestoneBlockId == null) {
                milestoneBlockIdsRequest.setLastBlockId(blockchain.getLastBlock().getStringId());
            } else {
                milestoneBlockIdsRequest.setLastMilestoneBlockId(lastMilestoneBlockId);
            }

            GetMilestoneBlockIdsResponse response;
            try {
                response = peer.send(milestoneBlockIdsRequest, new GetMilestoneBlockIdsResponseParser());
            } catch (PeerNotConnectedException ex) {
                response = null;
            }
            if (response == null) {
                return 0;
            }
            List<String> milestoneBlockIds = response.getMilestoneBlockIds();
            if (milestoneBlockIds == null) {
                return 0;
            }
            if (milestoneBlockIds.isEmpty()) {
                return blockchain.getShardInitialBlock().getId();
            }
            // prevent overloading with blockIds
            if (milestoneBlockIds.size() > 20) {
                log.debug("Obsolete or rogue peer " + peer.getHost() + " sends too many milestoneBlockIds, blacklisting");
                peer.blacklist("Too many milestoneBlockIds");
                return 0;
            }
            if (Boolean.TRUE.equals(response.isLast())) {
                peerHasMore = false;
            }
            for (String milestoneBlockId : milestoneBlockIds) {
                long blockId = Convert.parseUnsignedLong(milestoneBlockId);
                if (blockchain.hasBlock(blockId)) {
                    if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                        peerHasMore = false;
                    }
                    return blockId;
                }
                lastMilestoneBlockId = milestoneBlockId;
            }
        }
    }

    private void verifyFailedTransactions(Block fromBlock) {
        List<Block> downloadedBlocks = blockchain.getBlocksAfter(fromBlock.getHeight(), Integer.MAX_VALUE);
        Map<Long, VerifiedError> failedTxs = downloadedBlocks.stream()
            .flatMap(e -> e.getTransactions().stream())
            .filter(e -> e.getErrorMessage().isPresent())
            .collect(Collectors.toConcurrentMap(Transaction::getId, e -> new VerifiedError(e.getErrorMessage().get())));
        if (failedTxs.isEmpty()) {
            log.info("No failed transactions downloaded to validate starting from height {} to current height {} ", fromBlock.getHeight(), blockchain.getHeight());
            return;
        }
        log.info("Selected {} failed transactions to verify failure cause, [{}]", failedTxs.size(), failedTxToString(failedTxs));
        List<GetTransactionsRequest> requests = getTransactionsRequestDivided(new ArrayList<>(failedTxs.keySet()));
        List<Future<?>> verificationJobs = new ArrayList<>();
        for (GetTransactionsRequest request : requests) {
            Future<?> transactionVerificationJob = networkService.submit(() -> verifyForRequest(failedTxs, request));
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

    private String failedTxToString(Map<Long, VerifiedError> failedTxs) {
        return failedTxs.entrySet()
            .stream()
            .map(e -> Long.toUnsignedString(e.getKey()) + ":" + e.getValue().getError())
            .collect(Collectors.joining(","));
    }

    private void verifyForRequest(Map<Long, VerifiedError> failedTxs, GetTransactionsRequest request) {
        while (true) {
            Set<Long> notFullyConfirmedTxs = request.getTransactionIds()
                .stream()
                .filter(e -> failedTxs.get(e).count < numberOfFailedTransactionConfirmations)
                .collect(Collectors.toSet());
            if (notFullyConfirmedTxs.isEmpty()) {
                log.info("Failed transactions are verified for request: {}", request);
                return;
            }
            GetTransactionsRequest correctedRequest = request.clone();
            correctedRequest.setTransactionIds(notFullyConfirmedTxs);
            Set<Peer> alreadyUsedPeers = new HashSet<>();
            Optional<PeerGetTransactionsResponse> peerResponseOptional = sendToAnother(correctedRequest, alreadyUsedPeers);
            if (peerResponseOptional.isEmpty()) {
                log.warn("Not enough peers to get failed transaction statuses, connected peers {}, already used peers [{}], request: {}", connectedPublicPeers.size(), alreadyUsedPeers.stream().map(Peer::getHostWithPort).collect(Collectors.joining(",")), correctedRequest);
                return;
            }
            GetTransactionsResponse response = peerResponseOptional.get().getResponse();
            Peer peer = peerResponseOptional.get().getPeer();
            alreadyUsedPeers.add(peer);
            Set<Long> requestedIds = correctedRequest.getTransactionIds();
            if (response.getTransactions().size() > requestedIds.size()) {
                log.warn("Possibly malicious {} peer detected: received too many transactions {} instead of {} ", peer.getHostWithPort(), response.getTransactions().size(), requestedIds.size());
                peer.blacklist("Too many transactions supplied for failed transactions validation");
                continue;
            }
            if (response.getTransactions().size() < requestedIds.size()) {
                Set<Long> receivedIds = response.getTransactions()
                    .stream()
                    .map(UnconfirmedTransactionDTO::getTransaction)
                    .map(Long::parseUnsignedLong)
                    .collect(Collectors.toSet());
                receivedIds.removeAll(requestedIds);
                log.debug("Peer {} is missing {} transactions for validation: {}", peer.getHostWithPort(), receivedIds.size(), receivedIds);
            }
            for (TransactionDTO tx : response.getTransactions()) {
                VerifiedError verifiedError = failedTxs.get(Long.parseUnsignedLong(tx.getTransaction()));
                if (verifiedError == null) {
                    log.warn("Possibly malicious {} peer detected at height {}: {} is not expected transaction from GetTransactions request: {}", peer.getHostWithPort(), blockchain.getHeight(), tx.getTransaction(), correctedRequest);
                    peer.blacklist("Peer returned not expected transaction: " + tx.getTransaction());
                    break;
                }
                if (!verifiedError.verify(tx.getErrorMessage())) {
                    log.warn("Blockchain inconsistency may occur. Transaction {} validation & execution results into error message '{}', " +
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
                GetTransactionsResponse response = peer.send(request, getTransactionsResponseParser);
                if (response == null) {
                    excludedPeersCopy.add(peer);
                    continue;
                }
                return Optional.of(new PeerGetTransactionsResponse(peer, response));
            } catch (PeerNotConnectedException e) {
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



    private List<Long> getBlockIdsAfterCommon(final Peer peer, final long startBlockId, final boolean countFromStart) {
        long matchId = startBlockId;
        List<Long> blockList = new ArrayList<>(Constants.MAX_AUTO_ROLLBACK);
        boolean matched = false;
        int limit = countFromStart ? Constants.MAX_AUTO_ROLLBACK : Constants.MAX_AUTO_ROLLBACK * 2;
        while (true) {
            GetNextBlockIdsRequest request = new GetNextBlockIdsRequest(
                Long.toUnsignedString(matchId),
                limit,
                blockchainConfig.getChain().getChainId()
            );

            GetNextBlockIdsResponse response = null;
            try {
                response = peer.send(request, new GetNextBlockIdsResponseParser());
            } catch (PeerNotConnectedException ex) {
                log.warn(ex.getMessage());
            }

            if (response == null) {
                log.debug("null response from peer {} while getNextBlockIds", peer.getHostWithPort());
                return Collections.emptyList();
            }
            List<String> nextBlockIds = response.getNextBlockIds();
            if (nextBlockIds == null || nextBlockIds.isEmpty()) {
                break;
            }
            // prevent overloading with blockIds
            if (nextBlockIds.size() > limit) {
                log.debug("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlockIds, blacklisting");
                peer.blacklist("Too many nextBlockIds");
                return Collections.emptyList();
            }
            boolean matching = true;
            int count = 0;
            for (String nextBlockId : nextBlockIds) {
                long blockId = Convert.parseUnsignedLong(nextBlockId);
                if (matching) {
                    if (blockchain.hasBlock(blockId)) {
                        matchId = blockId;
                        matched = true;
                    } else {
                        blockList.add(matchId);
                        blockList.add(blockId);
                        matching = false;
                    }
                } else {
                    blockList.add(blockId);
                    if (blockList.size() >= Constants.MAX_AUTO_ROLLBACK) {
                        break;
                    }
                }
                if (countFromStart && ++count >= Constants.MAX_AUTO_ROLLBACK) {
                    break;
                }
            }
            if (!matching || countFromStart) {
                break;
            }
        }
        if (blockList.isEmpty() && matched) {
            blockList.add(matchId);
        }
        return blockList;
    }

    /**
     * Download the block chain
     *
     * @param feederPeer  Peer supplying the blocks list
     * @param commonBlock Common block
     * @throws InterruptedException Download interrupted
     */
    private void downloadBlockchain(final Peer feederPeer, final Block commonBlock, final int startHeight) throws InterruptedException {
        log.debug("Downloading blockchain from: {} at height: {}", feederPeer.getHostWithPort(), startHeight);
        Map<Long, PeerBlock> blockMap = new HashMap<>();
        //
        // Break the download into multiple segments.  The first block in each segment
        // is the common block for that segment.
        //
        List<GetNextBlocksTask> getList = new ArrayList<>();
        int segSize = Constants.MAX_AUTO_ROLLBACK / 20;
        int stop = chainBlockIds.size() - 1;
        for (int start = 0; start < stop; start += segSize) {
            getList.add(new GetNextBlocksTask(chainBlockIds, start,
                Math.min(start + segSize, stop), startHeight, blockchainConfig, getNextBlocksResponseParser));
        }
        int nextPeerIndex = ThreadLocalRandom.current().nextInt(connectedPublicPeers.size());
        long maxResponseTime = 0;
        Peer slowestPeer = null;
        //
        // Issue the getNextBlocks requests and get the results.  We will repeat
        // a request if the peer didn't respond or returned a partial block list.
        // The download will be aborted if we are unable to get a segment after
        // retrying with different peers.
        //
        download:
        while (!getList.isEmpty()) {
            //
            // Submit threads to issue 'getNextBlocks' requests.  The first segment
            // will always be sent to the feeder peer.  Subsequent segments will
            // be sent to the feeder peer if we failed trying to download the blocks
            // from another peer.  We will stop the download and process any pending
            // blocks if we are unable to download a segment from the feeder peer.
            //
            for (GetNextBlocksTask nextBlocks : getList) {
                Peer peer;
                if (nextBlocks.getRequestCount() > 1) { // request sent two times
                    break download;
                }
                if (nextBlocks.getStart() == 0 || nextBlocks.getRequestCount() != 0) {
                    peer = feederPeer; // first segment or already sent request
                } else {
                    if (nextPeerIndex >= connectedPublicPeers.size()) { // round robin from the randomly selected position
                        nextPeerIndex = 0;
                    }
                    peer = connectedPublicPeers.get(nextPeerIndex++);
                }
                if (nextBlocks.getPeer() == peer) { // feederPeer failed to return all its blocks or there is only one public peer
                    break download;
                }
                nextBlocks.setPeer(peer);
                Future<List<Block>> future = networkService.submit(nextBlocks);
                nextBlocks.setFuture(future);
            }
            //
            // Get the results.  A peer is on a different fork if a returned
            // block is not in the block identifier list.
            //
            Iterator<GetNextBlocksTask> it = getList.iterator();
            while (it.hasNext()) {
                GetNextBlocksTask nextBlocks = it.next();
                List<Block> blockList;
                try {
                    blockList = nextBlocks.getFuture().get();
                } catch (ExecutionException exc) {
                    throw new RuntimeException(exc.getMessage(), exc);
                }
                if (blockList == null) {
// most certainly this is wrong. We should not kill peer if it does not have blocks higher then we
//                        nextBlocks.getPeer().deactivate();
                    continue;
                }
                Peer peer = nextBlocks.getPeer();
                int index = nextBlocks.getStart() + 1;
                for (Block block : blockList) {
                    if (block.getId() != chainBlockIds.get(index)) {
                        break;
                    }
                    blockMap.put(block.getId(), new PeerBlock(peer, block));
                    index++;
                }
                if (index > nextBlocks.getStop()) {
                    it.remove();
                } else {
                    nextBlocks.setStart(index - 1); // download the rest of blocks in the next iteration
                }
                if (nextBlocks.getResponseTime() > maxResponseTime) {
                    maxResponseTime = nextBlocks.getResponseTime();
                    slowestPeer = nextBlocks.getPeer();
                }
            }

        }
        if (slowestPeer != null && connectedPublicPeers.size() >= PeersService.maxNumberOfConnectedPublicPeers && chainBlockIds.size() > Constants.MAX_AUTO_ROLLBACK / 2) {
            log.debug("Slowest peer {} took {} ms, disconnecting", slowestPeer.getHost(), maxResponseTime);
            slowestPeer.deactivate("This peer is slowest");
        }
        //
        // Add the new blocks to the blockchain.  We will stop if we encounter
        // a missing block (this will happen if an invalid block is encountered
        // when downloading the blocks)
        //
        globalSync.writeLock();
        try {
            List<Block> forkBlocks = new ArrayList<>();
            for (int index = 1; index < chainBlockIds.size() && blockchain.getHeight() - startHeight < Constants.MAX_AUTO_ROLLBACK; index++) {
                PeerBlock peerBlock = blockMap.get(chainBlockIds.get(index));
                if (peerBlock == null) {
                    break;
                }
                Block block = peerBlock.getBlock();
                if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                    try {
                        blockchainProcessor.pushBlock(block);
                    } catch (BlockchainProcessor.BlockNotAcceptedException e) {
                        peerBlock.getPeer().blacklist(e);
                    }
                } else {
                    forkBlocks.add(block);
                }
            }
            //
            // Process a fork
            //
            int myForkSize = blockchain.getHeight() - startHeight;
            if (!forkBlocks.isEmpty() && myForkSize < Constants.MAX_AUTO_ROLLBACK) {
                log.debug("Will process a fork of {} blocks, mine is {}, feed peer addr: {}", forkBlocks.size(), myForkSize, feederPeer.getHost());
                processFork(feederPeer, forkBlocks, commonBlock);
            }
        } finally {
            globalSync.writeUnlock();
        }

    }

    private void processFork(final Peer peer, final List<Block> forkBlocks, final Block commonBlock) {

        BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

        List<Block> myPoppedOffBlocks = blockchainProcessor.popOffToCommonBlock(commonBlock);

        int pushedForkBlocks = 0;
        if (blockchain.getLastBlock().getId() == commonBlock.getId()) {
            for (Block block : forkBlocks) {
                if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                    try {
                        blockchainProcessor.pushBlock(block);
                        pushedForkBlocks += 1;
                    } catch (BlockchainProcessor.BlockNotAcceptedException e) {
                        peer.blacklist(e);
                        break;
                    }
                }
            }
        }

        if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
            log.debug("Pop off caused by peer {}, blacklisting", peer.getHostWithPort());
            peer.blacklist("Pop off");
            List<Block> peerPoppedOffBlocks = blockchainProcessor.popOffToCommonBlock(commonBlock);
            pushedForkBlocks = 0;
            for (Block block : peerPoppedOffBlocks) {
                transactionProcessor.processLater(block.getTransactions());
            }
        }

        if (pushedForkBlocks == 0) {
            log.debug("Didn't accept any blocks, pushing back my previous blocks");
            for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
                Block block = myPoppedOffBlocks.remove(i);
                try {
                    blockchainProcessor.pushBlock(block);
                } catch (BlockchainProcessor.BlockNotAcceptedException e) {
                    log.error("Popped off block no longer acceptable: " + blockSerializer.getJSONObject(block).toJSONString(), e);
                    break;
                }
            }
        } else {
            log.debug("Switched to peer's fork, peer addr: {}", peer.getHostWithPort());
            for (Block block : myPoppedOffBlocks) {
                transactionProcessor.processLater(block.getTransactions());
            }
        }
    }
}
