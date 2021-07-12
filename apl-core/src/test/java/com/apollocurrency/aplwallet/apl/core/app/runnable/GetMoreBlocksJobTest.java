/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetCumulativeDifficultyRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetMilestoneBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlocksRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetCumulativeDifficultyResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetMilestoneBlockIdsResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetNextBlockIdsResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.BlockchainProcessorState;
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
import com.apollocurrency.aplwallet.apl.core.peer.respons.GetNextBlocksResponse;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockSerializer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableRestorationService;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetMoreBlocksJobTest {
    static AtomicInteger counter = new AtomicInteger(0);
    private final UUID chainId = UUID.randomUUID();
    @Mock
    BlockchainProcessor blockchainProcessor;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Blockchain blockchain;
    @Mock
    PeersService peersService;
    @Mock
    GlobalSync globalSync;
    @Mock
    TimeService timeService;
    @Mock
    PrunableRestorationService prunableRestorationService;

    ExecutorService networkService = Executors.newCachedThreadPool();
    @Mock
    TransactionProcessor transactionProcessor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    BlockSerializer blockSerializer;
    @Mock
    GetNextBlocksResponseParser getNextBlocksResponseParser;
    @Mock
    GetTransactionsResponseParser getTransactionsResponseParser;
    @Mock
    Chain chain;
    @Mock
    Peer peer1;
    @Mock
    Peer peer2;
    @Mock
    Peer peer3;
    @Mock
    Block currentLastBlock;
    @Mock
    Block shardInitialBlock;
    @Mock
    Block commonBlock;

    List<Transaction> failedTransactions = Collections.synchronizedList(new ArrayList<>());

    GetMoreBlocksJob job;
    BlockchainProcessorState state = new BlockchainProcessorState();

    @BeforeEach
    void setUp() {
        doReturn(2).when(propertiesHolder).getIntProperty("apl.numberOfFailedTransactionConfirmations", 3);
        doReturn(0).when(propertiesHolder).getIntProperty("apl.numberOfForkConfirmations");
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(chainId).when(chain).getChainId();
        job = new GetMoreBlocksJob(blockchainProcessor, state, blockchainConfig, blockchain, peersService
            , globalSync, timeService, prunableRestorationService, networkService, propertiesHolder, transactionProcessor, getNextBlocksResponseParser, blockSerializer, getTransactionsResponseParser);
        job.setFailedTransactionsPerRequest(2);
    }

    @Test
    void downloadBlockchainProcessingPeersFork() throws PeerNotConnectedException, BlockchainProcessor.BlockNotAcceptedException {
        doReturn(2000).when(blockchain).getHeight();
        List<Peer> allPeers = List.of(peer1, peer2, peer3);
        doReturn(allPeers).when(peersService).getPublicPeers(PeerState.CONNECTED, true);
        doReturn(peer1).when(peersService).getWeightedPeer(allPeers);
        GetCumulativeDifficultyResponse cumulativeDiffResponse = new GetCumulativeDifficultyResponse(30_000, BigInteger.valueOf(9898));
        doReturn(cumulativeDiffResponse).when(peer1).send(new GetCumulativeDifficultyRequest(chainId), new GetCumulativeDifficultyResponseParser());
        doReturn(currentLastBlock).when(blockchain).getLastBlock();
        doReturn(BigInteger.valueOf(7766)).when(currentLastBlock).getCumulativeDifficulty();
        doReturn(shardInitialBlock).when(blockchain).getShardInitialBlock();
        doReturn(99L).when(shardInitialBlock).getId();
        doReturn("200").when(currentLastBlock).getStringId();
        doReturn(200L).when(currentLastBlock).getId();
        doReturn(new GetMilestoneBlockIdsResponse(List.of("99", "88", "77"), false)).when(peer1).send(new GetMilestoneBlockIdsRequest(chainId, "200", null), new GetMilestoneBlockIdsResponseParser());
        List<String> returnedBlockIds = IntStream.range(10_000, 11439).boxed().map(String::valueOf).collect(Collectors.toList());
        doReturn(new GetNextBlockIdsResponse(returnedBlockIds)).when(peer1).send(new GetNextBlockIdsRequest("77", 1440, chainId), new GetNextBlockIdsResponseParser());
        doAnswer(invocation -> {
            Long blockId = invocation.getArgument(0);
            return blockId == 77 || returnedBlockIds.subList(0, 1000).contains(String.valueOf(blockId)); // getMilestoneBlockId + getNextBlockIds
        }).when(blockchain).hasBlock(anyLong());
        doReturn(commonBlock).when(blockchain).getBlock(10_999L);
        doReturn(1999).when(commonBlock).getHeight();
        doReturn(10_999L).when(commonBlock).getId();
        doReturn("peer1IP:1111").when(peer1).getHostWithPort();
        doAnswer(invocation -> returnAllRequestedPeerBlocks(returnedBlockIds, invocation)).when(peer1).send(any(GetNextBlocksRequest.class), any(GetNextBlocksResponseParser.class));
        doAnswer(invocation -> returnAllRequestedPeerBlocks(returnedBlockIds, invocation)).when(peer2).send(any(GetNextBlocksRequest.class), any(GetNextBlocksResponseParser.class));
        doAnswer(invocation -> returnRequestedPeerBlocksPartially(returnedBlockIds, invocation)).when(peer3).send(any(GetNextBlocksRequest.class), any(GetNextBlocksResponseParser.class));
        List<Block> pushedBlocks = new CopyOnWriteArrayList<>();
        mockPushBlock(pushedBlocks);
        doAnswer(invocation-> {
            doReturn(commonBlock).when(blockchain).getLastBlock();
            return List.of(currentLastBlock);
        }).when(blockchainProcessor).popOffToCommonBlock(commonBlock);
        // processing failed transactions mocks
        doAnswer(invocation -> {
            mockBlockTxs(pushedBlocks);
            return pushedBlocks;
        }).when(blockchain).getBlocksAfter(1999, Integer.MAX_VALUE);
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer1).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer2).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));

        job.run();

        assertSame(peer1, state.getLastBlockchainFeeder());
        assertEquals(30_000, state.getLastBlockchainFeederHeight());
        assertEquals(2438, blockchain.getLastBlock().getHeight());
        assertEquals(11438, blockchain.getLastBlock().getId());
        verify(globalSync).updateLock();
        verify(globalSync).updateUnlock();
        verify(globalSync).writeLock();
        verify(globalSync).writeUnlock();
    }

    private GetTransactionsResponse returnAllRequestedCorrectTxs(InvocationOnMock invocation) {
        GetTransactionsRequest request = invocation.getArgument(0);
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        request.getTransactionIds().forEach(e-> {
            Optional<Transaction> foundTx = failedTransactions.stream().filter(tx -> tx.getId() == e).findFirst();
            if (foundTx.isEmpty()) {
                throw new IllegalStateException("Unknown transaction requested from peer: " + e);
            }
            Transaction failedTx = foundTx.get();
            String error = failedTx.getErrorMessage().orElseThrow(() -> new IllegalStateException("Failed tx: " + failedTx.getId() + " should have error message"));
            TransactionDTO txDto = mock(TransactionDTO.class);
            doReturn(error).when(txDto).getErrorMessage();
            doReturn(failedTx.getStringId()).when(txDto).getTransaction();
            txsToReturn.add(txDto);
        });
        return new GetTransactionsResponse(txsToReturn);
    }

    private GetTransactionsResponse returnErrorTxsResponse(InvocationOnMock invocation) throws PeerNotConnectedException {
        Random random = new Random();
        if (random.nextInt(10) > 6) { // 30% change
            return null;
        }
        if (random.nextInt(10) > 7) { // 20% chance
            throw new PeerNotConnectedException("Peer is not connected test error");
        }
        GetTransactionsRequest request = invocation.getArgument(0);
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        request.getTransactionIds().forEach(e-> {
            Optional<Transaction> foundTx = failedTransactions.stream().filter(tx -> tx.getId() == e).findFirst();
            if (foundTx.isEmpty()) {
                throw new IllegalStateException("Unknown transaction requested from peer: " + e);
            }
            Transaction failedTx = foundTx.get();
            String error = failedTx.getErrorMessage().orElseThrow(() -> new IllegalStateException("Failed tx: " + failedTx.getId() + " should have error message"));
            TransactionDTO txDto = mock(TransactionDTO.class);
            doReturn(error).when(txDto).getErrorMessage();
            doReturn(failedTx.getStringId()).when(txDto).getTransaction();
            txsToReturn.add(txDto);
        });
        return new GetTransactionsResponse(txsToReturn);
    }

    private void mockPushBlock(List<Block> pushedBlocks) throws BlockchainProcessor.BlockNotAcceptedException {
        doAnswer(invocation -> {
            int height = blockchain.getLastBlock().getHeight();
            Block block = invocation.getArgument(0);
            doReturn(block).when(blockchain).getLastBlock();
            doReturn(height + 1).when(block).getHeight();
            pushedBlocks.add(block);
            return null;

        }).when(blockchainProcessor).pushBlock(any(Block.class));
    }

    private void mockBlockTxs(List<Block> pushedBlocks) {
        for (int i = 0; i < pushedBlocks.size(); i++) {
            Block block = pushedBlocks.get(i);
            int txNumber = new Random().nextInt(15);
            if (i % 4 == 0) {
                doReturn(generateMixed(block.getId(), txNumber)).when(block).getTransactions();
            } else if (i % 3 == 0) {
                doReturn(generateFailedTransactions(block.getId(), txNumber)).when(block).getTransactions();
            } else if (i % 2 == 0) {
                doReturn(generateOkTransactions(txNumber)).when(block).getTransactions();
            } else {
                doReturn(List.of()).when(block).getTransactions();
            }
        }
    }

    private Object returnAllRequestedPeerBlocks(List<String> returnedBlockIds, InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        assertTrue(returnedBlockIds.containsAll(request.getBlockIds()), "returned block ids list should contain all the requested block ids from peer");
        List<Block> respondedBlocks = new ArrayList<>();
        for (int i = 0; i < request.getBlockIds().size(); i++) {
            Block block = mockIdBlock(request.getBlockIds().get(i), i == 0 ? request.getBlockId() : request.getBlockIds().get(i - 1));
            respondedBlocks.add(block);
        }
        return new GetNextBlocksResponse(respondedBlocks);
    }

    private Object returnRequestedPeerBlocksPartially(List<String> returnedBlockIds, InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        assertTrue(returnedBlockIds.containsAll(request.getBlockIds()), "returned block ids list should contain all the requested block ids from peer");
        List<Block> respondedBlocks = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < request.getBlockIds().subList(0, random.nextInt(request.getBlockIds().size() + 1)).size(); i++) {
            respondedBlocks.add(mockIdBlock(request.getBlockIds().get(i), i == 0 ? request.getBlockId() : request.getBlockIds().get(i - 1)));
        }
        return new GetNextBlocksResponse(respondedBlocks);
    }

    private Block mockIdBlock(String id, String prevId) {
        Block block = mock(Block.class);
        lenient().doReturn(id).when(block).getStringId();
        lenient().doReturn(Long.parseUnsignedLong(id)).when(block).getId();
        doReturn(Long.parseUnsignedLong(prevId)).when(block).getPreviousBlockId();
        doReturn(BigInteger.valueOf(10_000)).when(block).getCumulativeDifficulty();
        return block;
    }

    private List<Transaction> generateOkTransactions(int number) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            transactions.add(generateOkTx());
        }
        return transactions;
    }
    private List<Transaction> generateFailedTransactions(long blockId, int number) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < number; i++) {
           transactions.add(generateFailedTx(blockId, i));
        }
        return transactions;
    }

    private Transaction generateOkTx() {
        long txId = counter.incrementAndGet();
        Transaction tx = mock(Transaction.class);
        doReturn(Optional.empty()).when(tx).getErrorMessage();
        lenient().doReturn(txId).when(tx).getId();
        lenient().doReturn(Long.toUnsignedString(txId)).when(tx).getStringId();
        return tx;
    }

    private Transaction generateFailedTx(long blockId, int index) {
        Transaction tx = generateOkTx();
        doReturn(Optional.of("Transaction Error " + blockId + "-" + index)).when(tx).getErrorMessage();
        failedTransactions.add(tx);
        return tx;
    }

    private List<Transaction> generateMixed(long blockId, int number) {
        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            Transaction tx;
            if (random.nextBoolean()) {
                tx = generateFailedTx(blockId, i);
            } else {
                tx = generateOkTx();
            }
            transactions.add(tx);
        }
        return transactions;
    }
}