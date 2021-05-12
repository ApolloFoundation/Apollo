package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.dex.core.dao.DexTransactionDao;
import com.apollocurrency.aplwallet.apl.dex.core.exception.NotValidTransactionException;
import com.apollocurrency.aplwallet.apl.dex.core.mapper.DepositedOrderDetailsMapper;
import com.apollocurrency.aplwallet.apl.dex.core.mapper.ExpiredSwapMapper;
import com.apollocurrency.aplwallet.apl.dex.core.mapper.SwapDataInfoMapper;
import com.apollocurrency.aplwallet.apl.dex.core.mapper.UserAddressesMapper;
import com.apollocurrency.aplwallet.apl.dex.core.mapper.UserEthDepositInfoMapper;
import com.apollocurrency.aplwallet.apl.dex.core.model.DepositedOrderDetails;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexTransaction;
import com.apollocurrency.aplwallet.apl.dex.core.model.ExpiredSwap;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.dex.core.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.dex.core.model.UserAddressesWithOffset;
import com.apollocurrency.aplwallet.apl.dex.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.dex.eth.contracts.DexContractImpl;
import com.apollocurrency.aplwallet.apl.dex.eth.model.EthDepositsWithOffset;
import com.apollocurrency.aplwallet.apl.dex.eth.service.DexBeanProducer;
import com.apollocurrency.aplwallet.apl.dex.eth.service.DexEthService;
import com.apollocurrency.aplwallet.apl.dex.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.dex.eth.web3j.ChainId;
import com.apollocurrency.aplwallet.apl.dex.eth.web3j.ComparableStaticGasProvider;
import com.apollocurrency.aplwallet.apl.dex.eth.web3j.DefaultRawTransactionManager;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.service.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.service.KMSService;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
@Singleton
public class DexSmartContractService {
    private static final String SERVICE_NAME = "SmartContractAdapter";
    private static final String LOCK_MAP_CLEANER_NAME = "SCLockMapCleaner";
    private static final int LOCK_MAP_CLEANER_DELAY = 60 * 1000; // 1 min in ms
    private static final String ACCOUNT_TO_READ_DATA = "1234";

    private final String smartContractAddress;
    private final String paxContractAddress;
    private final DexEthService dexEthService;
    private final EthereumWalletService ethereumWalletService;
    private final DexTransactionDao dexTransactionDao;
    private final DexBeanProducer dexBeanProducer;
    private final TaskDispatchManager taskManager;
    private final KMSService KMSService;
    private final ChainId chainId;

    private Map<String, Object> idLocks = Collections.synchronizedMap(new HashMap<>());
    private Set<String> locksToRemoveLater = ConcurrentHashMap.newKeySet();

    @Inject
    public DexSmartContractService(PropertiesHolder propertiesHolder, DexEthService dexEthService,
                                   EthereumWalletService ethereumWalletService, DexTransactionDao dexTransactionDao,
                                   DexBeanProducer dexBeanProducer, TaskDispatchManager taskDispatchManager, KMSService KMSService, ChainId chainId) {
        this.smartContractAddress = propertiesHolder.getStringProperty("apl.eth.swap.proxy.contract.address");
        this.paxContractAddress = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
        this.dexEthService = dexEthService;
        this.ethereumWalletService = ethereumWalletService;
        this.dexTransactionDao = dexTransactionDao;
        this.taskManager = taskDispatchManager;
        this.dexBeanProducer = dexBeanProducer;
        this.KMSService = KMSService;
        this.chainId = chainId;
    }

    @PostConstruct
    public void init() {
        TaskDispatcher taskDispatcher = taskManager.newScheduledDispatcher(SERVICE_NAME);
        taskDispatcher.schedule(Task.builder()
            .name(LOCK_MAP_CLEANER_NAME)
            .task(this::removeLocks)
            .delay(LOCK_MAP_CLEANER_DELAY)
            .initialDelay(LOCK_MAP_CLEANER_DELAY)
            .build());
    }

    void removeLocks() {
        Iterator<String> iterator = locksToRemoveLater.iterator();
        while (iterator.hasNext()) {
            String id = iterator.next();
            idLocks.remove(id);
            iterator.remove();
        }
    }

