package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.AplWalletKey;
import com.apollocurrency.aplwallet.apl.core.model.ApolloFbWallet;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.eth.web3j.ComparableStaticGasProvider;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.exchange.model.EthStationGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
class DexSmartContractServiceTest {
    @Mock
    private Web3j web3j;
    @Mock
    private KeyStoreService keyStoreService;

    private PropertiesHolder holder;
    @Mock
    private DexEthService dexEthService;
    @Mock
    private DexContract dexContract;
    @Mock
    private EthereumWalletService ethereumWalletService;
    @Mock
    private DexTransactionDao dexTransactionDao;
    private DexSmartContractService service;
    private static final long ALICE_ID = 100;
    private static final String ALICE_PASS = "PASS";
    private static final String ALICE_ETH_ADDRESS = "0x155b6577d5b73d779ce7ef4d397821dde1f6d26c";
    private static final String PAX_ETH_ADDRESS = "0xc3188f569Ec3fD52335B8BcDB4A57A3cc377c221";
    private static final String SWAP_ETH_ADDRESS = "0x64A2759A779d0928A00082621c0BB4b8050144f9";
    private static final String ALICE_PRIV_KEY = "f47759941904a9bf6f89736c4541d850107c9be6ec619e7e65cf80a14ff7e8e4";

    private EthWalletKey aliceWalletKey;
    private WalletKeysInfo aliceWalletKeysInfo;

