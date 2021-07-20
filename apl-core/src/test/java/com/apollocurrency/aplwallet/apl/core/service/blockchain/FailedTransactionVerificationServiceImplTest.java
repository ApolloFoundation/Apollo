/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.exception.AplCoreContractViolationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionNotFoundException;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TxsVerificationResult;
import com.apollocurrency.aplwallet.apl.core.model.VerifiedTransaction;
import com.apollocurrency.aplwallet.apl.core.peer.Peer;
import com.apollocurrency.aplwallet.apl.core.peer.PeerNotConnectedException;
import com.apollocurrency.aplwallet.apl.core.peer.PeerState;
import com.apollocurrency.aplwallet.apl.core.peer.PeersService;
import com.apollocurrency.aplwallet.apl.core.peer.parser.GetTransactionsResponseParser;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FailedTransactionVerificationServiceImplTest {
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
    TaskDispatcher dispatcher;
    @Mock
    PeersService peersService;
    @Mock
    OptionDAO optionDAO;

    FailedTransactionVerificationServiceImpl service;

    AtomicInteger txIdCounter = new AtomicInteger(0);
    AtomicInteger peerSendingRequestCounter = new AtomicInteger(0);
    List<Transaction> failedTxs = new ArrayList<>();


    @Test
    void verifyFailedTransactions_notEnoughPeersCorrectData() throws PeerNotConnectedException {
        setUpMocksForSchedulerLaunch();
        doAnswer(invocation -> null).when(peer1).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAcceptableNotFullTxsResponse).when(peer2).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAcceptableNotFullTxsResponse).when(peer3).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation ->  throwNotConnectedException()).when(peer4).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> throwNotConnectedException()).when(peer5).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> throwNotConnectedException()).when(peer6).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));

        // launch processing iteration
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        verifyNoBlacklist(List.of(peer1, peer2, peer3, peer4, peer5, peer6));
        verifyNoVerifiedTransactions();
    }

    @Test
    void verifyFailedTransactions_OK() throws PeerNotConnectedException {
        setUpMocksWithDispatcher();
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer1).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnTooManyTxs).when(peer2).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnIncorrectTxByIdsResponse).when(peer3).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation ->  null).when(peer4).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(invocation -> throwNotConnectedException()).when(peer5).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer6).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        service.verifyTransactions();

        verifyNoBlacklist(List.of(peer1, peer4, peer5, peer6));
        verifyBlacklist(List.of(peer2, peer3));
        TxsVerificationResult result = verifySuccessfulEnding();
        assertEquals(Set.of(), result.allNotVerifiedIds());
        assertEquals(failedTxs.stream().map(Transaction::getId).collect(Collectors.toSet()), result.allVerifiedIds());
    }

    @Test
    void createVerificationService_backgroundVerificationIsDisabled_failedTxFeatureIsDisabled() {
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), null));

        assertFalse(service.isEnabled(), "Background processing should be disabled when failed tx feature is disabled");
        verifyNoInteractions(peer1, peer2, peer3, peer4, peer5, peer6, dispatcher, dispatchManager, blockchain, peersService);
    }

    @Test
    void createVerificationService_backgroundVerificationIsDisabled_lessThanOneConfirmationForTxSet() {
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(0, 5, UUID.randomUUID(), 0));

        assertFalse(service.isEnabled(), "Background processing should be disabled when number of confirmations set < 1");
        verifyNoInteractions(peer1, peer2, peer3, peer4, peer5, peer6, dispatcher, dispatchManager, blockchain, peersService);
    }

    @Test
    void verifyFailedTransactions_manually_notEnoughBlocks() {
        mockDispatchManager();
        doReturn("1").when(optionDAO).get("lastFailedTransactionsVerificationBlock");
        doReturn(1440).when(blockchain).getHeight();
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        Optional<TxsVerificationResult> result = service.verifyTransactions();

        assertTrue(result.isEmpty(), "Result should not be present when not enough blocks");
        assertTrue(service.isEnabled(), "Background verification should be enabled when not enough blocks");
        assertTrue(service.getLastVerificationResult().isEmpty(), "No verification should be done when not enough blocks");
        assertEquals(1, service.getLastVerifiedBlockHeight());
        verifyNoInteractions(peer1, peer2, peer3, peer4, peer5, peer6, peersService);
        verify(optionDAO, never()).set(anyString(), anyString());
    }

    @Test
    void verifyFailedTransactions_noTransactions() {
        mockDispatchManager();
        doReturn("10").when(optionDAO).get("lastFailedTransactionsVerificationBlock");
        doReturn(1460).when(blockchain).getHeight();
        List<Block> blocks = createEmptyMockBlocks();
        doReturn(blocks).when(blockchain).getBlocksAfter(20, 1440);
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 20));

        Optional<TxsVerificationResult> result = service.verifyTransactions();

        assertTrue(result.isEmpty(), "Result should not be present when failed transactions do not exist");
        assertTrue(service.isEnabled(), "Background verification should be enabled when not enough blocks");
        assertTrue(service.getLastVerificationResult().isEmpty(), "No verification should be done when not enough blocks");
        assertEquals(10, service.getLastVerifiedBlockHeight());
        verifyNoInteractions(peer1, peer2, peer3, peer4, peer5, peer6, peersService);
        verify(optionDAO, never()).set(anyString(), anyString());
    }


    @Test
    void verifyFailedTransactionManually_disabledProcessing() {
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), null));

        AplFeatureNotEnabledException ex = assertThrows(AplFeatureNotEnabledException.class, () -> service.verifyTransaction(1L));

        assertEquals("Feature 'Failed transactions processing' is not enabled, details: 'txId= 1'", ex.getMessage());
    }

    @Test
    void verifyFailedTransactionManually_transactionNotFound() {
        mockDispatchManager();
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        AplTransactionNotFoundException ex = assertThrows(AplTransactionNotFoundException.class, () -> service.verifyTransaction(1L));

        assertEquals("Transaction with id '1' was not found, details: 'Verify transaction op'", ex.getMessage());
    }

    @Test
    void verifyFailedTransactionManually_transactionIsNotFailed() {
        mockDispatchManager();
        Transaction tx = mock(Transaction.class);
        doReturn(false).when(tx).isFailed();
        doReturn(tx).when(blockchain).getTransaction(1);
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        TxsVerificationResult result = service.verifyTransaction(1L);

        assertTrue(result.isEmpty(), "For not failed tx, there should be no verification results");
    }

    @Test
    void verifyFailedTransactionManually_transactionIsFailed_contractViolationNoErrorMessage() {
        mockDispatchManager();
        Transaction tx = mock(Transaction.class);
        doReturn(true).when(tx).isFailed();
        doReturn(tx).when(blockchain).getTransaction(1);
        doReturn("1").when(tx).getStringId();
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, UUID.randomUUID(), 0));

        AplCoreContractViolationException ex = assertThrows(AplCoreContractViolationException.class, () -> service.verifyTransaction(1L));

        assertEquals("Failed transaction should have error message, id = 1", ex.getMessage());
    }

    @Test
    void verifyFailedTransactionManually_OK() throws PeerNotConnectedException {
        mockDispatchManager();
        UUID chainId = UUID.randomUUID();
        Transaction tx = generateFailedTx(1L, 0);
        long txId = txIdCounter.get();
        doReturn(tx).when(blockchain).getTransaction(txId);
        doReturn(List.of(peer1, peer3)).when(peersService).getPublicPeers(PeerState.CONNECTED, true);
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer1).send(new GetTransactionsRequest(Set.of(txId), chainId), new GetTransactionsResponseParser());
        doAnswer(this::returnAllRequestedCorrectTxs).when(peer3).send(new GetTransactionsRequest(Set.of(txId), chainId), new GetTransactionsResponseParser());
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(2, 5, chainId, 0));

        TxsVerificationResult result = service.verifyTransaction(txId);

        assertTrue(result.isVerified(txId), "Transaction with id = " + txId +" should be successfully verified");
        Optional<VerifiedTransaction> transactionOpt = result.get(txId);
        assertTrue(transactionOpt.isPresent(), "Transaction with id " + txId + " should be present inside the verification result");
        VerifiedTransaction verifiedTx = transactionOpt.get();
        assertEquals(2, verifiedTx.getCount());
        assertEquals("Transaction Error 1-0", verifiedTx.getError());
        assertEquals(txId, verifiedTx.getId());
        verifyNoBlacklist(List.of(peer1,  peer3));
    }

    @Test
    void onBlockPopped_toHeightOfLastProcessed() {
        mockDispatchManager();
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(1, 5, UUID.randomUUID(), 900));
        doReturn("1000").when(optionDAO).get("lastFailedTransactionsVerificationBlock");
        Block block = mock(Block.class);
        doReturn(1000).when(block).getHeight();

        service.onBlockPopped(block);

        verify(optionDAO).set("lastFailedTransactionsVerificationBlock", "999");
    }

    @Test
    void onBlockPopped_toHeightGreaterThanLastProcessed() {
        mockDispatchManager();
        service = new FailedTransactionVerificationServiceImpl(blockchain, peersService, optionDAO, dispatchManager, new FailedTransactionVerificationConfig(1, 5, UUID.randomUUID(), 900));
        doReturn("1000").when(optionDAO).get("lastFailedTransactionsVerificationBlock");
        Block block = mock(Block.class);
        doReturn(1001).when(block).getHeight();

        service.onBlockPopped(block);

        verify(optionDAO, never()).set(anyString(), anyString());
    }

    private void setUpMocksForSchedulerLaunch() {
        setUpMocks();
        launchTaskOnScheduling();
    }


    private void launchTaskOnScheduling() {
        mockDispatchManager();
        doAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            assertEquals(60_000, t.getInitialDelay());
            assertEquals(10_000, t.getDelay());
            assertEquals("FailedTransactionVerificationTask", t.getName());
            t.run();  // launch processing
            return null;
        }).when(dispatcher).schedule(any(Task.class));
    }

    private void mockDispatchManager() {
        doReturn(dispatcher).when(dispatchManager).newScheduledDispatcher(FailedTransactionVerificationServiceImpl.class.getSimpleName());
    }

    private void verifyNoVerifiedTransactions() {
        TxsVerificationResult result = verifySuccessfulEnding();
        assertEquals(0, result.allVerifiedIds().size());
        assertEquals(failedTxs.stream().map(Transaction::getId).collect(Collectors.toSet()), result.allNotVerifiedIds());
    }

    private TxsVerificationResult verifySuccessfulEnding() {
        verify(optionDAO).set("lastFailedTransactionsVerificationBlock", String.valueOf(720));
        Optional<TxsVerificationResult> resultOp = service.getLastVerificationResult();
        assertTrue(resultOp.isPresent());
        return resultOp.get();
    }

    private void setUpMocks() {
        List<Block> mockBlocks = createMockBlocks();
        doReturn(1440).when(blockchain).getHeight();
        doReturn(List.of(peer1, peer2, peer3, peer4, peer5, peer6)).when(peersService).getPublicPeers(PeerState.CONNECTED, true);
        doReturn(mockBlocks).when(blockchain).getBlocksAfter(0, 1440);
    }

    private void setUpMocksWithDispatcher() {
        setUpMocks();
        mockDispatchManager();
    }

    private void verifyNoBlacklist(List<Peer> peers) {
        peers.forEach(e-> {
            verify(e, never()).blacklist(anyString());
            try {
                verify(e, atLeastOnce()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
            } catch (PeerNotConnectedException peerNotConnectedException) {
                fail(peerNotConnectedException);
            }
        });
    }

    private void verifyBlacklist(List<Peer> peers) {
        peers.forEach(e-> {
            verify(e, atLeast(1)).blacklist(anyString());
            try {
                verify(e, atLeastOnce()).send(any(GetTransactionsRequest.class), any(GetTransactionsResponseParser.class));
            } catch (PeerNotConnectedException peerNotConnectedException) {
                fail(peerNotConnectedException);
            }
        });
    }

    private GetTransactionsResponse returnAllRequestedCorrectTxs(InvocationOnMock invocation) {
        GetTransactionsRequest request = invocation.getArgument(0);
        List<TransactionDTO> txsToReturn = new ArrayList<>();
        request.getTransactionIds().forEach(e-> {
            Optional<Transaction> foundTx = failedTxs.stream().filter(tx -> tx.getId() == e).findFirst();
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
        Optional<Transaction> foundTx = failedTxs.stream().filter(tx -> tx.getId() == id).findFirst();
        if (foundTx.isEmpty()) {
            throw new IllegalStateException("Unknown transaction requested from peer: " + id);
        }
        Transaction failedTx = foundTx.get();
        String error = failedTx.getErrorMessage().orElseThrow(() -> new IllegalStateException("Failed tx: " + failedTx.getId() + " should have error message"));
        return createTransactionDTO(failedTx.getStringId(), error);
    }

    private void mockBlockTxs(List<Block> pushedBlocks) {
        for (int i = 0; i < pushedBlocks.size(); i++) {
            Block block = pushedBlocks.get(i);
            int txNumber = new Random().nextInt(2) + 1;
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
        lenient().doReturn(true).when(tx).isFailed();
        failedTxs.add(tx);
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

    private List<Block> createMockBlocks() {
        List<Block> blocks = createEmptyMockBlocks();
        mockBlockTxs(blocks.subList(0, 720));
        return blocks;
    }
    private List<Block> createEmptyMockBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < 1440; i++) {
            Block block = mockIdBlock(String.valueOf(i));
            lenient().doReturn(List.of()).when(block).getTransactions();
            blocks.add(block);
        }
        return blocks;
    }
}