    /**
     * Deposit(freeze) money(eth or pax) on the contract.
     *
     * @param currency Eth or Pax
     * @return String transaction hash.
     */
    public String deposit(String passphrase, Long offerId, Long accountId, String fromAddress, BigInteger weiValue, Long gas, DexCurrency currency) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);
        Long gasPrice = gas;
        if (gasPrice == null) {
            gasPrice = getEthGasPrice();
        }

        if (!currency.isEthOrPax()) {
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        if (currency.isPax()) {
            BigInteger allowance;
            try {
                allowance = ethereumWalletService.getAllowance(smartContractAddress, ethWalletKey.getCredentials().getAddress(), paxContractAddress);

                if (allowance.compareTo(weiValue) < 0) {
                    String approvedTx = ethereumWalletService.sendApproveTransaction(ethWalletKey.getCredentials(), smartContractAddress, Constants.ETH_MAX_POS_INT);

                    if (approvedTx == null) {
                        log.error("Approved tx wasn't send for PAX. AccountId:{}, OrderIs:{}, FromAddress:{}", accountId, offerId, fromAddress);
                        throw new AplException.ExecutiveProcessException("Approved tx wasn't send for PAX. OrderIs: " + offerId);
                    }

                    dexBeanProducer.receiptProcessor().waitForTransactionReceipt(approvedTx);
                }
            } catch (IOException | TransactionException e) {
                throw new RuntimeException(e);
            }
        }

        return deposit(ethWalletKey.getCredentials(), offerId, weiValue, gasPrice, currency.isEth() ? null : paxContractAddress);
    }


    /**
     * Withdraw money(eth or pax) from the contract.
     *
     * @return String transaction hash.
     */
    public String withdraw(String passphrase, long accountId, String fromAddress, BigInteger orderId, Long gas) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);

        Long gasPrice = gas;
        if (gasPrice == null) {
            gasPrice = getEthGasPrice();
        }

        return withdraw(ethWalletKey.getCredentials(), orderId, gasPrice);
    }


    public String initiate(String passphrase, long accountId, String fromAddress, Long orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gas) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);
        Long gasPrice = gas;
        if (gasPrice == null) {
            gasPrice = getEthGasPrice();
        }

        return initiate(ethWalletKey.getCredentials(), new BigInteger(Long.toUnsignedString(orderId)), secretHash, recipient, refundTimestamp, gasPrice);
    }

    public String approve(String passphrase, byte[] secret, String fromAddress, long accountId) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);

        return approve(ethWalletKey.getCredentials(), secret, getEthGasPrice());

    }

    public String refund(byte[] secretHash, String passphrase, String fromAddress, long accountId, boolean waitConfirmation) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);

        String params = Numeric.toHexString(secretHash);
        String identifier = fromAddress + params + DexTransaction.Op.REFUND;
        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            String txHash = checkExistingTx(dexTransactionDao.get(params, fromAddress, DexTransaction.Op.REFUND), waitConfirmation);
            if (txHash == null) {
                ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(getEthGasPrice()), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
                DexContract dexContract = createDexContract(contractGasProvider, createDexTransaction(DexTransaction.Op.REFUND, params, fromAddress), ethWalletKey.getCredentials());
                txHash = dexContract.refund(secretHash, waitConfirmation);
            }
            locksToRemoveLater.add(identifier);
            return txHash;
        }

    }

    public String refundAndWithdraw(byte[] secretHash, String passphrase, String fromAddress, long accountId, boolean waitConfirmation) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = KMSService.getEthWallet(accountId, passphrase, fromAddress);

        String params = Numeric.toHexString(secretHash);
        String identifier = fromAddress + params + DexTransaction.Op.REFUND;
        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            String txHash = checkExistingTx(dexTransactionDao.get(params, fromAddress, DexTransaction.Op.REFUND), waitConfirmation);
            if (txHash == null) {
                ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(getEthGasPrice()), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
                DexContract dexContract = createDexContract(contractGasProvider, createDexTransaction(DexTransaction.Op.REFUND, params, fromAddress), ethWalletKey.getCredentials());
                txHash = dexContract.refundAndWithdraw(secretHash, waitConfirmation);
            }
            locksToRemoveLater.add(identifier);
            return txHash;
        }
    }

    public boolean hasFrozenMoney(DexOrder order) {
        if (order.getType() == OrderType.SELL) {
            return true;
        }
        return isDepositForOrderExist(order.getFromAddress(), order.getId(), EthUtil.atmToEth(order.getOrderAmount()).multiply(order.getPairRate()));
    }


    public SwapDataInfo getSwapData(byte[] secretHash) throws AplException.ExecutiveProcessException {
        return getSwapData(Credentials.create(ACCOUNT_TO_READ_DATA), secretHash);
    }

    public SwapDataInfo getSwapData(Credentials credentials, byte[] secretHash) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), credentials, null);
        try {
            SwapDataInfo swapDataInfo = SwapDataInfoMapper.map(dexContract.getSwapData(secretHash).sendAsync().get());
            return swapDataInfo;
        } catch (Exception e) {
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public String getHashForAtomicSwapTransaction(long orderId) throws NoSuchElementException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), Credentials.create(ACCOUNT_TO_READ_DATA), null);
        return dexContract.initiatedEventFlowable(orderId).toObservable().blockingFirst().log.getTransactionHash();
    }


    public EthDepositsWithOffset getUserActiveDeposits(String user, long offset, long limit) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            RemoteCall<Tuple4<List<BigInteger>, List<BigInteger>, List<BigInteger>, BigInteger>> call = dexContract.getUserActiveDeposits(user, offset, limit);
            Tuple4<List<BigInteger>, List<BigInteger>, List<BigInteger>, BigInteger> callResponse = call.send();
            return UserEthDepositInfoMapper.map(callResponse);
        } catch (Exception e) {
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public EthDepositsWithOffset getUserFilledOrders(String user, long offset, long limit) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            return UserEthDepositInfoMapper.map(dexContract.getUserFilledOrders(user, offset, limit).sendAsync().get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public List<ExpiredSwap> getExpiredSwaps(String user) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            return ExpiredSwapMapper.map(dexContract.getExpiredSwaps(user).sendAsync().get());
        } catch (Exception e) {
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }

    public UserAddressesWithOffset getUserAddresses(long offset, long limit) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            return UserAddressesMapper.map(dexContract.getUsersList(offset, limit).sendAsync().get());
        } catch (Exception e) {
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }


    public boolean isDepositForOrderExist(String userAddress, Long orderId) {
        DepositedOrderDetails depositedOrderDetails = getDepositedOrderDetails(userAddress, orderId);

        return depositedOrderDetails != null && !depositedOrderDetails.isWithdrawn() && !depositedOrderDetails.getAmount().equals(BigDecimal.ZERO);
    }

    public boolean isDepositForOrderExist(String userAddress, Long orderId, BigDecimal amountEth) {
        DepositedOrderDetails depositedOrderDetails = getDepositedOrderDetails(userAddress, orderId);

        return depositedOrderDetails != null && depositedOrderDetails.isCreated() && !depositedOrderDetails.isWithdrawn() && depositedOrderDetails.getAmount().compareTo(amountEth) == 0;
    }

    private String approve(Credentials credentials, byte[] secret, Long gasPrice) {
        String params = Numeric.toHexString(secret);
        String identifier = credentials.getAddress() + params + DexTransaction.Op.REDEEM;
        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            String txHash = checkExistingTx(dexTransactionDao.get(params, credentials.getAddress(), DexTransaction.Op.REDEEM));
            if (txHash == null) {
                ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(gasPrice), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
                DexContract dexContract = createDexContract(contractGasProvider, createDexTransaction(DexTransaction.Op.REDEEM, params, credentials.getAddress()), credentials);
                txHash = dexContract.redeem(secret);
            }
            locksToRemoveLater.add(identifier);

            return txHash;
        }
    }

    private TransactionManager createTransactionManager(DexTransaction dexTransaction, Credentials credentials) {
        return new DefaultRawTransactionManager(dexBeanProducer.web3j(), credentials, chainId.getValid(), dexTransaction, dexTransactionDao);
    }

    /**
     * Deposit some eth/erc20.
     *
     * @param orderId is Long but then will use as unsign value.
     * @param token
     * @return link on tx.
     */
    private String deposit(Credentials credentials, Long orderId, BigInteger weiValue, Long gasPrice, String token) {
        String params = orderId.toString(); // assume that order id is unique
        String identifier = credentials.getAddress() + orderId.toString() + DexTransaction.Op.DEPOSIT;
        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            DexTransaction existingTx = dexTransactionDao.get(params, credentials.getAddress(), DexTransaction.Op.DEPOSIT);
            String txHash = checkExistingTx(existingTx);
            if (txHash == null) {
                ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(gasPrice), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
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
                    log.error(e.getMessage(), e);
                }
            }
            locksToRemoveLater.add(identifier);
            return txHash;
        }
    }

    DexContract createDexContract(ContractGasProvider gasProvider, DexTransaction dexTransaction, Credentials credentials) {
        return new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), createTransactionManager(dexTransaction, credentials), gasProvider, ethereumWalletService);
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
                                log.info("Delete saved reverted transaction. New one will be sent. Hash {}", txHash);
                                dexTransactionDao.delete(tx.getDbId());
                                txHash = null;
                            }
                        } else {
                            log.warn("Expected, that tx {} should be confirmed", txHash);
                        }
                    } else {
                        log.debug("Tx {} is unconfirmed, skip sending ", txHash);
                    }
                } else {
                    try {
                        sendRawTransaction(Numeric.toHexString(tx.getRawTransactionBytes()), waitConfirmation); // broadcast existing tx
                    } catch (NotValidTransactionException e) {
                        log.info("Stored previous transaction is incorrect, removing... New will be sent.", e);
                        dexTransactionDao.delete(tx.getDbId());
                        txHash = null;
                    }
                }
            } catch (IOException e) {
                log.error("Unable to broadcast tx or get receipt " + Numeric.toHexString(tx.getHash()), e);
            }
        }
        return txHash;
    }

    // set of methods to allow mocking of web3j interactions
    Optional<Transaction> getTxByHash(String hash) throws IOException {
        return dexBeanProducer.web3j().ethGetTransactionByHash(hash).send().getTransaction();
    }

    Optional<TransactionReceipt> getTxReceipt(String hash) throws IOException {
        return dexBeanProducer.web3j().ethGetTransactionReceipt(hash).send().getTransactionReceipt();
    }

    String sendRawTransaction(String encodedTx, boolean waitConfirmation) throws IOException, NotValidTransactionException {
        EthSendTransaction response = dexBeanProducer.web3j().ethSendRawTransaction(encodedTx).send();
        if (response != null) {
            if (response.hasError()) {
                throw new NotValidTransactionException(response.getError().getMessage() + ", data - " + response.getError().getData() + ", tx: " + encodedTx);
            }
        } else {
            throw new RuntimeException("Unable to broadcast eth transaction, null response:  " + encodedTx);
        }
        String transactionHash = response.getTransactionHash();
        if (waitConfirmation) {
            try {
                TransactionReceipt receipt = dexBeanProducer.receiptProcessor().waitForTransactionReceipt(transactionHash);
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
        ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(gasPrice), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        String identifier = credentials.getAddress() + orderId.toString() + DexTransaction.Op.WITHDRAW;

        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            String txHash = checkExistingTx(dexTransactionDao.get(orderId.toString(), credentials.getAddress(), DexTransaction.Op.WITHDRAW));
            if (txHash == null) {
                DexTransaction dexTransaction = createDexTransaction(DexTransaction.Op.WITHDRAW, orderId.toString(), credentials.getAddress());
                DexContract dexContract = createDexContract(contractGasProvider, dexTransaction, credentials);
                txHash = dexContract.withdraw(orderId);
            }
            locksToRemoveLater.add(identifier);
            return txHash;
        }
    }

    private DexTransaction createDexTransaction(DexTransaction.Op op, String params, String address) {
        return new DexTransaction(null, null, null, op, params, address, 0);
    }

    /**
     * Initiate atomic swap.
     *
     * @return link on tx.
     */
    private String initiate(Credentials credentials, BigInteger orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gasPrice) {
        ContractGasProvider contractGasProvider = new ComparableStaticGasProvider(EthUtil.gweiToWei(gasPrice), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        String identifier = credentials.getAddress() + Numeric.toHexString(secretHash) + DexTransaction.Op.INITIATE;
        synchronized (idLocks.compute(identifier, (k, v) -> v == null ? new Object() : v)) {
            String txHash = checkExistingTx(dexTransactionDao.get(orderId.toString(), credentials.getAddress(), DexTransaction.Op.INITIATE));
            if (txHash == null) {
                DexContract dexContract = createDexContract(contractGasProvider,
                    createDexTransaction(DexTransaction.Op.INITIATE, orderId.toString(), credentials.getAddress())
                    , credentials);
                txHash = dexContract.initiate(orderId, secretHash, recipient, BigInteger.valueOf(refundTimestamp));
            }
            locksToRemoveLater.add(identifier);
            return txHash;
        }
    }

    public DepositedOrderDetails getDepositedOrderDetails(String address, Long orderId) {
        TransactionManager transactionManager = new ClientTransactionManager(dexBeanProducer.web3j(), address);
        DexContract dexContract = new DexContractImpl(smartContractAddress, dexBeanProducer.web3j(), transactionManager, null, null);
        try {
            return DepositedOrderDetailsMapper.map(dexContract.getOrderDetails(new BigInteger(Long.toUnsignedString(orderId)), address).sendAsync().get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private Long getEthGasPrice() throws AplException.ExecutiveProcessException {
        Long gasPrice;
        try {
            gasPrice = dexEthService.getEthPriceInfo().getFastSpeedPrice();
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException("Third service is not available, try later.");
        }

        if (gasPrice == null) {
            throw new AplException.ThirdServiceIsNotAvailable("Eth Price Info is not available.");
        }
        return gasPrice;
    }
}
