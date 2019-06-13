package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContract;
import com.apollocurrency.aplwallet.apl.eth.contracts.DexContractImpl;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import static com.apollocurrency.aplwallet.apl.util.Constants.ETH_DEFAULT_ADDRESS;

@Singleton
public class DexSmartContractService {
    private static final Logger LOG = LoggerFactory.getLogger(DexSmartContractService.class);

    private Web3j web3j;
    private String smartContractAddress;
    private String paxContractAddress;
    private KeyStoreService keyStoreService;
    private DexEthService dexEthService;
    private EthereumWalletService ethereumWalletService;

    @Inject
    public DexSmartContractService(Web3j web3j, PropertiesHolder propertiesHolder, KeyStoreService keyStoreService, DexEthService dexEthService,
                                   EthereumWalletService ethereumWalletService) {
        this.web3j = web3j;
        this.keyStoreService = keyStoreService;
        smartContractAddress = propertiesHolder.getStringProperty("apl.eth.smart.contract.address");
        paxContractAddress = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
        this.dexEthService = dexEthService;
        this.ethereumWalletService = ethereumWalletService;
    }

    /**
     *  Deposit(freeze) money(eth or pax) on the contract.
     * @param currency Eth or Pax
     * @return String transaction hash.
     */
    public String deposit(String passphrase, long accountId, String fromAddress, BigInteger weiValue, Long gas, DexCurrencies currency) throws ExecutionException, AplException.ExecutiveProcessException {
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWalletKey = keyStore.getEthWalletForAddress(fromAddress);
        Long gasPrice = gas;

        if(gasPrice == null){
            gasPrice = dexEthService.getEthPriceInfo().getFastSpeedPrice();
        }

       if(gasPrice == null){
           throw new AplException.ThirdServiceIsNotAvailable("Eth Price Info is not available.");
       }
        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        if(currency.isPax()){
            ethereumWalletService.sendApproveTransaction(passphrase, accountId, fromAddress, smartContractAddress, weiValue);
        }

        return deposit(ethWalletKey.getCredentials(), weiValue, gasPrice, currency.isEth() ? null : paxContractAddress);
    }


    /**
     *  Withdraw money(eth or pax) from the contract.
     * @param currency Eth or Pax
     * @return String transaction hash.
     */
    public String withdraw(String passphrase, long accountId, String fromAddress,  BigInteger weiValue, Long gas, DexCurrencies currency) throws AplException.ExecutiveProcessException {
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWalletKey = keyStore.getEthWalletForAddress(fromAddress);

        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }
        Long gasPrice = gas;

        if(gasPrice == null){
            try {
                gasPrice = dexEthService.getEthPriceInfo().getAverageSpeedPrice();
            } catch (ExecutionException e) {
                throw new AplException.ExecutiveProcessException("Third service is not available, try later.");
            }
        }

        return withdraw(ethWalletKey.getCredentials(), weiValue, gasPrice, currency.isEth() ? null : paxContractAddress);
    }

    /**
     * Get information how much money frozen a particular address.
     * @return Amount of the froze money on the contract for the address.
     */
    public BigDecimal getUserInfoEth(String address, DexCurrencies currency){
        if(!currency.isEthOrPax()){
            throw new UnsupportedOperationException("This function not supported this currency " + currency.name());
        }

        return EthUtil.weiToEther(getUserAssetAmount(address, currency.isEth() ? ETH_DEFAULT_ADDRESS : paxContractAddress));
    }



    private String deposit(Credentials credentials, BigInteger weiValue, Long gasPrice, String token){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ERC20);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            if(token==null) {
                transactionReceipt = dexContract.depositAsset(weiValue).sendAsync().get();
            } else {
                transactionReceipt = dexContract.depositAsset(weiValue, token).sendAsync().get();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    private String withdraw(Credentials credentials, BigInteger weiValue, Long gasPrice, String token){
        ContractGasProvider contractGasProvider = new StaticGasProvider(EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI), Constants.GAS_LIMIT_FOR_ERC20);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, credentials, contractGasProvider);
        TransactionReceipt transactionReceipt = null;
        try {
            if(token==null) {
                transactionReceipt = dexContract.withdrawAsset(weiValue).sendAsync().get();
            } else {
                transactionReceipt = dexContract.withdrawAsset(weiValue, token).sendAsync().get();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return transactionReceipt != null ? transactionReceipt.getTransactionHash() : null;
    }

    private BigInteger getUserAssetAmount(String address, String asset){
        TransactionManager transactionManager = new ClientTransactionManager(web3j, address);
        DexContract  dexContract = new DexContractImpl(smartContractAddress, web3j, transactionManager, null);
        try {
            return dexContract.getUserAssetAmount(address, asset).sendAsync().get();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }



}
