package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContractImpl;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.exchange.mapper.SwapDataInfoMapper;
import com.apollocurrency.aplwallet.apl.exchange.mapper.UserEthDepositInfoMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private static final String ACCOUNT_TO_READ_DATA = "1234";

    @Inject
    public DexSmartContractService(Web3j web3j, PropertiesHolder propertiesHolder, KeyStoreService keyStoreService, DexEthService dexEthService,
                                   EthereumWalletService ethereumWalletService) {
        this.web3j = web3j;
        this.keyStoreService = keyStoreService;
        this.smartContractAddress = propertiesHolder.getStringProperty("apl.eth.swap.contract.address");
        this.paxContractAddress = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
        this.dexEthService = dexEthService;
        this.ethereumWalletService = ethereumWalletService;
    }

    /**
     *  Deposit(freeze) money(eth or pax) on the contract.
     * @param currency Eth or Pax
     * @return String transaction hash.
     */
    public String deposit(String passphrase, Long offerId, Long accountId, String fromAddress, BigInteger weiValue, Long gas, DexCurrencies currency) throws ExecutionException, AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        if(currency.isPax()){
            ethereumWalletService.sendApproveTransaction(ethWalletKey, smartContractAddress, weiValue);
        }

        return deposit(ethWalletKey.getCredentials(), offerId, weiValue, gasPrice, currency.isEth() ? null : paxContractAddress);
    }


        /**
         *  Withdraw money(eth or pax) from the contract.
         * @param currency Eth or Pax
         * @return String transaction hash.
         */
    public String withdraw(String passphrase, long accountId, String fromAddress,  BigInteger orderId, Long gas, DexCurrencies currency) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        return withdraw(ethWalletKey.getCredentials(), orderId, gasPrice, currency.isEth() ? null : paxContractAddress);
    }


    public String initiate(String passphrase, long accountId, String fromAddress, Long orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gas) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);
        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        return initiate(ethWalletKey.getCredentials(), new BigInteger(Long.toUnsignedString(orderId)), secretHash, recipient, refundTimestamp, gasPrice);
    }

    public String depositAndInitiate(String passphrase, long accountId, String fromAddress, Long orderId, BigInteger weiValue, byte[] secretHash, String recipient, Integer refundTimestamp, Long gas,  String token) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);
        Long gasPrice = gas;
        if(gasPrice == null){
            gasPrice = getEthGasPrice();
        }

        return depositAndInitiate(ethWalletKey.getCredentials(), new BigInteger(Long.toUnsignedString(orderId)), weiValue, secretHash, recipient, refundTimestamp,gasPrice, token);
    }

    public boolean approve(String passphrase, byte[] secret, String fromAddress, long accountId) throws AplException.ExecutiveProcessException {
        EthWalletKey ethWalletKey = getEthWalletKey(passphrase, accountId, fromAddress);

        boolean isApproved = approve(ethWalletKey.getCredentials(), secret, getEthGasPrice());

        return isApproved;
    }

    public SwapDataInfo getSwapData(byte[] secretKey) throws AplException.ExecutiveProcessException {
        return getSwapData(Credentials.create(ACCOUNT_TO_READ_DATA), secretKey);
    }

    public SwapDataInfo getSwapData(Credentials credentials, byte[] secretKey) throws AplException.ExecutiveProcessException {
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, null);
        try {
            SwapDataInfo swapDataInfo = SwapDataInfoMapper.map(dexContract.getSwapData(secretKey).sendAsync().get());
            return swapDataInfo;
        } catch (Exception e){
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage());
        }
    }


    public List<UserEthDepositInfo> getUserFilledDeposits(String user) throws AplException.ExecutiveProcessException {
        DexContract dexContract = new DexContractImpl(smartContractAddress, web3j, Credentials.create(ACCOUNT_TO_READ_DATA), null);
        try {
            List<UserEthDepositInfo> userDeposit = new ArrayList<>(UserEthDepositInfoMapper.map(dexContract.getUserFilledDeposits(user).sendAsync().get()));
            return userDeposit;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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


    public boolean isDepositForOrderExist(String user, Long orderId) throws AplException.ExecutiveProcessException {
        for (UserEthDepositInfo userFilledDeposit : getUserFilledDeposits(user)) {
            if (userFilledDeposit.getOrderId().equals(orderId)) {
                return true;
            }
        }
        return false;
    }

    private boolean approve(Credentials credentials, byte[] secret, Long gasPrice){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);

        try {
            dexContract.redeem(secret).sendAsync().get();
        } catch (Exception e){
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     *  Deposit some eth/erc20.
     * @param orderId  is Long but then will use as unsign value.
     * @param token
     * @return link on tx.
     */
    private String deposit(Credentials credentials, Long orderId, BigInteger weiValue, Long gasPrice, String token){
        BigInteger orderIdUnsign = new BigInteger(Long.toUnsignedString(orderId));
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            if(token==null) {
                transactionReceipt = dexContract.deposit(orderIdUnsign, weiValue).sendAsync().get();
            } else {
                transactionReceipt = dexContract.deposit(orderIdUnsign, weiValue, token).sendAsync().get();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    /**
     *  Deposit some eth/erc20.
     * @param orderId  is Long but then will use as unsign value.
     * @param token
     * @return link on tx.
     */
    private String depositAndInitiate(Credentials credentials, BigInteger orderId, BigInteger weiValue, byte[] secretHash, String recipient, Integer refundTimestamp, Long gasPrice,  String token){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            if(token==null) {
                transactionReceipt = dexContract.depositAndInitiate(orderId, secretHash, recipient, BigInteger.valueOf(refundTimestamp), weiValue).sendAsync().get();
            } else {
                transactionReceipt = dexContract.depositAndInitiate(orderId, weiValue, token, secretHash, recipient, BigInteger.valueOf(refundTimestamp)).sendAsync().get();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    private String withdraw(Credentials credentials, BigInteger orderId, Long gasPrice, String token){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            if(token==null) {
                log.debug("dexContract withdraw, order: {}", orderId);
                transactionReceipt = dexContract.withdraw(orderId).sendAsync().get();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    /**
     *  Initiate atomic swap.
     * @return link on tx.
     */
    private String initiate(Credentials credentials, BigInteger orderId, byte[] secretHash, String recipient, Integer refundTimestamp, Long gasPrice){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ETH_ATOMIC_SWAP_CONTRACT);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = dexContract.initiate(orderId, secretHash, recipient, BigInteger.valueOf(refundTimestamp)).sendAsync().get();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    private BigInteger getDepositedOrderDetails(String address, BigInteger orderId){
        TransactionManager transactionManager = new ClientTransactionManager(web3j, address);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, transactionManager, null);
        try {
            dexContract.getDepositedOrderDetails(orderId, address).sendAsync().get();
            //TODO Process it
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
        if(keyStore==null){
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
