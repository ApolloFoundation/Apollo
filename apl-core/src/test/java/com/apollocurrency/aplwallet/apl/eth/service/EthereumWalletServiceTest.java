package com.apollocurrency.aplwallet.apl.eth.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.exchange.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.exchange.exception.NotSufficientFundsException;
import com.apollocurrency.aplwallet.apl.exchange.exception.NotValidTransactionException;
import com.apollocurrency.aplwallet.apl.exchange.service.DexBeanProducer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EthereumWalletServiceTest {
    @Mock
    Web3j web3j;
    @Mock
    DexBeanProducer dexBeanProducer;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    KeyStoreService keyStoreService;
    @Mock
    DexEthService dexEthService;
    @Mock
    UserErrorMessageDao userErrorMessageDao;
    String txHash = "0x88df016429689c079f3b2f6ad39fa052532c56795b733da78a91ebe6a713944b";
    String accountAddress = "0x68957bB72A36f721610742B4Ca0C86c60ceD5845";
    Function function = new Function("redeem", Collections.singletonList(new Bytes32(new byte[32])), Collections.emptyList());
    EthereumWalletService service;

    @BeforeEach
    void setUp() {
        service = new EthereumWalletService(propertiesHolder, keyStoreService, dexEthService, userErrorMessageDao, dexBeanProducer);
        when(dexBeanProducer.web3j()).thenReturn(web3j);
        service.init();
    }

    @Test
    void testGetNumberOfConfirmationsWhenResponseAboutTransactionIsNull() {
        mockTxReceiptRequest();

        assertNoConfirmations();
    }

    @Test
    void testGetNumberOfConfirmationsWhenTransactionWithGivenHashWasNotFound() throws IOException {
        mockTxReceiptRequestResponse();

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
    void testGetNumberOfConfirmationsWhenBlockNumberResponseContainsError() throws IOException {
        mockTxErrorInResponse("Node is not available");

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
        Request request = mockTxReceiptRequest();
        doThrow(new IOException()).when(request).send();

        assertThrows(RuntimeException.class, () -> service.getNumberOfConfirmations(txHash));
    }

    private void assertNoConfirmations() {
        int numberOfConfirmations = service.getNumberOfConfirmations(txHash);

        assertEquals(-1, numberOfConfirmations);
    }

    private Request mockTxReceiptRequest() {
        Request mockRequest = mock(Request.class);
        doReturn(mockRequest).when(web3j).ethGetTransactionReceipt(txHash);
        return mockRequest;
    }

    private Request mockEstimateGas() {
        Request mockRequest = mock(Request.class);
        doReturn(mockRequest).when(web3j).ethEstimateGas(any(org.web3j.protocol.core.methods.request.Transaction.class));
        return mockRequest;
    }

    private EthEstimateGas mockEstimateGasRequestResponse(Response.Error error, BigInteger estimate) throws IOException {
        Request request = mockEstimateGas();
        EthEstimateGas ethEstimateGas = new EthEstimateGas();
        if (error != null) {
            ethEstimateGas.setError(error);
        } else {
            ethEstimateGas.setResult(Numeric.encodeQuantity(estimate));
        }
        doReturn(ethEstimateGas).when(request).send();
        return ethEstimateGas;
    }

    private EthGetTransactionReceipt mockTxReceiptRequestResponse() throws IOException {
        Request request = mockTxReceiptRequest();
        EthGetTransactionReceipt ethTransactionResponse = mock(EthGetTransactionReceipt.class);
        doReturn(ethTransactionResponse).when(request).send();
        return ethTransactionResponse;
    }

    private void mockTxInResponse(BigInteger blockNumber) throws IOException {
        EthGetTransactionReceipt ethTransaction = mockTxReceiptRequestResponse();
        TransactionReceipt receipt = mock(TransactionReceipt.class);
        doReturn(receipt).when(ethTransaction).getResult();
        doReturn(blockNumber).when(receipt).getBlockNumber();
    }

    private void mockTxErrorInResponse(String error) throws IOException {
        EthGetTransactionReceipt ethTransaction = mockTxReceiptRequestResponse();
        Response.Error err = new Response.Error(0, error);
        doReturn(err).when(ethTransaction).getError();
        doReturn(true).when(ethTransaction).hasError();
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

    @Test
    void testEstimateGasWithError() throws IOException {
        mockEstimateGasRequestResponse(new Response.Error(1, "Error"), null);

        assertThrows(NotValidTransactionException.class, () -> service.estimateGasLimit(accountAddress, "", function, BigInteger.TEN));
    }

    @Test
    void testEstimateGas() throws IOException {
        mockEstimateGasRequestResponse(null, BigInteger.TEN);

        BigInteger gasLimit = service.estimateGasLimit(accountAddress, "", function, BigInteger.TEN);

        assertEquals(BigInteger.valueOf(11), gasLimit);

    }

    @Test
    void testEstimateGasWithException() throws IOException {
        Request request = mockEstimateGas();
        doThrow(new IOException()).when(request).send();

        assertThrows(RuntimeException.class, () -> service.estimateGasLimit(accountAddress, "", function, BigInteger.TEN));
    }

    @Test
    void testValidateBalance() throws IOException {
        mockEstimateGasRequestResponse(null, BigInteger.TEN);

        Request balanceRequest = mock(Request.class);
        doReturn(balanceRequest).when(web3j).ethGetBalance(accountAddress, DefaultBlockParameterName.LATEST);
        EthGetBalance getBalanceResponse = new EthGetBalance();
        getBalanceResponse.setResult("0x64");
        doReturn(getBalanceResponse).when(balanceRequest).send();

        BigInteger gasLimit = service.validateBalanceAndReturnGasLimit(accountAddress, "", function, BigInteger.TEN, BigInteger.TWO);

        assertEquals(BigInteger.valueOf(11), gasLimit);
    }

    @Test
    void testValidateBalanceMatchExact() throws IOException {
        mockEstimateGasRequestResponse(null, BigInteger.valueOf(5));

        Request balanceRequest = mock(Request.class);
        doReturn(balanceRequest).when(web3j).ethGetBalance(accountAddress, DefaultBlockParameterName.LATEST);
        EthGetBalance getBalanceResponse = new EthGetBalance();
        getBalanceResponse.setResult("0x64");
        doReturn(getBalanceResponse).when(balanceRequest).send();

        BigInteger gasLimit = service.validateBalanceAndReturnGasLimit(accountAddress, "", function, BigInteger.valueOf(90), BigInteger.TWO);

        assertEquals(BigInteger.valueOf(5), gasLimit);
    }

    @Test
    void testValidateBalanceNotEnoughFundsToPayFee() throws IOException {
        mockEstimateGasRequestResponse(null, BigInteger.TEN);

        Request balanceRequest = mock(Request.class);
        doReturn(balanceRequest).when(web3j).ethGetBalance(accountAddress, DefaultBlockParameterName.LATEST);
        EthGetBalance getBalanceResponse = new EthGetBalance();
        getBalanceResponse.setResult("0x64");
        doReturn(getBalanceResponse).when(balanceRequest).send();

        assertThrows(NotSufficientFundsException.class, () -> service.validateBalanceAndReturnGasLimit(accountAddress, "", function, BigInteger.valueOf(79), BigInteger.TWO));
    }
}