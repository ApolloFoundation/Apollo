/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.rest.v2.impl;

import com.apollocurrency.aplwallet.api.v2.NotFoundException;
import com.apollocurrency.aplwallet.api.v2.TransactionApiService;
import com.apollocurrency.aplwallet.api.v2.model.BaseResponse;
import com.apollocurrency.aplwallet.api.v2.model.ErrorResponse;
import com.apollocurrency.aplwallet.api.v2.model.ListResponse;
import com.apollocurrency.aplwallet.api.v2.model.TransactionInfoResp;
import com.apollocurrency.aplwallet.api.v2.model.TxReceipt;
import com.apollocurrency.aplwallet.api.v2.model.TxRequest;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TransactionInfoMapper;
import com.apollocurrency.aplwallet.apl.core.rest.v2.converter.TxReceiptMapper;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.MemPool;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.child.CreateChildTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SIGNED_TX_1_HEX;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SIGNED_TX_1_WRONG_HEX_FORMAT;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SIGNED_TX_1_WRONG_LENGTH;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.TX_1_ID;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.TX_1_SIGNATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class TransactionApiServiceImplTest {

    @Mock
    Blockchain blockchain;
    @Mock
    SecurityContext securityContext;

    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    Chain chain = mock(Chain.class);

    {
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @Mock
    BlockChainInfoService blockChainInfoService;

    MemPool memPool = mock(MemPool.class);
    @Mock
    PrunableLoadingService prunableLoadingService;
    TransactionBuilderFactory transactionBuilderFactory;
    @Mock
    AccountService accountService;
    @Mock
    AccountPublicKeyService accountPublicKeyService;
    @Mock
    TransactionProcessor processor;

    TxReceiptMapper txReceiptMapper;
    TransactionInfoMapper transactionInfoMapper;
    TransactionApiService transactionApiService;

    @BeforeEach
    void setUp() {
        transactionBuilderFactory = new TransactionBuilderFactory(new CachedTransactionTypeFactory(List.of(new CreateChildTransactionType(blockchainConfig, accountService, accountPublicKeyService, blockchain))), blockchainConfig);
        Convert2.init("APL", 1739068987193023818L);
        txReceiptMapper = new TxReceiptMapper(blockChainInfoService);
        transactionInfoMapper = new TransactionInfoMapper(blockchain, prunableLoadingService);
        transactionApiService = new TransactionApiServiceImpl(memPool, blockchainConfig, blockchain, txReceiptMapper, transactionInfoMapper, transactionBuilderFactory, processor);
    }

    @SneakyThrows
    @Test
    void broadcastTx() {
        //GIVEN
        TxRequest request = new TxRequest();
        request.setTx(SIGNED_TX_1_HEX);
        doReturn(1).when(memPool).canAcceptToProcessingQueue();
//        doReturn(true).when(memPool).softBroadcast(any(Transaction.class));
        //WHEN
        Response response = transactionApiService.broadcastTx(request, securityContext);
        //THEN
        assertNotNull(response);
        TxReceipt receipt = (TxReceipt) response.getEntity();
        assertEquals(TX_1_ID, receipt.getTransaction());
    }

    @SneakyThrows
    @Test
    void broadcastTxWhenBlockchainIsBusy() {
        //GIVEN
        TxRequest request = new TxRequest();
        request.setTx(SIGNED_TX_1_HEX);
        doReturn(0).when(memPool).canAcceptToProcessingQueue();
        //WHEN
        Response response = transactionApiService.broadcastTx(request, securityContext);
        //THEN
        assertNotNull(response);
        assertEquals(409, response.getStatus());
        ErrorResponse receipt = (ErrorResponse) response.getEntity();
        assertEquals(102, receipt.getErrorCode());
    }

    @ParameterizedTest(name = "tx {index}")
    @ValueSource(strings = {SIGNED_TX_1_WRONG_LENGTH, SIGNED_TX_1_WRONG_HEX_FORMAT})
    void broadcastTx_wrongTxByteArray(String txStr) {
        //GIVEN
        TxRequest request = new TxRequest();
        request.setTx(txStr);
        //WHEN
        Response response = transactionApiService.broadcastTx(request, securityContext);
        //THEN
        assertNotNull(response);
        BaseResponse receipt = (BaseResponse) response.getEntity();
        if (receipt instanceof ErrorResponse) {
            assertTrue(((ErrorResponse) receipt).getErrorCode() > 0);
        } else {
            fail("Unexpected flow.");
        }
    }

    @SneakyThrows
    @Test
    void broadcastTxBatch() {
        //GIVEN
        List<TxRequest> requestList = new ArrayList<>();
        TxRequest request = new TxRequest();
        request.setTx(SIGNED_TX_1_HEX);
        requestList.add(request);

        TxRequest request2 = new TxRequest();
        request2.setTx(SIGNED_TX_1_WRONG_LENGTH);
        requestList.add(request2);

        doReturn(2).when(memPool).canAcceptToProcessingQueue();
//        doReturn(true).when(memPool).softBroadcast(any(Transaction.class));

        //WHEN
        Response response = transactionApiService.broadcastTxBatch(requestList, securityContext);
        //THEN
        assertNotNull(response);
        ListResponse receipt = (ListResponse) response.getEntity();

        assertEquals(requestList.size(), receipt.getResult().size());
        assertEquals(TX_1_ID, ((TxReceipt) receipt.getResult().get(0)).getTransaction());
        assertEquals(2001, ((ErrorResponse) receipt.getResult().get(1)).getErrorCode());
    }

    @Test
    void getTxById() {
        //GIVEN
        String txIdStr = TX_1_ID;
        long txId = Convert.parseUnsignedLong(txIdStr);
        Transaction transaction = mock(Transaction.class);
        Signature signature = spy(SignatureToolFactory.createSignature(Convert.parseHexString(TX_1_SIGNATURE)));
        doReturn(Convert.parseHexString(TX_1_SIGNATURE)).when(signature).bytes();
        CreateChildTransactionType childTransactionType = new CreateChildTransactionType(blockchainConfig, accountService, accountPublicKeyService, blockchain);
        doReturn(childTransactionType).when(transaction).getType();
        doReturn(signature).when(transaction).getSignature();
        doReturn(transaction).when(blockchain).getTransaction(txId);
        //WHEN
        Response response = transactionApiService.getTxById(txIdStr, securityContext);
        //THEN
        assertNotNull(response);
        if (response.getEntity() instanceof ErrorResponse) {
            fail("Unexpected flow.");
        }
        TransactionInfoResp receipt = (TransactionInfoResp) response.getEntity();
        assertEquals(childTransactionType.getSpec().getType(), Long.parseLong(receipt.getType()));
        assertEquals(TX_1_SIGNATURE, receipt.getSignature());
    }

    @Test
    void getTxById_WithNotFoundException() {
        //GIVEN
        String txId = TX_1_ID;

        //THEN
        assertThrows(NotFoundException.class,
            //WHEN
            () -> transactionApiService.getTxById(txId, securityContext));
    }

    @Test
    void getTxById_withRuntimeException() {
        //GIVEN
        String txId = "TX_1_ID";
        //WHEN
        Response response = transactionApiService.getTxById(txId, securityContext);
        //THEN
        ErrorResponse receipt = (ErrorResponse) response.getEntity();
        assertTrue(receipt.getErrorDescription().contains("Can't parse transaction id"));
    }
}
