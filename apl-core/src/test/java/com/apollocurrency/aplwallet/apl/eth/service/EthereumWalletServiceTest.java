package com.apollocurrency.aplwallet.apl.eth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;

@ExtendWith(MockitoExtension.class)
class EthereumWalletServiceTest {
    @Mock Web3j web3j;
    @Mock PropertiesHolder propertiesHolder;
    @Mock KeyStoreService keyStoreService;
    @Mock DexEthService dexEthService;
    String txHash = "0x88df016429689c079f3b2f6ad39fa052532c56795b733da78a91ebe6a713944b";
    EthereumWalletService service;

    @BeforeEach
    void setUp() {
        service = new EthereumWalletService(web3j, propertiesHolder, keyStoreService, dexEthService);
    }

    @Test
    void testGetNumberOfConfirmationsWhenResponseAboutTransactionIsNull() {
        mockTxRequest();

        assertNoConfirmations();
    }

    @Test
    void testGetNumberOfConfirmationsWhenTransactionWithGivenHashWasNotFound() throws IOException {
        mockTxRequestResponse();

        assertNoConfirmations();
    }

    @Test
    void testGetNumberOfConfirmationsWhenUnableToGetBlockNumber() throws IOException {
        mockTxInResponse(BigInteger.valueOf(100));
        mockBlockNumberRequest();

        assertNoConfirmations();
    }

    @Test
    void testGetNumberOfConfirmationsWhenBlockNumberResponseIsEmpty() throws IOException {
        mockTxInResponse(BigInteger.valueOf(200));
        mockBlockNumberRequestResponse();

        assertNoConfirmations();
    }

    @Test
    void testGetNumberOfConfirmationsWhenTxInLastBlock() throws IOException {
        mockTxInResponse(BigInteger.valueOf(100));
        mockBlockNumberInResponse(BigInteger.valueOf(100));

        int numberOfConfirmations = service.getNumberOfConfirmations(txHash);

        assertEquals(0, numberOfConfirmations);
    }

    @Test
    void testGetNumberOfConfirmationsWhenTxInPrevBlock() throws IOException {
        mockTxInResponse(BigInteger.valueOf(100));
        mockBlockNumberInResponse(BigInteger.valueOf(103));

        int numberOfConfirmations = service.getNumberOfConfirmations(txHash);

        assertEquals(3, numberOfConfirmations);
    }

    @Test
    void testGetNumberOfConfirmationsWhenIOErrorOccurred() throws IOException {
        Request request = mockTxRequest();
        doThrow(new IOException()).when(request).send();

        assertThrows(RuntimeException.class, () -> service.getNumberOfConfirmations(txHash));
    }

    private void assertNoConfirmations() {
        int numberOfConfirmations = service.getNumberOfConfirmations(txHash);

        assertEquals(-1, numberOfConfirmations);
    }

    private Request mockTxRequest() {
        Request mockRequest = mock(Request.class);
        doReturn(mockRequest).when(web3j).ethGetTransactionByHash(txHash);
        return mockRequest;
    }

    private EthTransaction mockTxRequestResponse() throws IOException {
        Request request = mockTxRequest();
        EthTransaction ethTransactionResponse = mock(EthTransaction.class);
        doReturn(ethTransactionResponse).when(request).send();
        return ethTransactionResponse;
    }

    private void mockTxInResponse(BigInteger blockNumber) throws IOException {
        EthTransaction ethTransaction = mockTxRequestResponse();
        Transaction tx = mock(Transaction.class);
        doReturn(tx).when(ethTransaction).getResult();
        doReturn(blockNumber).when(tx).getBlockNumber();
    }

    private Request mockBlockNumberRequest() {
        Request mockRequest = mock(Request.class);
        doReturn(mockRequest).when(web3j).ethBlockNumber();
        return mockRequest;
    }

    private EthBlockNumber mockBlockNumberRequestResponse() throws IOException {
        Request request = mockBlockNumberRequest();
        EthBlockNumber blockNumberRespnse = mock(EthBlockNumber.class);
        doReturn(blockNumberRespnse).when(request).send();
        return blockNumberRespnse;
    }

    private void mockBlockNumberInResponse(BigInteger blockNumber) throws IOException {
        EthBlockNumber blockNumberResponse = mockBlockNumberRequestResponse();
        doReturn("0x" + blockNumber.toString(16)).when(blockNumberResponse).getResult();
    }
}