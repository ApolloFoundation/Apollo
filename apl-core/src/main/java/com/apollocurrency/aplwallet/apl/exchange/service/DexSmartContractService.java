package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContractImpl;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.eth.web3j.ComparableStaticGasProvider;
import com.apollocurrency.aplwallet.apl.eth.web3j.DefaultRawTransactionManager;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.exchange.mapper.DepositedOrderDetailsMapper;
import com.apollocurrency.aplwallet.apl.exchange.mapper.SwapDataInfoMapper;
import com.apollocurrency.aplwallet.apl.exchange.mapper.UserEthDepositInfoMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DepositedOrderDetails;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.UserEthDepositInfo;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.ethereum.util.blockchain.EtherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.ChainId;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DexSmartContractService {
    private static final Logger LOG = LoggerFactory.getLogger(DexSmartContractService.class);

    private Web3j web3j;
    private String smartContractAddress;
    private String paxContractAddress;
    private KeyStoreService keyStoreService;
    private DexEthService dexEthService;
    private EthereumWalletService ethereumWalletService;
    private DexTransactionDao dexTransactionDao;
    private TransactionReceiptProcessor receiptProcessor;
    private ReceiptProcessorProducer receiptProcessorProducer;

    private static final String ACCOUNT_TO_READ_DATA = "1234";

    @Inject
    public DexSmartContractService(Web3j web3j, PropertiesHolder propertiesHolder, KeyStoreService keyStoreService, DexEthService dexEthService,
                                   EthereumWalletService ethereumWalletService, DexTransactionDao dexTransactionDao, TransactionReceiptProcessor receiptProcessor,
                                   ReceiptProcessorProducer receiptProcessorProducer) {
        this.web3j = web3j;
        this.keyStoreService = keyStoreService;
        this.smartContractAddress = propertiesHolder.getStringProperty("apl.eth.swap.proxy.contract.address");
        this.paxContractAddress   = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
        this.dexEthService = dexEthService;
        this.ethereumWalletService = ethereumWalletService;
        this.dexTransactionDao = dexTransactionDao;
        this.receiptProcessor = receiptProcessor;
        this.receiptProcessorProducer = receiptProcessorProducer;
    }

    /**
     *  Deposit(freeze) money(eth or pax) on the contract.
     * @param currency Eth or Pax
     * @return String transaction hash.
     */
    public String deposit(String passphrase, Long offerId, Long accountId, String fromAddress, BigInteger weiValue, Long gas, DexCurrency currency) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);
        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        if (currency.isPax()) {
            BigInteger allowance;
            try {
                allowance = ethereumWalletService.getAllowance(smartContractAddress, ethWalletKey.getCredentials().getAddress(), paxContractAddress);

                if (allowance.compareTo(weiValue) < 0) {
                    String approvedTx = ethereumWalletService.sendApproveTransaction(ethWalletKey, smartContractAddress, Constants.ETH_MAX_POS_INT);

                    if (approvedTx == null) {
                        log.error("Approved tx wasn't send for PAX. AccountId:{}, OrderIs:{}, FromAddress:{}", accountId, offerId, fromAddress);
                        throw new AplException.ExecutiveProcessException("Approved tx wasn't send for PAX. OrderIs: " + offerId);
                    }

                    receiptProcessorProducer.receiptProcessor().waitForTransactionReceipt(approvedTx);
                }
            } catch (IOException | TransactionException e) {
                throw new RuntimeException(e);
            }
        }

        return deposit(ethWalletKey.getCredentials(), offerId, weiValue, gasPrice, currency.isEth() ? null : paxContractAddress);
    }


        /**
         *  Withdraw money(eth or pax) from the contract.
         * @return String transaction hash.
         */
        public String withdraw(String passphrase, long accountId, String fromAddress, BigInteger orderId, Long gas) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        return withdraw(ethWalletKey.getCredentials(), orderId, gasPrice);
    }


    public String initiate(String passphrase, long accountId, String fromAddress, Long orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gas) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);
        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        return initiate(ethWalletKey.getCredentials(), new BigInteger(Long.toUnsignedString(orderId)), secretHash, recipient, refundTimestamp, gasPrice);
    }

    public String approve(String passphrase, byte[] secret, String fromAddress, long accountId) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        return approve(ethWalletKey.getCredentials(), secret, getEthGasPrice());

    }

    public String refund(byte[] secretHash, String passphrase, String fromAddress, long accountId, boolean waitConfirmation) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        String params = Numeric.toHexString(secretHash);
        String txHash = checkExistingTx(dexTransactionDao.get(params, fromAddress, DexTransaction.Op.REFUND), true);
        if (txHash == null) {
            ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EtherUtil.convert(getEthGasPrice(), EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
            DexContract dexContract = createDexContract(contractGasProvider, createDexTransaction(DexTransaction.Op.REFUND, params, fromAddress), ethWalletKey.getCredentials());
            txHash = dexContract.refund(secretHash, waitConfirmation);
        }
        return txHash;

    }

    public boolean hasFrozenMoney(DexOrder order) {
        if (order.getType() == OrderType.SELL) {
            return true;
        }
        try {
            List<UserEthDepositInfo> deposits = getUserFilledDeposits(order.getFromAddress());
            BigDecimal expectedFrozenAmount = EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate());
            for (UserEthDepositInfo deposit : deposits) {
                if (deposit.getOrderId().equals(order.getId()) && deposit.getAmount().compareTo(expectedFrozenAmount) == 0) {
                    return true;
                }
            }
        }
        catch (AplException.ExecutiveProcessException e) {
            log.warn("Unable to extract user deposits (possible cause - eth service is not available)", e);
        }
        return false;
    }


    public SwapDataInfo getSwapData(byte[] secretHash) throws AplException.ExecutiveProcessException {
        return getSwapData(Credentials.create(ACCOUNT_TO_READ_DATA), secretHash);
    }

    public SwapDataInfo getSwapData(Credentials credentials, byte[] secretHash) throws AplException.ExecutiveProcessException {
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, null);
        try {
            SwapDataInfo swapDataInfo = SwapDataInfoMapper.map(dexContract.getSwapData(secretHash).sendAsync().get());
            return swapDataInfo;
        } catch (Exception e){
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public String getHashForAtomicSwapTransaction(long orderId) throws NoSuchElementException, AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, web3j, Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            DexContract.InitiatedEventResponse response = dexContract.initiatedEventFlowable(orderId).toFuture().get();
            return response.log.getTransactionHash();
        } catch (InterruptedException | ExecutionException e) {
            throw new AplException.ExecutiveProcessException("Unable to retrieve hash from event.", e);
        }
    }


    public List<UserEthDepositInfo> getUserFilledDeposits(String user) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, web3j, Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            RemoteCall<Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>>> call = dexContract.getUserFilledDeposits(user);
            Tuple3<List<BigInteger>, List<BigInteger>, List<BigInteger>> callResponse = call.send();
            return new ArrayList<>(UserEthDepositInfoMapper.map(callResponse));
        } catch (Exception e) {
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public List<UserEthDepositInfo> getUserFilledOrders(String user) throws AplException.ExecutiveProcessException {
        List<UserEthDepositInfo> userDeposit = new ArrayList<>();
        DexContract dexContract = new DexContractImpl(smartContractAddress, web3j, Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            userDeposit.addAll(UserEthDepositInfoMapper.map(dexContract.getUserFilledOrders(user).sendAsync().get()));
            return userDeposit;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }


    public boolean isDepositForOrderExist(String userAddress, Long orderId) {
        DepositedOrderDetails depositedOrderDetails = getDepositedOrderDetails(userAddress, orderId);

        if (depositedOrderDetails == null || depositedOrderDetails.isWithdrawn() || depositedOrderDetails.getAmount().equals(BigInteger.ZERO)) {
            return false;
        }

        return true;
    }

    public boolean isUserTransferMoney(String user, Long orderId) throws AplException.ExecutiveProcessException {
        for (UserEthDepositInfo userInfo : getUserFilledOrders(user)) {
            if (userInfo.getOrderId().equals(orderId)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getEthUserAddresses(String passphrase, Long accountId) {
        WalletKeysInfo walletKeysInfo = keyStoreService.getWalletKeysInfo(passphrase, accountId);

        return walletKeysInfo.getEthWalletKeys().stream()
                .map(k -> k.getCredentials().getAddress())
                .collect(Collectors.toList());
    }

    private String approve(Credentials credentials, byte[] secret, Long gasPrice) {
        String params = Numeric.toHexString(secret);
        String txHash = checkExistingTx(dexTransactionDao.get(params, credentials.getAddress(), DexTransaction.Op.REDEEM));
        if (txHash == null) {
            ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
            DexContract dexContract = createDexContract(contractGasProvider, createDexTransaction(DexTransaction.Op.REDEEM,params, credentials.getAddress()) ,credentials);
            txHash = dexContract.redeem(secret);
        }

        return txHash;
    }

    private TransactionManager createTransactionManager(DexTransaction dexTransaction, Credentials credentials) {
        return new DefaultRawTransactionManager(web3j, credentials, ChainId.NONE, dexTransaction, dexTransactionDao);
    }

    /**
     *  Deposit some eth/erc20.
     * @param orderId  is Long but then will use as unsign value.
     * @param token
     * @return link on tx.
     */
    private String deposit(Credentials credentials, Long orderId, BigInteger weiValue, Long gasPrice, String token) {
        String params = orderId.toString(); // assume that order id is unique
        DexTransaction existingTx = dexTransactionDao.get(params, credentials.getAddress(), DexTransaction.Op.DEPOSIT);
        String txHash = checkExistingTx(existingTx);
        if (txHash == null) {
            ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
            BigInteger orderIdUnsign = new BigInteger(Long.toUnsignedString(orderId));
            DexTransaction dexTransaction = createDexTransaction(DexTransaction.Op.DEPOSIT, params, credentials.getAddress());
            DexContract dexContract = createDexContract(contractGasProvider, dexTransaction, credentials);
            try {
                if (token == null) {
                    txHash = dexContract.deposit(orderIdUnsign, weiValue);
                } else {
                    txHash = dexContract.deposit(orderIdUnsign, weiValue, token);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return txHash;
    }

    DexContract createDexContract(ContractGasProvider gasProvider, DexTransaction dexTransaction, Credentials credentials) {
       return new DexContractImpl(smartContractAddress, web3j, createTransactionManager(dexTransaction, credentials), gasProvider, ethereumWalletService);
    }

    private String checkExistingTx(DexTransaction tx) {
        return checkExistingTx(tx, false);
    }

    private String checkExistingTx(DexTransaction tx, boolean waitConfirmation) {
        String txHash = null;
        if (tx != null) {
            txHash = Numeric.toHexString(tx.getHash());
            try {
                Optional<Transaction> transaction = getTxByHash(txHash);
                if (transaction.isPresent()) { // pending or confirmed
                    if (transaction.get().getBlockNumberRaw() != null) { // confirmed
                        Optional<TransactionReceipt> receiptOptional = getTxReceipt(txHash);
                        if (receiptOptional.isPresent()) {
                            TransactionReceipt receipt = receiptOptional.get();
                            String status = receipt.getStatus();
                            if (Numeric.decodeQuantity(status).longValue() != 1) { // transaction was reverted
                                dexTransactionDao.delete(tx.getDbId());
                                txHash = null;
                            }
                        } else {
                            log.warn("Expected, that tx {} should be confirmed", txHash);
                        }
                    }
                } else {
                    sendRawTransaction(Numeric.toHexString(tx.getRawTransactionBytes()), waitConfirmation); // broadcast existing tx
                }
            } catch (IOException e) {
                log.error("Unable to broadcast tx or get receipt " + Numeric.toHexString(tx.getHash()), e);
            }
        }
        return txHash;
    }
    // set of methods to allow mocking of web3j interactions
    Optional<Transaction> getTxByHash(String hash) throws IOException {
        return  web3j.ethGetTransactionByHash(hash).send().getTransaction();
    }

    Optional<TransactionReceipt> getTxReceipt(String hash) throws IOException {
        return  web3j.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
    }

    String sendRawTransaction(String encodedTx, boolean waitConfirmation) throws IOException {
        EthSendTransaction response = web3j.ethSendRawTransaction(encodedTx).send();
        if (response != null) {
            if (response.hasError()) {
                throw new RuntimeException(response.getError().getMessage() + ", data - " + response.getError().getData() + ", tx: " + encodedTx);
            }
        } else {
            throw new RuntimeException("Unable to broadcast eth transaction, null response:  " + encodedTx);
        }
        String transactionHash = response.getTransactionHash();
        if (waitConfirmation) {
            try {
                TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(transactionHash);
                if (!transactionHash.equals(receipt.getTransactionHash())) {
                    throw new AplException.DEXProcessingException("Transaction with hash - " + transactionHash + " was mined with another hash" + receipt.getTransactionHash());
                }
            } catch (TransactionException e) {
                throw new AplException.DEXProcessingException("Unable to wait confirmation, hash - " + transactionHash);
            }
        }
        return transactionHash;
    }


    private String withdraw(Credentials credentials, BigInteger orderId, Long gasPrice) {
        ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        String txHash = checkExistingTx(dexTransactionDao.get(orderId.toString(), credentials.getAddress(), DexTransaction.Op.WITHDRAW));
        if (txHash == null) {
            DexTransaction dexTransaction = createDexTransaction(DexTransaction.Op.WITHDRAW, orderId.toString(), credentials.getAddress());
            DexContract dexContract = createDexContract(contractGasProvider, dexTransaction, credentials);
            txHash = dexContract.withdraw(orderId);
        }
        return txHash;
    }

    private DexTransaction createDexTransaction(DexTransaction.Op op, String params, String address) {
        return new DexTransaction(null, null, null, op, params, address, 0);
    }

    /**
     *  Initiate atomic swap.
     * @return link on tx.
     */
    private String initiate(Credentials credentials, BigInteger orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gasPrice) {
        ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        String txHash = checkExistingTx(dexTransactionDao.get(orderId.toString(), credentials.getAddress(), DexTransaction.Op.INITIATE));
        if (txHash == null) {
            DexContract dexContract = createDexContract(contractGasProvider,
                            createDexTransaction(DexTransaction.Op.INITIATE, orderId.toString(), credentials.getAddress())
                            , credentials);
            txHash = dexContract.initiate(orderId, secretHash, recipient, BigInteger.valueOf(refundTimestamp));
        }
        return txHash;
    }

    public DepositedOrderDetails getDepositedOrderDetails(String address, Long orderId) {
        TransactionManager transactionManager = new ClientTransactionManager(web3j, address);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, transactionManager, null, null);
        try {
            return DepositedOrderDetailsMapper.map(dexContract.getOrderDetails(new BigInteger(Long.toUnsignedString(orderId)), address).sendAsync().get());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private EthWalletKey getEthWalletKey(String passphrase, Long accountId, String fromAddress) throws AplException.ExecutiveProcessException {
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        if(keyStore==null){
            throw new AplException.ExecutiveProcessException("User wallet wasn't found.");
        }
        EthWalletKey ethWalletKey = keyStore.getEthWalletForAddress(fromAddress);
        if(ethWalletKey==null){
            throw new AplException.ExecutiveProcessException("Wallet's address wasn't found. " + fromAddress);
        }
        return ethWalletKey;
    }

    private Long getEthGasPrice() throws AplException.ExecutiveProcessException {
        Long gasPrice;
        try {
            gasPrice = dexEthService.getEthPriceInfo().getFastSpeedPrice();
        } catch (ExecutionException e) {
            LOG.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException("Third service is not available, try later.");
        }

        if(gasPrice == null){
            throw new AplException.ThirdServiceIsNotAvailable("Eth Price Info is not available.");
        }
        return gasPrice;
    }



}
