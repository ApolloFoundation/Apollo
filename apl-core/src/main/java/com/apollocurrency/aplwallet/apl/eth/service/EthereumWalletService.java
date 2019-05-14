package com.apollocurrency.aplwallet.apl.eth.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.utils.Web3jUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.ethereum.util.blockchain.EtherUtil;
import org.slf4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
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

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class EthereumWalletService {
    private static final Logger log = getLogger(EthereumWalletService.class);

    private Web3j web3j = CDI.current().select(Web3j.class).get();
    private PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private final KeyStoreService keyStoreService = CDI.current().select(KeyStoreService.class).get();

    private String paxContractAddress = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");

    /**
     * Get Eth PAX token balance.
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getPaxBalanceWei(String address){
        return getTokenBalance(paxContractAddress, address);
    }

    /**
     * Get Eth PAX token balance.
     * @param address Eth address
     * @return account balance in Eth
     */
    public BigDecimal getPaxBalanceEther(String address) {
        return Web3jUtils.weiToEther(getPaxBalanceWei(address));
    }

    /**
     * Get Eth ERC-20 balance.
     * @param contractAddress ERC-20 address
     * @param address Eth address
     * @return account balance in Wei
     */
    private BigInteger getTokenBalance(String contractAddress, String address) {
        BigInteger balance = null;
        try {
            Function function = balanceOf(address);
            String responseValue = callSmartContractFunction(function, contractAddress, address);

            List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

            balance = (BigInteger) response.get(0).getValue();
        } catch (Exception ex){
            log.error(ex.getMessage());
        }

        return balance;
    }

    private BigInteger getTokenTotalSupply(String contractAddress, String fromAddress) throws ExecutionException, InterruptedException {
        Function function = totalSupply();
        String responseValue = callSmartContractFunction(function, contractAddress, fromAddress);

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

        return (BigInteger) response.get(0).getValue();
    }

    private Function balanceOf(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private Function totalSupply() {
        return new Function(
                "totalSupply",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private String callSmartContractFunction(Function function, String contractAddress, String fromAddress) throws ExecutionException, InterruptedException {
        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(fromAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get();

        return response.getValue();
    }

    /**
     * Get Eth balance.
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getBalanceWei(String address){
    // send asynchronous requests to get balance
        BigInteger wei = null;
        try {
            EthGetBalance ethGetBalance = web3j
                    .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();

            wei = ethGetBalance.getBalance();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return wei;
    }

    /**
     * Get Eth balance.
     * @param address Eth address
     * @return account balance in Eth
     */
    public BigDecimal getBalanceEther(String address) {
        return Web3jUtils.weiToEther(getBalanceWei(address));
    }

    /**
     * Transfer money from account to another one.
     * @param amountEth
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    public String transferEth(String passphrase, long accountId, String toAddress, BigDecimal amountEth, Long gasPrice){
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        Credentials ethCredentials = keyStore.getEthWalletKey().getCredentials();

        return transferEth(ethCredentials.getAddress(), ethCredentials, toAddress, Web3jUtils.etherToWei(amountEth), gasPrice);
    }

    /**
     * Transfer money from account to another one.
     * @param amountEth
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    public String transferEth(String fromAddress, Credentials credentials, String toAddress, BigDecimal amountEth, Long gasPrice){
        return transferEth(fromAddress, credentials, toAddress, Web3jUtils.etherToWei(amountEth), gasPrice);
    }


    /**
     * Transfer money from account to another one.
     * @param amountWei
     * @param gasPrice Gwei
     * @return String - transaction Hash.
     */
    public String transferEth(String fromAddress, Credentials credentials, String toAddress, BigInteger amountWei, Long gasPrice){
        log.info("Account (to address) " + toAddress + "\n" + "Balance before Tx: " + getBalanceWei(toAddress) + "\n");
        log.info("Transfer " + Web3jUtils.weiToEther(amountWei) + " Ether to account");

        // step 1: get the nonce (tx count for sending address)
        EthGetTransactionCount transactionCount = null;
        try {
            transactionCount = web3j
                    .ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        BigInteger nonce = transactionCount.getTransactionCount();
        log.info("Nonce for sending address (coinbase): " + nonce);

        RawTransaction rawTransaction  = RawTransaction
                .createEtherTransaction(
                        nonce,
                        EtherUtil.convert(gasPrice, EtherUtil.Unit.GWEI),
                        Web3jUtils.GAS_LIMIT_ETHER_TX,
                        toAddress,
                        amountWei
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = null;
        try {
            ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
        String transactionHash = ethSendTransaction.getTransactionHash();
        // poll for transaction response via org.web3j.protocol.Web3j.ethGetTransactionReceipt(<txHash>)

        log.info("Tx hash: " + transactionHash);

        return transactionHash;
    }


    public TransactionReceipt waitForReceipt(String transactionHash) {
        int attempts = Web3jUtils.CONFIRMATION_ATTEMPTS;
        int sleep_millis = Web3jUtils.SLEEP_DURATION;

        Optional<TransactionReceipt> receipt = getReceipt(transactionHash);

        while(attempts-- > 0 && !receipt.isPresent()) {
            try {
                Thread.sleep(sleep_millis);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            receipt = getReceipt(transactionHash);
        }

        if (attempts <= 0) {
            throw new RuntimeException("No Tx receipt received");
        }

        return receipt.get();
    }

    /**
     * Returns the TransactionRecipt for the specified tx hash as an optional.
     */
    public Optional<TransactionReceipt> getReceipt(String transactionHash) {
        EthGetTransactionReceipt receipt = null;
        try {
            receipt = web3j
                    .ethGetTransactionReceipt(transactionHash)
                    .sendAsync()
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        return receipt.getTransactionReceipt();
    }





}
