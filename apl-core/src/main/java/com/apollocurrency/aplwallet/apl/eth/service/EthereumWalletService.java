package com.apollocurrency.aplwallet.apl.eth.service;

import com.apollocurrency.aplwallet.apl.core.app.KeyStoreService;
import com.apollocurrency.aplwallet.apl.core.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletKey;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.UserErrorMessageDao;
import com.apollocurrency.aplwallet.apl.exchange.exception.NotSufficientFundsException;
import com.apollocurrency.aplwallet.apl.exchange.exception.NotValidTransactionException;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.UserErrorMessage;
import com.apollocurrency.aplwallet.apl.exchange.service.DexBeanProducer;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.ethereum.util.blockchain.EtherUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeEncoder;
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
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
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
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class EthereumWalletService {

    public final String PAX_CONTRACT_ADDRESS;
    private final KeyStoreService keyStoreService;
    private final DexEthService dexEthService;
    private final UserErrorMessageDao userErrorMessageDao;
    private final DexBeanProducer dexBeanProducer;
    private Web3j web3j;

    @Inject
    public EthereumWalletService(PropertiesHolder propertiesHolder, KeyStoreService keyStoreService, DexEthService dexEthService, UserErrorMessageDao userErrorMessageDao,
                                 DexBeanProducer dexBeanProducer) {
        this.dexBeanProducer = dexBeanProducer;
        this.keyStoreService = keyStoreService;
        this.dexEthService = dexEthService;
        this.userErrorMessageDao = userErrorMessageDao;

        this.PAX_CONTRACT_ADDRESS = propertiesHolder.getStringProperty("apl.eth.pax.contract.address");
    }

    @PostConstruct
    public void init() {
        this.web3j = dexBeanProducer.web3j();
    }

    /**
     * Get balances for Eth/tokens.
     *
     * @param address Eth address
     * @return account balance in Wei
     */
    public EthWalletBalanceInfo balanceInfo(String address) {
        Objects.requireNonNull(address);
        EthWalletBalanceInfo ethWalletBalanceInfo = new EthWalletBalanceInfo(address);

        ethWalletBalanceInfo.put(DexCurrency.ETH.getCurrencyCode(), getEthBalanceWei(address));
        ethWalletBalanceInfo.put(DexCurrency.PAX.getCurrencyCode(), getPaxBalanceWei(address));

        return ethWalletBalanceInfo;
    }


    /**
     * Get Eth balance for ETH wallets
     *
     * @param address Eth address
     * @return ETH account balance in Wei
     */
    public BigInteger getOnlyEthBalanceWei(String address) {
        return getEthBalanceWei(address);
    }


    /**
     * Get Eth / PAX token balance.
     *
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getEthOrPaxBalanceWei(String address, DexCurrency dexCurrency) {
        if (!dexCurrency.isEthOrPax()) {
            throw new UnsupportedOperationException("This currency is not supported");
        }

        if (dexCurrency.isEth()) {
            return getEthBalanceWei(address);
        } else if (dexCurrency.isPax()) {
            return getPaxBalanceWei(address);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Calculate number of confirmations for transaction referred by given hash
     *
     * @param txHash hash of transaction to get number of confirmations
     * @return -1 when transaction does not exists or node responded with error, otherwise return number of confirmations
     * @throws RuntimeException when IO error occurred
     */
    public int getNumberOfConfirmations(String txHash) throws RuntimeException {
        StringValidator.requireNonBlank(txHash);
        int confirmations = -1;
        try {
            EthGetTransactionReceipt txResponse = web3j.ethGetTransactionReceipt(txHash).send();
            TransactionReceipt tx = getResultFrom(txResponse);
            if (tx != null) {
                BigInteger txBlockNumber = tx.getBlockNumber();
                EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
                String blockNumber = getResultFrom(blockNumberResponse);
                if (blockNumber != null) {
                    confirmations = Numeric.decodeQuantity(blockNumber).subtract(txBlockNumber).intValue();
                }
            }
        } catch (MessageDecodingException e) {
            log.warn(e.getMessage(), e, "txHash: " + txHash);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return confirmations;
    }

    private <T> T getResultFrom(Response<T> response) {
        if (response == null) {
            return null;
        }
        if (response.hasError()) {
            log.error("Error processing request: {}", response.getError().getMessage());
            return null;
        }
        if (response.getResult() == null) {
            log.debug("Unable to get result from response - {}" + response.getRawResponse());
            return null;
        }
        return response.getResult();
    }

    /**
     * Get PAX token balance.
     *
     * @param address Eth address
     * @return account balance in Wei
     */
    public BigInteger getPaxBalanceWei(String address) {
        Objects.requireNonNull(address);
        return getTokenBalance(PAX_CONTRACT_ADDRESS, address);
    }

    /**
     * Get Eth balance.
     *
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
     *
     * @param amountEth
     * @param gasPrice  Gwei
     * @return String - transaction Hash.
     */
    public String transfer(String passphrase, long accountId, String fromAddress, String toAddress, BigDecimal amountEth, Long gasPrice, DexCurrency currencies) throws AplException.ExecutiveProcessException {
        WalletKeysInfo keyStore = keyStoreService.getWalletKeysInfo(passphrase, accountId);
        EthWalletKey ethWalletKey = keyStore.getEthWalletForAddress(fromAddress);

        if (ethWalletKey == null) {
            throw new AplException.ExecutiveProcessException("Not found eth address at the user storage: " + fromAddress);
        }

        if (DexCurrency.ETH.equals(currencies)) {
            return transferEth(ethWalletKey.getCredentials(), toAddress, EthUtil.etherToWei(amountEth), gasPrice);
        } else if (DexCurrency.PAX.equals(currencies)) {
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

        return sendApproveTransaction(ethWalletKey.getCredentials(), spenderAddress, ethGasInfo.getAverageSpeedPrice(), value);
    }


    public EthBlock.Block getLastBlock() throws AplException.ExecutiveProcessException {
        try {
            return web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).sendAsync().get().getBlock();
        } catch (ExecutionException | InterruptedException e) {
            throw new AplException.ExecutiveProcessException("Third service is not available.");
        }
    }

    /**
     * Send Approve Transaction
     *
     * @param spenderAddress sender address
     * @param value          amount
     * @return tx transaction id
     */
    private String sendApproveTransaction(Credentials credentials, String spenderAddress, Long gasPrice, BigInteger value) {
        String tx = null;
        try {
            Function function = approve(spenderAddress, value);
            tx = execute(credentials, function, PAX_CONTRACT_ADDRESS, gasPrice);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return tx;
    }

    public BigInteger getAllowance(String spenderAddress, String owner, String contractAddress) throws IOException {
        Function function = allowance(owner, spenderAddress);
        String responseValue = callSmartContractFunction(function, contractAddress, owner);

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

        return (BigInteger) response.get(0).getValue();
    }


    /**
     * Get Eth ERC-20 balance.
     *
     * @param contractAddress ERC-20 address
     * @param address         Eth address
     * @return account balance in Wei
     */
    private BigInteger getTokenBalance(String contractAddress, String address) {
        BigInteger balance = null;
        try {
            Function function = balanceOf(address);
            String responseValue = callSmartContractFunction(function, contractAddress, address);

            List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());

            balance = (BigInteger) response.get(0).getValue();
        } catch (Exception e) {
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
     *
     * @param amountWei
     * @param gasPrice  Gwei
     * @return String - transaction Hash.
     */
    public String transferEth(Credentials credentials, String toAddress, BigInteger amountWei, Long gasPrice) throws AplException.ExecutiveProcessException {
        // step 1: get the nonce (tx count for sending address)
        BigInteger nonce;
        try {
            nonce = getNonce(credentials.getAddress());
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage(), e);
        }
        log.info("Nonce for sending address (coinbase): " + nonce);

        RawTransaction rawTransaction = RawTransaction
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
            throw new AplException.ExecutiveProcessException(e.getMessage(), e);
        }
        String transactionHash = ethSendTransaction.getTransactionHash();

        log.info("Tx hash: " + transactionHash);

        return transactionHash;
    }

    /**
     * Transfer ERC20 tokens from account to another one.
     *
     * @param amountWei
     * @param gasPrice  Gwei
     * @return String - transaction Hash.
     */
    private String transferERC20(String erc20Address, Credentials recipientCredentials, String toAddress, BigInteger amountWei, Long gasPrice) throws AplException.ExecutiveProcessException {
        Function function = transfer(toAddress, amountWei);
        String transactionHash;
        try {
            transactionHash = execute(recipientCredentials, function, erc20Address, gasPrice);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AplException.ExecutiveProcessException(e.getMessage(), e);
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
            throw new AplException.ExecutiveProcessException(e.getMessage(), e);
        }

        return receipt.getTransactionReceipt();
    }

    private String execute(Credentials credentials, Function function, String contractToAddress, Long gasPrice) throws ExecutionException, InterruptedException {
        BigInteger gasPriceWei = EthUtil.gweiToWei(gasPrice);
        BigInteger gasLimitWei = validateBalanceAndReturnGasLimit(credentials.getAddress(), contractToAddress, function, BigInteger.ZERO, gasPriceWei);
        BigInteger nonce = getNonce(credentials.getAddress());
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPriceWei,
            gasLimitWei,
            contractToAddress,
            encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

        return transactionResponse.getTransactionHash();
    }

    public BigInteger estimateGasLimit(String fromAddress, String toAddress, Function function, BigInteger weiValue) {
        try {
            EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(new Transaction(fromAddress, null, null, null
                , toAddress, weiValue, FunctionEncoder.encode(function))).send();
            if (ethEstimateGas.getError() != null) {
                String parameters = function.getInputParameters().stream().map(TypeEncoder::encode).collect(Collectors.joining(","));
                userErrorMessageDao.add(new UserErrorMessage(null, fromAddress, ethEstimateGas.getError().getMessage(), function.getName(), parameters, System.currentTimeMillis()));
                throw new NotValidTransactionException(String.format("Unable to send eth transaction from %s to %s : %s, error - %s", fromAddress, toAddress, function.getName(), ethEstimateGas.getError().getMessage()));
            }
            BigInteger amountUsed = ethEstimateGas.getAmountUsed();
            return amountUsed.add(amountUsed.divide(BigInteger.TEN)); //+10%
        } catch (IOException e) {
            throw new RuntimeException("I/O error occurred, maybe eth node is down");
        }
    }

    public BigInteger validateBalanceAndReturnGasLimit(String fromAddress, String toAddress, Function function, BigInteger weiValue, BigInteger gasPrice) {
        BigInteger gasLimit = estimateGasLimit(fromAddress, toAddress, function, weiValue);
        BigInteger balance = getEthBalanceWei(fromAddress);
        BigInteger fee = gasLimit.multiply(gasPrice);
        BigInteger requiredAmount = fee.add(weiValue);
        if (balance.compareTo(requiredAmount) < 0) {
            throw new NotSufficientFundsException("Not enough eth to pay fee + amount. Current balance -  " + balance + ", required " + requiredAmount);
        }
        return gasLimit;
    }

    private BigInteger getNonce(String address) throws ExecutionException, InterruptedException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
            address, DefaultBlockParameterName.PENDING).sendAsync().get();

        return ethGetTransactionCount.getTransactionCount();
    }

    private Function totalSupply() {
        return new Function(
            "totalSupply",
            Collections.emptyList(),
            Collections.singletonList(new TypeReference<Uint256>() {
            }));
    }

    private Function balanceOf(String owner) {
        return new Function(
            "balanceOf",
            Collections.singletonList(new Address(owner)),
            Collections.singletonList(new TypeReference<Uint256>() {
            }));
    }

    private Function transfer(String to, BigInteger value) {
        return new Function(
            "transfer",
            Arrays.asList(new Address(to), new Uint256(value)),
            Collections.singletonList(new TypeReference<Bool>() {
            }));
    }

    private Function allowance(String owner, String spender) {
        return new Function(
            "allowance",
            Arrays.asList(new Address(owner), new Address(spender)),
            Collections.singletonList(new TypeReference<Uint256>() {
            }));
    }

    private Function approve(String spender, BigInteger value) {
        return new Function(
            "approve",
            Arrays.asList(new Address(spender), new Uint256(value)),
            Collections.singletonList(new TypeReference<Bool>() {
            }));
    }

    private Function transferFrom(String from, String to, BigInteger value) {
        return new Function(
            "transferFrom",
            Arrays.asList(new Address(from), new Address(to), new Uint256(value)),
            Collections.singletonList(new TypeReference<Bool>() {
            }));
    }

    private Event transferEvent() {
        return new Event(
            "Transfer",
            Arrays.asList(
                new TypeReference<Address>(true) {
                },
                new TypeReference<Address>(true) {
                },
                new TypeReference<Uint256>() {
                }));
    }

    private Event approvalEvent() {
        return new Event(
            "Approval",
            Arrays.asList(
                new TypeReference<Address>(true) {
                },
                new TypeReference<Address>(true) {
                },
                new TypeReference<Uint256>() {
                }));
    }

}
