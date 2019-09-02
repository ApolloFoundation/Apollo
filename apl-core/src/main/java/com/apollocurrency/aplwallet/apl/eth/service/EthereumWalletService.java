package com.apollocurrency.aplwallet.apl.eth.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.ethereum.util.blockchain.EtherUtil;
import org.slf4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class EthereumWalletService {
    private static final Logger log = getLogger(EthereumWalletService.class);

    private Web3j web3j;
    private KeyStoreService keyStoreService;
    private DexEthService dexEthService;

    public String PAX_CONTRACT_ADDRESS;

    @Inject
    public EthereumWalletService(Web3j web3j, PropertiesHolder propertiesHolder, KeyStoreService keyStoreService, DexEthService dexEthService) {
        this.web3j = web3j;
        this.keyStoreService = keyStoreService;
        this.dexEthService = dexEthService;

        this.PAX_CONTRACT_ADDRESS = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
    }

    /**
     * Get balances for Eth/tokens.
     * @param address Eth address
     * @return account balance in Wei
     */
    public EthWalletBalanceInfo balanceInfo(String address){
        Objects.requireNonNull(address);
        EthWalletBalanceInfo ethWalletBalanceInfo = new EthWalletBalanceInfo(address);

        ethWalletBalanceInfo.put(DexCurrencies.ETH.getCurrencyCode(), getEthBalanceWei(address));
        ethWalletBalanceInfo.put(DexCurrencies.PAX.getCurrencyCode(), getPaxBalanceWei(address));

        return ethWalletBalanceInfo;
    }

    /**
     * Get Eth / PAX token balance.
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getBalanceWei(String address, DexCurrencies dexCurrencies){
        if(!dexCurrencies.isEthOrPax()){
            throw new UnsupportedOperationException("This currency is not supported");
        }

        if(dexCurrencies.isEth()){
            return getEthBalanceWei(address);
        } else if(dexCurrencies.isPax()){
            return getPaxBalanceWei(address);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get PAX token balance.
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getPaxBalanceWei(String address){
        Objects.requireNonNull(address);
        return getTokenBalance(PAX_CONTRACT_ADDRESS, address);
    }

    /**
     * Get Eth balance.
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getEthBalanceWei(String address) {
        Objects.requireNonNull(address);
        BigInteger wei = null;
        try {
            EthGetBalance ethGetBalance = web3j
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send();

            wei = ethGetBalance.getBalance();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return wei;
    }

    /**
     * Transfer ETH or PAX money from account to another one.
     * @param amountEth
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    public String transfer(String passphrase, long accountId, String fromAddress, String toAddress, BigDecimal amountEth, Long gasPrice, DexCurrencies currencies) throws AplException.ExecutiveProcessException {
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWalletKey = keyStore.getEthWalletForAddress(fromAddress);

        if(ethWalletKey == null){
            throw new AplException.ExecutiveProcessException("Not found eth address at the user storage: " + fromAddress);
        }

        if (DexCurrencies.ETH.equals(currencies)) {
            return transferEth(ethWalletKey.getCredentials(), toAddress, EthUtil.etherToWei(amountEth), gasPrice);
        } else if (DexCurrencies.PAX.equals(currencies)) {
            return transferERC20(PAX_CONTRACT_ADDRESS, ethWalletKey.getCredentials(), toAddress, EthUtil.etherToWei(amountEth), gasPrice);
        } else {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + currencies.getCurrencyCode());
        }
    }

    public String sendApproveTransaction(EthWalletKey ethWalletKey, String spenderAddress, BigInteger value) throws AplException.ExecutiveProcessException {
        EthGasInfo ethGasInfo;
        try {
            ethGasInfo = dexEthService.getEthPriceInfo();
        } catch (ExecutionException e) {
            throw new AplException.ExecutiveProcessException("Third service is not available.");
        }

        return sendApproveTransaction(ethWalletKey.getCredentials(), spenderAddress, value,  ethGasInfo.getAverageSpeedPrice());
    }

    /**
     * Send Approve Transaction
     * @param spenderAddress sender address
     * @param value amount
     * @return tx transaction id
     */
    private String sendApproveTransaction(Credentials credentials, String spenderAddress, BigInteger value, Long gasPrice){
        String tx = null;
        try {
            Function function = approve(spenderAddress, value);
            tx = execute(credentials, function, PAX_CONTRACT_ADDRESS, gasPrice);
        } catch (Exception e){
            log.error(e.getMessage());
        }

        return tx;
    }


    /**
     * Get Eth ERC-20 balance.
     * @param contractAddress ERC-20 address
     * @param address Eth address
     * @return account balance in Wei
     */
    private BigInteger getTokenBalance(String contractAddress, String address){
        BigInteger balance = null;
        try {
            Function function = balanceOf(address);
            String responseValue = callSmartContractFunction(function, contractAddress, address);

            List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

            balance = (BigInteger) response.get(0).getValue();
        } catch (Exception e){
            log.error(e.getMessage());
        }

        return balance;
    }

    private BigInteger getTokenTotalSupply(String contractAddress, String fromAddress) throws IOException {
        Function function = totalSupply();
        String responseValue = callSmartContractFunction(function, contractAddress, fromAddress);

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

        return (BigInteger) response.get(0).getValue();
    }

    private String callSmartContractFunction(Function function, String contractAddress, String fromAddress) throws IOException {
        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(fromAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .send();

        return response.getValue();
    }


    /**
     * Transfer money from account to another one.
     * @param amountWei
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    public String transferEth(Credentials credentials, String toAddress, BigInteger amountWei, Long gasPrice) throws AplException.ExecutiveProcessException {
        // step 1: get the nonce (tx count for sending address)
        BigInteger nonce;
        try {
            nonce = getNonce(credentials.getAddress());
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage(),e);
        }

        log.info("Nonce for sending address (coinbase): " + nonce);

        RawTransaction rawTransaction  = RawTransaction
                .createEtherTransaction(
                        nonce,
                        EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI),
                        Constants.GAS_LIMIT_ETHER_TX,
                        toAddress,
                        amountWei
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage(),e);
        }
        String transactionHash = ethSendTransaction.getTransactionHash();

        log.info("Tx hash: " + transactionHash);

        return transactionHash;
    }

    /**
     * Transfer ERC20 tokens from account to another one.
     * @param amountWei
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    private String transferERC20(String erc20Address, Credentials recipientCredentials, String toAddress, BigInteger amountWei, Long gasPrice) throws AplException.ExecutiveProcessException {
        Function function = transfer(toAddress, amountWei);
        String transactionHash;
        try {
            transactionHash = execute(recipientCredentials, function, erc20Address, gasPrice);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new AplException.ExecutiveProcessException(e.getMessage(),e);
        }

        return transactionHash;
    }

    /**
     * Returns the TransactionRecipt for the specified tx hash as an optional.
     */
    public Optional<TransactionReceipt> getReceipt(String transactionHash) throws AplException.ExecutiveProcessException {
        EthGetTransactionReceipt receipt;
        try {
            receipt = web3j
                    .ethGetTransactionReceipt(transactionHash)
                    .sendAsync()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage(),e);
        }

        return receipt.getTransactionReceipt();
    }

    private String execute(Credentials credentials, Function function, String contractToAddress, Long gasPrice) throws ExecutionException, InterruptedException {
        BigInteger nonce = getNonce(credentials.getAddress());

        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI),
                Constants.GAS_LIMIT_FOR_ERC20,
                contractToAddress,
                encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

        return transactionResponse.getTransactionHash();
    }

    private BigInteger getNonce(String address) throws ExecutionException, InterruptedException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).sendAsync().get();

        return ethGetTransactionCount.getTransactionCount();
    }

    private Function totalSupply() {
        return new Function(
                "totalSupply",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private Function balanceOf(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private Function transfer(String to, BigInteger value) {
        return new Function(
                "transfer",
                Arrays.asList(new Address(to), new Uint256(value)),
                Collections.singletonList(new TypeReference<Bool>() {}));
    }

    private Function allowance(String owner, String spender) {
        return new Function(
                "allowance",
                Arrays.asList(new Address(owner), new Address(spender)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private Function approve(String spender, BigInteger value) {
        return new Function(
                "approve",
                Arrays.asList(new Address(spender), new Uint256(value)),
                Collections.singletonList(new TypeReference<Bool>() {}));
    }

    private Function transferFrom(String from, String to, BigInteger value) {
        return new Function(
                "transferFrom",
                Arrays.asList(new Address(from), new Address(to), new Uint256(value)),
                Collections.singletonList(new TypeReference<Bool>() {}));
    }

    private Event transferEvent() {
        return new Event(
                "Transfer",
                Arrays.asList(
                        new TypeReference<Address>(true) {},
                        new TypeReference<Address>(true) {},
                        new TypeReference<Uint256>() {}));
    }

    private Event approvalEvent() {
        return new Event(
                "Approval",
                Arrays.asList(
                        new TypeReference<Address>(true) {},
                        new TypeReference<Address>(true) {},
                        new TypeReference<Uint256>() {}));
    }

}
