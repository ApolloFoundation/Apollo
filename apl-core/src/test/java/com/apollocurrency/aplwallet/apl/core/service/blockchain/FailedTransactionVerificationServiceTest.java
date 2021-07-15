/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetNextBlocksRequest;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.core.peer.respons.GetNextBlocksResponse;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FailedTransactionVerificationServiceTest {
    @Mock
    Peer peer1;
    @Mock
    Peer peer2;
    @Mock
    Peer peer3;
    @Mock
    Peer peer4;
    @Mock
    Peer peer5;
    @Mock
    Peer peer6;
    @Mock
    Blockchain blockchain;
    @Mock
    TaskDispatchManager dispatchManager;
    @Mock
    PeersService peersService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    OptionDAO optionDAO;

    FailedTransactionVerificationService service;



    @BeforeEach
    void setUp() {
        service = new FailedTransactionVerificationService(blockchain, peersService, blockchainConfig, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 4));
    }

    @Test
    void onBlockPopped() {
    }

    @Test
    void getLastVerificationResult() {
    }

    @Test
    void verifyTransaction() {
    }

    @Test
    void getLastVerifiedBlockHeight() {
    }

    @Test
    void setFailedTransactionsPerRequest() {
    }

    @Test
    void isBackgroundVerificationEnabled() {
    }

    @Test
    void downloadBlockchainProcessingPeersFork_noFailedTransactionsValidation_noEnoughPeersCorrectData() throws PeerNotConnectedException {
        // processing failed transactions mocks
        doReturn(List.of(mockIdBlock("1"), mockIdBlock("2"))).when(blockchain).getBlocksAfter(0, 1440);
        mockTransactionsForBlocks(blocks);
        doAnswer(invocation -> null).when(peer1).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAcceptableNotFullTxsResponse).when(peer2).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAcceptableNotFullTxsResponse).when(peer3).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation ->  throwNotConnectedException()).when(peer4).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> throwNotConnectedException()).when(peer5).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> throwNotConnectedException()).when(peer6).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));

        job.run();

        verifyUnsuccessfulFailedTxsProcessing();
    }


    private void mockTransactionsForBlocks(List<Block> blocks) {
        doAnswer(invocation -> {
            mockBlockTxs(blocks);
            return blocks;
        }).when(blockchain).getBlocksAfter(1999, Integer.MAX_VALUE);
    }

    private void verifyFailedTxsProcessing() {
        verify(peer1, never()).blacklist(anyString());
        verify(peer2, never()).blacklist(anyString());
        verify(peer3, never()).blacklist(anyString());
        verify(peer4, atLeastOnce()).blacklist(anyString());
        verify(peer5, never()).blacklist(anyString());
        verify(peer6, atLeastOnce()).blacklist(anyString());
    }

    private void verifyUnsuccessfulFailedTxsProcessing() {
        List.of(peer1, peer2, peer3, peer4, peer5, peer6).forEach(e-> {
            verify(e, never()).blacklist(anyString());
            try {
                verify(e, atLeastOnce()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
            } catch (PeerNotConnectedException peerNotConnectedException) {
                fail(peerNotConnectedException);
            }
        });
    }

    private void verifyNoFailedTxsProcessing() throws PeerNotConnectedException {
        verify(peer1, never()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        verify(peer2, never()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        verify(peer3, never()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
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
            TransactionDTO txDto = createTransactionDTO(failedTx.getStringId(), error);
            txsToReturn.add(txDto);
        });
        return new GetTransactionsResponse(txsToReturn);
    }

    private GetTransactionsResponse returnAcceptableNotFullTxsResponse(InvocationOnMock invocation) {
        int value = peerSendingRequestCounter.incrementAndGet();
        int decisionValue = value % 3;
        if (decisionValue == 1 ) {
            return null;
        }
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        GetTransactionsRequest request = invocation.getArgument(0);
        if (decisionValue == 2) { // peer contains not all txs
            request.getTransactionIds().stream().limit(request.getTransactionIds().size() / 2).forEach(e -> txsToReturn.add(createTransactionDTOFromTxId(e)));
        } else if (decisionValue == 0) { // peer has failed transactions but with different error message
            request.getTransactionIds().forEach(e -> txsToReturn.add(createTransactionDTO(String.valueOf(e), "another peer error")));
        }
        return new GetTransactionsResponse(txsToReturn);
    }

    private GetTransactionsResponse returnIncorrectTxByIdsResponse(InvocationOnMock invocation) {
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        GetTransactionsRequest request = invocation.getArgument(0);
        request.getTransactionIds().stream().skip(request.getTransactionIds().size() / 2).forEach(e -> txsToReturn.add(createTransactionDTOFromTxId(e)));
        request.getTransactionIds().stream().limit(request.getTransactionIds().size() / 2).forEach(e -> txsToReturn.add(createTransactionDTO(String.valueOf(txIdCounter.incrementAndGet()), "fake error")));
        return new GetTransactionsResponse(txsToReturn);
    }

    private GetTransactionsResponse throwNotConnectedException() throws PeerNotConnectedException {
        throw new PeerNotConnectedException("Peer is not connected test error");
    }

    private GetTransactionsResponse returnTooManyTxs(InvocationOnMock invocation) {
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        GetTransactionsRequest request = invocation.getArgument(0);
        int requestedNumber = request.getTransactionIds().size();
        for (int i = 0; i < requestedNumber + 1; i++) { // overload peer with malicious data
            txsToReturn.add(createTransactionDTO(String.valueOf(i), ""));
        }
        return new GetTransactionsResponse(txsToReturn);
    }



    private TransactionDTO createTransactionDTO(String id, String error) {
        TransactionDTO txDto = mock(TransactionDTO.class);
        doReturn(error).when(txDto).getErrorMessage();
        doReturn(id).when(txDto).getTransaction();
        return txDto;
    }

    private TransactionDTO createTransactionDTOFromTxId(Long id) {
        Optional<Transaction> foundTx = failedTransactions.stream().filter(tx -> tx.getId() == id).findFirst();
        if (foundTx.isEmpty()) {
            throw new IllegalStateException("Unknown transaction requested from peer: " + id);
        }
        Transaction failedTx = foundTx.get();
        String error = failedTx.getErrorMessage().orElseThrow(() -> new IllegalStateException("Failed tx: " + failedTx.getId() + " should have error message"));
        return createTransactionDTO(failedTx.getStringId(), error);
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

    private Object returnNoPeerBlocks(List<String> returnedBlockIds, InvocationOnMock invocation) {
        return new GetNextBlocksResponse(List.of());
    }

    private Object returnNullBlocksResponse() {
        return null;
    }

    private Object returnDifferentBlocksResponse(InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        List<Block> respondedBlocks = new ArrayList<>();
        Random random = new Random();
        for (int i = 1; i < request.getBlockIds().size(); i++) {
            respondedBlocks.add(mockIdBlock(request.getBlockIds().get(i), request.getBlockIds().get(i - 1)));
        }
        return new GetNextBlocksResponse(respondedBlocks);
    }


    private Object returnRandomPeerBlocksPartially(List<String> returnedBlockIds, InvocationOnMock invocation) {
        GetNextBlocksRequest request = invocation.getArgument(0);
        assertTrue(returnedBlockIds.containsAll(request.getBlockIds()), "returned block ids list should contain all the requested block ids from peer");
        List<Block> respondedBlocks = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < request.getBlockIds().subList(0, random.nextInt(request.getBlockIds().size() + 1)).size(); i++) {
            respondedBlocks.add(mockIdBlock(request.getBlockIds().get(i), i == 0 ? request.getBlockId() : request.getBlockIds().get(i - 1)));
        }
        return new GetNextBlocksResponse(respondedBlocks);
    }

    private Block mockIdBlock(String id) {
        Block block = mock(Block.class);
        lenient().doReturn(id).when(block).getStringId();
        lenient().doReturn(Long.parseUnsignedLong(id)).when(block).getId();
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
        long txId = txIdCounter.incrementAndGet();
        Transaction tx = mock(Transaction.class);
        lenient().doReturn(Optional.empty()).when(tx).getErrorMessage();
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