    DexOrder order = new DexOrder(2L, 100L, "from-address", "to-address", OrderType.BUY, OrderStatus.OPEN, DexCurrencies.APL, 127_000_000L, DexCurrencies.ETH, BigDecimal.valueOf(0.0001), 500);

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("apl.eth.swap.contract.address", SWAP_ETH_ADDRESS);
        props.setProperty("apl.eth.pax.contract.address", PAX_ETH_ADDRESS);
        holder = new PropertiesHolder();
        holder.init(props);
        service = spy(new DexSmartContractService(web3j, holder, keyStoreService, dexEthService, ethereumWalletService, dexTransactionDao));
        aliceWalletKey = new EthWalletKey(Credentials.create(ECKeyPair.create(Crypto.getPrivateKey(Convert.parseHexString(ALICE_PRIV_KEY)))));
        ApolloFbWallet apolloFbWallet = new ApolloFbWallet();
        apolloFbWallet.addAplKey(new AplWalletKey(Convert.parseHexString(ALICE_PRIV_KEY)));
        apolloFbWallet.addEthKey(aliceWalletKey);
        aliceWalletKeysInfo = new WalletKeysInfo(apolloFbWallet, ALICE_PASS);
    }


    @Test
    void testHasFrozenMoneyForSellOrder() {
        order.setType(OrderType.SELL);

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrder() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of(new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000126), 2L), new UserEthDepositInfo(1L, BigDecimal.valueOf(0.000127), 1L), new UserEthDepositInfo(order.getId(), BigDecimal.valueOf(0.000127), 1L));
        doReturn(userDeposits).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertTrue(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithoutUserDeposits() throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposits = List.of();
        doReturn(userDeposits).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertFalse(result);
    }

    @Test
    void testHasFrozenMoneyForBuyOrderWithException() throws AplException.ExecutiveProcessException {
        doThrow(new AplException.ExecutiveProcessException()).when(service).getUserFilledDeposits(order.getFromAddress());

        boolean result = service.hasFrozenMoney(order);

        assertFalse(result);
    }

    @Test
    void testDepositEth() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, null, DexCurrencies.ETH);

        assertEquals("hash", hash);
    }
    @Test
    void testDepositEthWithException() throws ExecutionException, AplException.ExecutiveProcessException {
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doThrow(new RuntimeException()).when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, null, DexCurrencies.ETH);

        assertNull( hash);
    }

    @Test
    void testDepositPaxWithoutAllowance() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doReturn(BigInteger.ZERO).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);
        doReturn("approve-hash").when(ethereumWalletService).sendApproveTransaction(aliceWalletKey, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount, PAX_ETH_ADDRESS);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, null, DexCurrencies.PAX);

        assertEquals("hash", hash);
    }

    @Test
    void testDepositPaxWithAllowance() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(new EthStationGasInfo(25.0, 20.0, 18.0)).when(dexEthService).getEthPriceInfo();
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(25_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doReturn(amount).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount, PAX_ETH_ADDRESS);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, null, DexCurrencies.PAX);

        assertEquals("hash", hash);
        verify(ethereumWalletService, never()).sendApproveTransaction(aliceWalletKey, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositEthWithoutGasPrice() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        BigInteger amount = BigInteger.valueOf(150_000_000_000_000L);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(27_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), amount);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 27L, DexCurrencies.ETH);

        assertEquals("hash", hash);
    }

    @Test
    void testDepositOnExceptionDuringAllowance() throws IOException, ExecutionException, AplException.ExecutiveProcessException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doThrow(new IOException()).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);


        assertThrows(RuntimeException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 10L, DexCurrencies.PAX));

        verifyZeroInteractions(dexContract);
        verify(ethereumWalletService, never()).sendApproveTransaction(aliceWalletKey, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositNotSupportedCurrency() throws IOException, ExecutionException, AplException.ExecutiveProcessException {
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);

        assertThrows(UnsupportedOperationException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.APL));

        verifyZeroInteractions(dexContract);
        verify(ethereumWalletService, never()).sendApproveTransaction(aliceWalletKey, SWAP_ETH_ADDRESS, Constants.ETH_MAX_POS_INT);
    }

    @Test
    void testDepositWithExceptionDuringApproving() throws IOException, AplException.ExecutiveProcessException {
        BigInteger amount = EthUtil.etherToWei(BigDecimal.ONE);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(BigInteger.ZERO).when(ethereumWalletService).getAllowance(SWAP_ETH_ADDRESS, ALICE_ETH_ADDRESS, PAX_ETH_ADDRESS);

        assertThrows(AplException.ExecutiveProcessException.class, () -> service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, amount, 10L, DexCurrencies.PAX));
    }

    @Test
    void testDepositWhenUncTransactionWasSentBefore() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);
    }


    @Test
    void testDepositWhenConfirmedTxWasSent() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn(BigInteger.ONE).when(responseTx).getBlockNumber();
        TransactionReceipt responseReceipt = mock(TransactionReceipt.class);
        doReturn(Optional.ofNullable(responseReceipt)).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));
        doReturn("0x1").when(responseReceipt).getStatus();

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);

    }

    @Test
    void testDepositWhenConfirmedFailedTxWasSent() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn(BigInteger.ONE).when(responseTx).getBlockNumber();
        TransactionReceipt responseReceipt = mock(TransactionReceipt.class);
        doReturn(Optional.ofNullable(responseReceipt)).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));
        doReturn("0x0").when(responseReceipt).getStatus();

        doReturn(dexContract).when(service).createDexContract(new ComparableStaticGasProvider(BigInteger.valueOf(27_000_000_000L), BigInteger.valueOf(400_000)), new DexTransaction(null, null, null, DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 0), aliceWalletKey.getCredentials());
        doReturn("hash").when(dexContract).deposit(BigInteger.valueOf(100), BigInteger.TEN);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 27L, DexCurrencies.ETH);

        assertEquals("hash", hash);
        verify(dexTransactionDao).delete(1L);
    }

    @Test
    void testDepositWhenConfirmedReceiptIsNull() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        Transaction responseTx = mock(Transaction.class);
        doReturn(Optional.ofNullable(responseTx)).when(service).getTxByHash(Numeric.toHexString(new byte[32]));
        doReturn(BigInteger.ONE).when(responseTx).getBlockNumber();
        doReturn(Optional.empty()).when(service).getTxReceipt(Numeric.toHexString(new byte[32]));

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.ETH);

        assertEquals(Numeric.toHexString(new byte[32]), hash);
    }

    @Test
    void testDepositWhenTxWasNotSent() throws ExecutionException, AplException.ExecutiveProcessException, IOException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        String empty32Bytes = Numeric.toHexString(new byte[32]);
        doReturn(Optional.empty()).when(service).getTxByHash(empty32Bytes);
        doReturn("hash").when(service).sendRawTransaction(empty32Bytes);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.ETH);

        assertEquals(empty32Bytes, hash);
        verify(service).sendRawTransaction(empty32Bytes);
    }

    @Test
    void testDepositWhenUnableToGetResponseFromNode() throws IOException, ExecutionException, AplException.ExecutiveProcessException {
        DexTransaction tx = new DexTransaction(1L, new byte[32], new byte[32], DexTransaction.DexOperation.DEPOSIT, "100", ALICE_ETH_ADDRESS, 150);
        doReturn(aliceWalletKeysInfo).when(keyStoreService).getWalletKeysInfo(ALICE_PASS, ALICE_ID);
        doReturn(tx).when(dexTransactionDao).get(tx.getParams(), tx.getAccount(), tx.getOperation());
        String empty32Bytes = Numeric.toHexString(new byte[32]);
        doThrow(new IOException()).when(service).getTxByHash(empty32Bytes);

        String hash = service.deposit(ALICE_PASS, 100L, ALICE_ID, ALICE_ETH_ADDRESS, BigInteger.TEN, 10L, DexCurrencies.ETH);

        assertEquals(empty32Bytes, hash);
    }

}