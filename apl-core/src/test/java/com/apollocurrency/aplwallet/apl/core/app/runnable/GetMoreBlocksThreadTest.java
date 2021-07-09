/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.runnable;

import com.apollocurrency.aplwallet.api.p2p.request.GetCumulativeDifficultyRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetMilestoneBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlockIdsRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlocksRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetCumulativeDifficultyResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetMilestoneBlockIdsResponse;
import com.apollocurrency.aplwallet.api.p2p.response.GetNextBlockIdsResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.BlockchainProcessorState;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetCumulativeDifficultyResponseParser;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
class GetMoreBlocksThreadTest {
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

    GetMoreBlocksThread thread;
    BlockchainProcessorState state = new BlockchainProcessorState();

    @BeforeEach
    void setUp() {
        doReturn(2).when(propertiesHolder).getIntProperty("apl.numberOfFailedTransactionConfirmations", 3);
        doReturn(chain).when(blockchainConfig).getChain();
        doReturn(chainId).when(chain).getChainId();
        thread = new GetMoreBlocksThread(blockchainProcessor, state, blockchainConfig, blockchain, peersService
            , globalSync, timeService, prunableRestorationService, networkService, propertiesHolder, transactionProcessor, getNextBlocksResponseParser, blockSerializer, getTransactionsResponseParser);
    }

    @Test
    void run() throws PeerNotConnectedException {
        doReturn(1200).when(blockchain).getHeight();
        List<Peer> allPeers = List.of(peer1, peer2, peer3);
        doReturn(allPeers).when(peersService).getPublicPeers(PeerState.CONNECTED, true);
        doReturn(peer1).when(peersService).getWeightedPeer(allPeers);
        GetCumulativeDifficultyResponse cumulativeDiffResponse = new GetCumulativeDifficultyResponse(2100, BigInteger.valueOf(9898));
        doReturn(cumulativeDiffResponse).when(peer1).send(new GetCumulativeDifficultyRequest(chainId), new GetCumulativeDifficultyResponseParser());
        doReturn(currentLastBlock).when(blockchain).getLastBlock();
        doReturn(BigInteger.valueOf(7766)).when(currentLastBlock).getCumulativeDifficulty();
        doReturn(shardInitialBlock).when(blockchain).getShardInitialBlock();
        doReturn(99L).when(shardInitialBlock).getId();
        doReturn("200").when(currentLastBlock).getStringId();
        doReturn(new GetMilestoneBlockIdsResponse(List.of("99", "88", "77"), false)).when(peer1).send(new GetMilestoneBlockIdsRequest(chainId, "200", null));
        List<String> returnedBlockIds = IntStream.range(10_000, 11439).boxed().map(String::valueOf).collect(Collectors.toList());
        doReturn(new GetNextBlockIdsResponse(returnedBlockIds)).when(peer1).send(new GetNextBlockIdsRequest("77", 1440, chainId));
        doAnswer(invocation -> {
            Long blockId = invocation.getArgument(0);
            return blockId == 77 || returnedBlockIds.subList(0, 1000).contains(String.valueOf(blockId)); // getMilestoneBlockId + getNextBlockIds
        }).when(blockchain).hasBlock(anyLong());
        doReturn(commonBlock).when(blockchain).getBlock(11_000L);
        doReturn(2000).when(commonBlock).getHeight();
        doReturn(200).when(currentLastBlock).getId();
        doReturn("peer1IP:1111").when(peer1).getHostWithPort();
        doAnswer(invocation -> returnAllRequestedPeerBlocks(returnedBlockIds, invocation)).when(peer1).send(any(GetNextBlocksRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> returnAllRequestedPeerBlocks(returnedBlockIds, invocation)).when(peer2).send(any(GetNextBlocksRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> returnRequestedPeerBlocksPartially(returnedBlockIds, invocation)).when(peer2).send(any(GetNextBlocksRequest.class), any(GetTransactionsResponseParser.class));




        thread.run();


        assertSame(peer1, state.getLastBlockchainFeeder());
        verify(globalSync).updateLock();
        verify(globalSync).updateUnlock();
        assertEquals(2100, state.getLastBlockchainFeederHeight());

    }

    private Object returnAllRequestedPeerBlocks(List<String> returnedBlockIds, InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        assertTrue(returnedBlockIds.containsAll(request.getBlockIds()), "returned block ids list should contain all the requested block ids from peer");
        List<Block> respondedBlocks = new ArrayList<>();
        request.getBlockIds().forEach(e -> respondedBlocks.add(mockIdBlock(e)));
        return new GetNextBlocksResponse(respondedBlocks);
    }
    private Object returnRequestedPeerBlocksPartially(List<String> returnedBlockIds, InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        assertTrue(returnedBlockIds.containsAll(request.getBlockIds()), "returned block ids list should contain all the requested block ids from peer");
        List<Block> respondedBlocks = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < request.getBlockIds().subList(0, random.nextInt(request.getBlockIds().size() + 1)).size(); i++) {
            respondedBlocks.add(mockIdBlock(request.getBlockIds().get(i), i == 0 ? request.getBlockId() : request.getBlockIds().get(i - 1)));
        });
        return new GetNextBlocksResponse(respondedBlocks);
    }

    private Block mockIdBlock(String id, String prevId) {
        Block block = mock(Block.class);
        lenient().doReturn(id).when(block).getStringId();
        lenient().doReturn(Long.parseUnsignedLong(id)).when(block).getId();
        doReturn(Long.parseUnsignedLong(prevId)).when(block).getPreviousBlockId();
        return block;
    }
}