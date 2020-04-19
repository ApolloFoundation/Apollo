package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.Currency;
import com.apollocurrency.aplwallet.api.dto.DGSGoodsDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.DexTradeInfoDto;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.BlocksResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrencyResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountLedgerResponse;
import com.apollocurrency.aplwallet.api.response.AccountOpenAssetOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountPropertiesResponse;
import com.apollocurrency.aplwallet.api.response.AccountTransactionIdsResponse;
import com.apollocurrency.aplwallet.api.response.AllTaggedDataResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.CurrenciesResponse;
import com.apollocurrency.aplwallet.api.response.CurrencyAccountsResponse;
import com.apollocurrency.aplwallet.api.response.DataTagCountResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountBlockCountResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrency.aplwallet.api.response.PollResultResponse;
import com.apollocurrency.aplwallet.api.response.PollVotesResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import io.qameta.allure.Step;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.addParameters;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.getInstanse;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.httpCallGet;
import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.httpCallPost;
import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.cancelAskOrder;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.cancelBidOrder;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.currencyBuy;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.currencyMint;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.currencyReserveClaim;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.currencyReserveIncrease;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.currencySell;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.deleteAssetShares;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.deleteCurrency;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsDelisting;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsDelivery;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsFeedback;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsListing;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsPriceChange;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsPurchase;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsQuantityChange;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dgsRefund;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.dividendPayment;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.extendTaggedData;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountAssetCount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountAssets;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountBlockCount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountBlockIds;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountBlocks;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountCurrencies;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountCurrentAskOrderIds;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountCurrentAskOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountCurrentBidOrderIds;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountCurrentBidOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountId;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountLedger;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountLedgerEntry;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAccountProperties;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllAssets;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllCurrencies;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllOpenAskOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllOpenBidOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllTaggedData;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAllTrades;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAskOrder;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAskOrderIds;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAskOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAsset;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAssetAccountCount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAssetAccounts;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAssetDeletes;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getAssetIds;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBalance;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBidOrders;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBlock;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBlockId;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBlockchainStatus;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getBlocks;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getCurrency;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getCurrencyAccounts;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getDGSGood;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getDataTagCount;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getECBlock;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getExpectedAssetDeletes;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getForging;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getPoll;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getPollResult;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getPollVotes;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getShuffling;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.getTaggedData;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.issueAsset;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.placeAskOrder;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.placeBidOrder;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.publishExchangeOffer;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.scheduleCurrencyBuy;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.searchTaggedData;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.shufflingCancel;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.shufflingCreate;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.shufflingProcess;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.shufflingRegister;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.shufflingVerify;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.startForging;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.startShuffler;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.stopForging;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.transferAsset;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.transferCurrency;
import static com.apollocurrrency.aplwallet.inttest.model.RequestType.uploadTaggedData;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestBaseOld extends TestBase {
    public static final Logger log = LoggerFactory.getLogger(TestBaseOld.class);

    @Step
    public boolean verifyTransactionInBlock(String transaction) {
        boolean inBlock = false;
        try {
            inBlock = Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).getConfirmations() >= 0);
        } catch (Exception e) {
            fail("Transaction does't add to block. Transaction " + transaction + " Exception: " + e.getMessage());
        }
        assertTrue(inBlock, String.format("Transaction %s in block: ", transaction));
        return inBlock;
    }

    @Step
    public boolean waitForHeight(int height) {
        log.info("Wait For Height: {}", height);
        RetryPolicy retry = new RetryPolicy()
            .retryWhen(false)
            .withMaxRetries(15)
            .withDelay(10, TimeUnit.SECONDS);
        boolean isHeight = false;

        try {
            isHeight = Failsafe.with(retry).get(() -> getBlock().getHeight() >= height);
        } catch (Exception e) {
            fail(String.format("Height %s  not reached. Exception msg: %s", height, e.getMessage()));
        }
        assertTrue(isHeight, String.format("Height %s not reached: %s", height, getBlock().getHeight()));
        return isHeight;
    }

    @Step
    public TransactionDTO getTransaction(String transaction) {
        addParameters(RequestType.requestType, RequestType.getTransaction);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(TransactionDTO.class);
    }

    @Step
    public BlockListInfoResponse getAccountBlocks(String account) {
        addParameters(RequestType.requestType, getAccountBlocks);
        addParameters(Parameters.account, account);
        return getInstanse(BlockListInfoResponse.class);
    }

    @Step
    public GetAccountResponse getAccount(String account) {
        addParameters(RequestType.requestType, getAccount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountResponse.class);
    }

    @Step
    public GetAccountBlockCountResponse getAccountBlockCount(String account) {
        addParameters(RequestType.requestType, getAccountBlockCount);
        addParameters(Parameters.account, account);
        return getInstanse(GetAccountBlockCountResponse.class);
    }

    @Step
    public AccountBlockIdsResponse getAccountBlockIds(String account) {
        addParameters(RequestType.requestType, getAccountBlockIds);
        addParameters(Parameters.account, account);
        return getInstanse(AccountBlockIdsResponse.class);
    }

    @Step
    public AccountDTO getAccountId(String secretPhrase) {
        addParameters(RequestType.requestType, getAccountId);
        addParameters(Parameters.secretPhrase, secretPhrase);
        return getInstanse(AccountDTO.class);
    }

    @Step
    public AccountLedgerResponse getAccountLedger(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountLedger);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountLedgerResponse.class);
    }

    @Step
    public AccountPropertiesResponse getAccountProperties(String account) {
        addParameters(RequestType.requestType, getAccountProperties);
        addParameters(Parameters.recipient, account);
        return getInstanse(AccountPropertiesResponse.class);
    }

    @Step
    public SearchAccountsResponse searchAccounts(String searchQuery) {
        addParameters(RequestType.requestType, RequestType.searchAccounts);
        addParameters(Parameters.query, searchQuery);
        return getInstanse(SearchAccountsResponse.class);
    }

    @Step
    public TransactionListResponse getUnconfirmedTransactions(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getUnconfirmedTransactions);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(TransactionListResponse.class);
    }

    @Step
    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) {
        addParameters(RequestType.requestType, RequestType.getUnconfirmedTransactionIds);
        addParameters(Parameters.account, account);
        return getInstanse(AccountTransactionIdsResponse.class);
    }

    @Step
    public BalanceDTO getGuaranteedBalance(String account, int confirmations) {
        addParameters(RequestType.requestType, RequestType.getGuaranteedBalance);
        addParameters(Parameters.account, account);
        addParameters(Parameters.numberOfConfirmations, confirmations);
        return getInstanse(BalanceDTO.class);
    }

    @Step
    public BalanceDTO getBalance(Wallet wallet) {
        addParameters(RequestType.requestType, getBalance);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(BalanceDTO.class);
    }

    @Step
    public EntryDTO getAccountLedgerEntry(String ledgerId) {
        addParameters(RequestType.requestType, getAccountLedgerEntry);
        addParameters(Parameters.ledgerId, ledgerId);
        return getInstanse(EntryDTO.class);
    }

    @Step
    public CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount) {
        addParameters(RequestType.requestType, RequestType.sendMoney);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount + "00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public AccountDTO getAccountPublicKey(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getAccountPublicKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountDTO.class);
    }

    @Step
    public BlockchainTransactionsResponse getAccountTransaction(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getBlockchainTransactions);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(BlockchainTransactionsResponse.class);
    }

    @Step
    public CreateTransactionResponse setAccountInfo(Wallet wallet, String accountName, String accountDescription) {
        addParameters(RequestType.requestType, RequestType.setAccountInfo);
        addParameters(Parameters.name, accountName);
        addParameters(Parameters.description, accountDescription);
        addParameters(Parameters.wallet, wallet);
        // addParameters(Parameters.recipient, accountID);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 300000000);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse setAccountProperty(Wallet wallet, String property) {
        addParameters(RequestType.requestType, RequestType.setAccountProperty);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, wallet.getUser());
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.feeATM, 100000000);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteAccountProperty(Wallet wallet, String property) {
        addParameters(RequestType.requestType, RequestType.deleteAccountProperty);
        addParameters(Parameters.property, property);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public AccountPropertiesResponse getAccountProperty(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getAccountProperties);
        addParameters(Parameters.recipient, wallet.getUser());
        return getInstanse(AccountPropertiesResponse.class);
    }

    @Step
    //Skrypchenko Serhii
    public AccountAliasesResponse getAliases(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getAliases);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountAliasesResponse.class);
    }

    //Skrypchenko Serhii
    @Step
    public AccountCountAliasesResponse getAliasCount(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.getAliasCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCountAliasesResponse.class);
    }

    @Step
    //Skrypchenko Serhii
    public AccountAliasDTO getAlias(String aliasname) {
        addParameters(RequestType.requestType, RequestType.getAlias);
        addParameters(Parameters.aliasName, aliasname);
        return getInstanse(AccountAliasDTO.class);
    }


    //Skrypchenko Serhii
    @Step
    public CreateTransactionResponse setAlias(Wallet wallet, String aliasURL, String aliasName, Integer feeATM, Integer deadline) {
        addParameters(RequestType.requestType, RequestType.setAlias);
        addParameters(Parameters.aliasURI, aliasURL);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, feeATM);
        addParameters(Parameters.deadline, deadline);
        return getInstanse(CreateTransactionResponse.class);

    }

    @Step
    //Skrypchenko Serhii
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasname) {
        addParameters(RequestType.requestType, RequestType.deleteAlias);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.aliasName, aliasname);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    @Step
    //Serhii Skrypchenko
    public AccountAliasesResponse getAliasesLike(String aliasename) {
        addParameters(RequestType.requestType, RequestType.getAliasesLike);
        addParameters(Parameters.aliasPrefix, aliasename);
        return getInstanse(AccountAliasesResponse.class);
    }


    //Serhii Skrypchenko (sell Alias)
    @Step
    public CreateTransactionResponse sellAlias(Wallet wallet, String aliasName) {
        addParameters(RequestType.requestType, RequestType.sellAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.priceATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse buyAlias(Wallet wallet, String aliasName) {
        addParameters(RequestType.requestType, RequestType.buyAlias);
        addParameters(Parameters.aliasName, aliasName);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, 100000000);
        addParameters(Parameters.amountATM, 1500000000);
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet, String recipient, int moneyAmount) {
        addParameters(RequestType.requestType, RequestType.sendMoneyPrivate);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.amountATM, moneyAmount + "00000000");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "500000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public Account2FAResponse generateNewAccount() {
        addParameters(RequestType.requestType, RequestType.generateAccount);
        return getInstanse(Account2FAResponse.class);
    }

    @Step
    public Account2FAResponse deleteSecretFile(Wallet wallet) {
        try {
            addParameters(RequestType.requestType, RequestType.deleteKey);
            addParameters(Parameters.wallet, wallet);
            return mapper.readValue(httpCallPost().body().string(), Account2FAResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Step
    public VaultWalletResponse exportSecretFile(Wallet wallet) {
        addParameters(RequestType.requestType, RequestType.exportKey);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(VaultWalletResponse.class);
    }

    @Step
    public boolean importSecretFile(String pathToSecretFile, String pass) {
        // addParameters(RequestType.requestType, importKey);
        //  addParameters(Parameters.wallet, wallet);
        //  return getInstanse(Account2FAResponse.class);
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<ShardDTO> getShards(String ip) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Step
    public AccountDTO enable2FA(Wallet wallet) {
        // addParameters(RequestType.requestType,RequestType.enable2FA);
        //  addParameters(Parameters.wallet, wallet);
        // return getInstanse(AccountDTO.class);
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexOrderDto> getDexOrders() {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexOrderDto> getDexHistory(String account, String pair, String type) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexOrderDto> getDexHistory(String account) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public EthGasInfoResponse getEthGasInfo() {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexTradeInfoDto> getDexTradeInfo(String pairCurrency, Integer startTime, Integer finishTime) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public CreateTransactionResponse dexCancelOrder(String orderId, Wallet wallet) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public String createDexOrder(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public List<DexOrderDto> getDexOrders(String accountId) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public Account2FAResponse getDexBalances(String ethAddress) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Override
    @Step
    public WithdrawResponse dexWidthraw(String fromAddress, Wallet wallet, String toAddress, String amount, String transferFee, boolean isEth) {
        throw new NotImplementedException("Already implemented in TestBaseNew");
    }

    @Step
    public PollDTO getPoll(String poll) {
        addParameters(RequestType.requestType, getPoll);
        addParameters(Parameters.poll, poll);
        return getInstanse(PollDTO.class);
    }

    @Step("Create Poll with param: votingModel: {2}")
    public CreateTransactionResponse createPoll(Wallet wallet, int votingModel, String name, int plusFinishHeight, String holding, int minBalance, int maxRangeValue) {
        int currentHeight = getBlock().getHeight();
        int finishHeight = currentHeight + plusFinishHeight;

        addParameters(RequestType.requestType, RequestType.createPoll);
        addParameters(Parameters.name, name);
        addParameters(Parameters.finishHeight, finishHeight);
        addParameters(Parameters.minNumberOfOptions, 1);
        addParameters(Parameters.maxNumberOfOptions, 1);
        addParameters(Parameters.feeATM, 1000000000);
        addParameters(Parameters.isCustomFee, true);
        addParameters(Parameters.minRangeValue, 0);
        addParameters(Parameters.maxRangeValue, maxRangeValue);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.answers, "YES");
        addParameters(Parameters.answers, "NO");
        addParameters(Parameters.answers, "MAYBE");
        addParameters(Parameters.create_poll_answers, 1);
        addParameters(Parameters.option00, "YES");
        addParameters(Parameters.option01, "NO");
        addParameters(Parameters.option02, "MAYBE");
        addParameters(Parameters.deadline, 1440);

        if (votingModel == 0) {
            addParameters(Parameters.votingModel, votingModel);
            addParameters(Parameters.minBalanceModel, 0);
            addParameters(Parameters.minBalance, 0);
            addParameters(Parameters.description, "poll by account");
            addParameters(Parameters.holding, "");
        }

        if (votingModel == 1) {
            addParameters(Parameters.votingModel, votingModel);
            addParameters(Parameters.minBalanceModel, 1);
            addParameters(Parameters.minBalance, minBalance);
            addParameters(Parameters.description, "poll by account balance");
            addParameters(Parameters.holding, "");
        }

        if (votingModel == 2) {
            addParameters(Parameters.votingModel, votingModel);
            addParameters(Parameters.minBalanceModel, 2);
            addParameters(Parameters.minBalance, minBalance);
            addParameters(Parameters.holding, holding);
            addParameters(Parameters.description, "poll by asset");
        }

        if (votingModel == 3) {
            addParameters(Parameters.votingModel, votingModel);
            addParameters(Parameters.minBalanceModel, 3);
            addParameters(Parameters.minBalance, minBalance);
            addParameters(Parameters.holding, holding);
            addParameters(Parameters.description, "poll by currency");
        }

        return getInstanse(CreateTransactionResponse.class);
    }

    public CreateTransactionResponse castVote(Wallet wallet, String poll, int vote) {

        addParameters(RequestType.requestType, RequestType.castVote);
        addParameters(Parameters.feeATM, 2000000000);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.vote00, vote);
        addParameters(Parameters.vote01, "");
        addParameters(Parameters.vote02, "");
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.poll, poll);

        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public List<String> getPeers() {
        try {
            addParameters(RequestType.requestType, RequestType.getPeers);
            addParameters(Parameters.active, true);
            Response response = httpCallGet();
            Assertions.assertEquals(200, response.code());
            GetPeersIpResponse peers = mapper.readValue(response.body().string(), GetPeersIpResponse.class);
            return peers.getPeers();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Step
    public PeerDTO getPeer(String peer) {
        addParameters(RequestType.requestType, RequestType.getPeer);
        addParameters(Parameters.peer, peer);
        return getInstanse(PeerDTO.class);
    }

    @Step
    public PeerDTO addPeer(String ip) {
        addParameters(RequestType.requestType, RequestType.addPeer);
        addParameters(Parameters.peer, ip);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(PeerDTO.class);
    }

    @Step
    public PeerInfo getMyInfo() {
        addParameters(RequestType.requestType, RequestType.getMyInfo);
        return getInstanse(PeerInfo.class);
    }

    @Step
    public BlockDTO getBlock(String block) {
        addParameters(RequestType.requestType, getBlock);
        addParameters(Parameters.block, block);
        addParameters(Parameters.includeTransactions, true);
        return getInstanse(BlockDTO.class);

    }

    @Step
    public BlockDTO getBlock() {
        addParameters(RequestType.requestType, getBlock);
        return getInstanse(BlockDTO.class);

    }

    @Step
    public GetBlockIdResponse getBlockId(String height) {
        addParameters(RequestType.requestType, getBlockId);
        addParameters(Parameters.height, height);
        return getInstanse(GetBlockIdResponse.class);

    }

    @Step
    public BlockchainInfoDTO getBlockchainStatus() {
        addParameters(RequestType.requestType, getBlockchainStatus);
        return getInstanse(BlockchainInfoDTO.class);
    }

    @Step
    public BlocksResponse getBlocks() {
        addParameters(RequestType.requestType, getBlocks);
        return getInstanse(BlocksResponse.class);
    }

    @Step
    public void verifyCreatingTransaction(CreateTransactionResponse transaction) {
        assertNotNull(transaction);
        assertNotNull(transaction.getTransaction(), transaction.errorDescription);
        assertNotNull(transaction.getTransactionJSON(), transaction.errorDescription);
        assertNotNull(transaction.getTransactionJSON().getSenderPublicKey());
        assertNotNull(transaction.getTransactionJSON().getSignature());
        assertNotNull(transaction.getTransactionJSON().getFullHash());
        assertNotNull(transaction.getTransactionJSON().getAmountATM());
        assertNotNull(transaction.getTransactionJSON().getEcBlockId());
        assertNotNull(transaction.getTransactionJSON().getSenderRS());
        assertNotNull(transaction.getTransactionJSON().getTransaction());
        assertNotNull(transaction.getTransactionJSON().getFeeATM());
        assertNotNull(transaction.getTransactionJSON().getType());

    }

    //AssetExchange
    //issueAsset
    @Step
    public CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU) {
        addParameters(RequestType.requestType, issueAsset);
        addParameters(Parameters.name, assetName);
        addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //placeBidOrder
    @Step
    public CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        addParameters(RequestType.requestType, placeBidOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 1400);
        return getInstanse(CreateTransactionResponse.class);

    }

    //placeAskOrder
    @Step
    public CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        addParameters(RequestType.requestType, placeAskOrder);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000");
        addParameters(Parameters.deadline, 1400);
        return getInstanse(CreateTransactionResponse.class);

    }


    //cancelBidOrder
    @Step
    public CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder) {
        addParameters(RequestType.requestType, cancelBidOrder);
        addParameters(Parameters.order, bidOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    //cancelAskOrder
    @Step
    public CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder) {
        addParameters(RequestType.requestType, cancelAskOrder);
        addParameters(Parameters.order, askOrder);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //deleteAssetShares
    @Step
    public CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU) {
        addParameters(RequestType.requestType, deleteAssetShares);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    //dividendPayment
    @Step
    public CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height) {
        addParameters(RequestType.requestType, dividendPayment);
        addParameters(Parameters.asset, assetID);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.amountATMPerATU, amountATMPerATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.height, height);
        return getInstanse(CreateTransactionResponse.class);
    }


    //getAccountAssets
    @Step
    public AccountAssetsResponse getAccountAssets(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountAssets);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountAssetsResponse.class);
    }

    //getAccountAssetCount
    @Step
    public AccountAssetsCountResponse getAccountAssetCount(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountAssetCount);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountAssetsCountResponse.class);
    }

    //getAsset
    @Step
    public AccountAssetDTO getAsset(String asset) {
        addParameters(RequestType.requestType, getAsset);
        addParameters(Parameters.asset, asset);
        return getInstanse(AccountAssetDTO.class);
    }


    //getAccountCurrentAskOrderIds
    @Step
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentAskOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetAskOrderIdsResponse.class);
    }

    //getAccountCurrentBidOrderIds
    @Step
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentBidOrderIds);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetBidOrderIdsResponse.class);
    }


    //getAccountCurrentAskOrders
    @Step
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentAskOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetAskOrdersResponse.class);
    }

    //getAccountCurrentBidOrders
    @Step
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrentBidOrders);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(AccountCurrentAssetBidOrdersResponse.class);
    }

    //getAllAssets
    @Step
    public AssetsResponse getAllAssets() {
        addParameters(RequestType.requestType, getAllAssets);
        return getInstanse(AssetsResponse.class);
    }

    //getAllOpenAskOrders
    @Step
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders() {
        addParameters(RequestType.requestType, getAllOpenAskOrders);
        return getInstanse(AccountOpenAssetOrdersResponse.class);
    }


    //getAllOpenBidOrders
    @Step
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders() {
        addParameters(RequestType.requestType, getAllOpenBidOrders);
        return getInstanse(AccountOpenAssetOrdersResponse.class);
    }

    //getAllTrades
    @Step
    public AssetTradeResponse getAllTrades() {
        addParameters(RequestType.requestType, getAllTrades);
        return getInstanse(AssetTradeResponse.class);
    }

    //getAskOrder
    @Step
    public AccountAssetOrderDTO getAskOrder(String askOrder) {
        addParameters(RequestType.requestType, getAskOrder);
        addParameters(Parameters.order, askOrder);
        return getInstanse(AccountAssetOrderDTO.class);
    }

    //getAskOrderIds
    @Step
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID) {
        addParameters(RequestType.requestType, getAskOrderIds);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AccountCurrentAssetAskOrderIdsResponse.class);
    }

    //getAskOrders
    @Step
    public AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID) {
        addParameters(RequestType.requestType, getAskOrders);
        addParameters(Parameters.asset, assetID);

        return getInstanse(AccountCurrentAssetAskOrdersResponse.class);
    }

    //getBidOrders
    @Step
    public AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID) {
        addParameters(RequestType.requestType, getBidOrders);
        addParameters(Parameters.asset, assetID);

        return getInstanse(AccountCurrentAssetBidOrdersResponse.class);
    }


    //getAssetAccountCount
    @Step
    public AssetsAccountsCountResponse getAssetAccountCount(String assetID) {
        addParameters(RequestType.requestType, getAssetAccountCount);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AssetsAccountsCountResponse.class);
    }

    //getAssetAccounts
    @Step
    public AccountAssetsResponse getAssetAccounts(String assetID) {
        addParameters(RequestType.requestType, getAssetAccounts);
        addParameters(Parameters.asset, assetID);
        return getInstanse(AccountAssetsResponse.class);
    }

    //getAssetDeletes
    @Step
    public ExpectedAssetDeletes getAssetDeletes(Wallet wallet) {
        addParameters(RequestType.requestType, getAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(ExpectedAssetDeletes.class);
    }

    //getExpectedAssetDeletes
    @Step
    public ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet) {
        addParameters(RequestType.requestType, getExpectedAssetDeletes);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(ExpectedAssetDeletes.class);
    }

    //getAssetDividends NOT READY YET!!!!!
    /*public  GetAssetDividends getAssetDividends (Wallet wallet) {
        addParameters(RequestType.requestType, getAssetDividends);
        addParameters(Parameters.wallet, wallet);
        return getInstanse(GetAssetDividends.class);
    }*/

    //getAssetIds
    @Step
    public AccountAssetsIdsResponse getAssetIds() {
        addParameters(RequestType.requestType, getAssetIds);
        return getInstanse(AccountAssetsIdsResponse.class);
    }

    //transferAsset
    @Step
    public CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient) {
        addParameters(RequestType.requestType, transferAsset);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.asset, asset);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.quantityATU, quantityATU);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }


    @Step
    public ECBlockDTO getECBlock() {
        addParameters(RequestType.requestType, getECBlock);
        return getInstanse(ECBlockDTO.class);
    }

    @Step
    public ForgingResponse getForging() {
        addParameters(RequestType.requestType, getForging);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(ForgingResponse.class);
    }

    @Step
    public ForgingDetails startForging(Wallet wallet) {
        addParameters(RequestType.requestType, startForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(ForgingDetails.class);
    }

    @Step
    public ForgingDetails stopForging(Wallet wallet) {
        addParameters(RequestType.requestType, stopForging);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.adminPassword, getTestConfiguration().getAdminPass());
        return getInstanse(ForgingDetails.class);
    }

    @Step
    public CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage) {
        ;
        addParameters(RequestType.requestType, RequestType.sendMessage);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.message, testMessage);
        addParameters(Parameters.feeATM, 500000000);
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.messageIsPrunable, true);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public AccountMessageDTO readMessage(Wallet wallet, String transaction) {
        addParameters(RequestType.requestType, RequestType.readMessage);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(AccountMessageDTO.class);
    }

    /*
     phasingQuorum is the number of "votes" needed for transaction approval
    (required if phasingVotingModel >= 0, default 0):
     0 for voting model -1
     the number of accounts for model 0
     total ATM for model 1
     total ATU for models 2 and 3
     the number of transactions for model 4  1 for model 5
    */
    public void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding) {
        addParameters(Parameters.phased, true);
        addParameters(Parameters.phasingFinishHeight, phasingFinishHeight);
        addParameters(Parameters.votingModel, votingModel);
        addParameters(Parameters.phasingQuorum, phasingQuorum);
        addParameters(Parameters.phasingMinBalance, phasingMinBalance);
        addParameters(Parameters.phasingMinBalanceModel, phasingMinBalanceModel);
        addParameters(Parameters.phasingHolding, phasingHolding);
    }


    @Step("Issue Currency with param: Type: {2}")
    public CreateTransactionResponse issueCurrency(Wallet wallet, int type, String name, String description, String code, int initialSupply, int maxSupply, int decimals) {
        int currentHeight = getBlock().getHeight();
        int issuanceHeight = currentHeight + 11;

        final int EXCHANGEABLE = 1;
        final int CONTROLLABLE = 2;
        final int RESERVABLE = 4;
        final int CLAIMABLE = 8;
        final int MINTABLE = 16;
        final int NON_SHUFFLEABLE = 32;

        addParameters(RequestType.requestType, RequestType.issueCurrency);
        addParameters(Parameters.name, name);
        addParameters(Parameters.code, code);
        addParameters(Parameters.description, description);
        addParameters(Parameters.type, type);
        addParameters(Parameters.initialSupply, initialSupply);
        addParameters(Parameters.decimals, decimals);
        addParameters(Parameters.feeATM, 100000000000L);
        addParameters(Parameters.deadline, "1440");
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.issuanceHeight, 0);
        addParameters(Parameters.maxSupply, maxSupply);
        addParameters(Parameters.reserveSupply, 0);

        if ((type & RESERVABLE) == RESERVABLE) {
            addParameters(Parameters.maxSupply, maxSupply + 50);
            addParameters(Parameters.reserveSupply, maxSupply + 50);
            addParameters(Parameters.issuanceHeight, issuanceHeight);
            addParameters(Parameters.minReservePerUnitATM, 1);
        }
        if ((type & CLAIMABLE) == CLAIMABLE) {
            addParameters(Parameters.initialSupply, 0);
        }
        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) == RESERVABLE) {
            addParameters(Parameters.algorithm, 2);
            addParameters(Parameters.minDifficulty, 1);
            addParameters(Parameters.maxDifficulty, 2);
            addParameters(Parameters.maxSupply, maxSupply + 50);
            addParameters(Parameters.reserveSupply, maxSupply + 10);
        }

        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) != RESERVABLE) {
            addParameters(Parameters.algorithm, 2);
            addParameters(Parameters.minDifficulty, 1);
            addParameters(Parameters.maxDifficulty, 2);
            addParameters(Parameters.reserveSupply, 0);
        }

        return getInstanse(CreateTransactionResponse.class);
    }


    @Step
    public CreateTransactionResponse deleteCurrency(Wallet wallet, String CurrencyId) {
        addParameters(RequestType.requestType, deleteCurrency);
        addParameters(Parameters.currency, CurrencyId);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CurrencyAccountsResponse getCurrencyAccounts(String CurrencyId) {
        addParameters(RequestType.requestType, getCurrencyAccounts);
        addParameters(Parameters.currency, CurrencyId);
        return getInstanse(CurrencyAccountsResponse.class);
    }

    @Step
    public Currency getCurrency(String CurrencyId) {
        addParameters(RequestType.requestType, getCurrency);
        addParameters(Parameters.currency, CurrencyId);
        return getInstanse(Currency.class);
    }

    @Step
    public CurrenciesResponse getAllCurrencies() {
        addParameters(RequestType.requestType, getAllCurrencies);
        return getInstanse(CurrenciesResponse.class);
    }

    @Step
    public CreateTransactionResponse transferCurrency(String recipient, String currency, Wallet wallet, int units) {
        addParameters(RequestType.requestType, transferCurrency);
        addParameters(Parameters.recipient, recipient);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyMint(Long nonce, String currency, Wallet wallet, int units, int counter) {
        addParameters(RequestType.requestType, currencyMint);
        addParameters(Parameters.nonce, nonce);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.counter, counter);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveClaim(String currency, Wallet wallet, int units) {
        addParameters(RequestType.requestType, currencyReserveClaim);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveIncrease(String currency, Wallet wallet, int amountPerUnitATM) {
        addParameters(RequestType.requestType, currencyReserveIncrease);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.amountPerUnitATM, amountPerUnitATM);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse publishExchangeOffer(String currency, Wallet wallet, int buyRateATM, int sellRateATM, int initialBuySupply, int initialSellSupply) {
        addParameters(RequestType.requestType, publishExchangeOffer);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.buyRateATM, buyRateATM);
        addParameters(Parameters.sellRateATM, sellRateATM);
        addParameters(Parameters.totalBuyLimit, 1000);
        addParameters(Parameters.totalSellLimit, 1000);
        addParameters(Parameters.initialBuySupply, initialBuySupply);
        addParameters(Parameters.initialSellSupply, initialSellSupply);
        addParameters(Parameters.expirationHeight, 999999999);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencySell(String currency, Wallet wallet, int units, int rate) {
        addParameters(RequestType.requestType, currencySell);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.rateATM, rate);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyBuy(String currency, Wallet wallet, int units, int rate) {
        addParameters(RequestType.requestType, currencyBuy);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.rateATM, rate);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse scheduleCurrencyBuy(String currency, Wallet wallet, int units, int rate, String offerIssuer) {
        addParameters(RequestType.requestType, scheduleCurrencyBuy);
        addParameters(Parameters.currency, currency);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.units, units);
        addParameters(Parameters.offerIssuer, offerIssuer);
        addParameters(Parameters.rateATM, rate);
        addParameters(Parameters.prunableAttachmentJSON, "{Invalid JSON Itegration Test}");
        addParameters(Parameters.broadcast, false);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public AccountCurrencyResponse getAccountCurrencies(Wallet wallet) {
        addParameters(RequestType.requestType, getAccountCurrencies);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.includeCurrencyInfo, true);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(AccountCurrencyResponse.class);
    }

    @Step
    public CreateTransactionResponse shufflingCreate(Wallet wallet, int registrationPeriod, int participantCount, int amount, String holding, int holdingType) {
        addParameters(RequestType.requestType, shufflingCreate);
        addParameters(Parameters.wallet, wallet);
        if (holdingType > 0) {
            addParameters(Parameters.holding, holding);
            addParameters(Parameters.holdingType, holdingType);
            addParameters(Parameters.amount, amount);
        } else {
            addParameters(Parameters.amount, amount + "00000000");
        }
        addParameters(Parameters.registrationPeriod, registrationPeriod);
        addParameters(Parameters.participantCount, participantCount);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    @Step
    public CreateTransactionResponse shufflingCancel(Wallet wallet, String shuffling, String cancellingAccount, String shufflingStateHash) {
        addParameters(RequestType.requestType, shufflingCancel);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.shuffling, shuffling);
        if (cancellingAccount != null) {
            addParameters(Parameters.cancellingAccount, cancellingAccount);
        }
        addParameters(Parameters.shufflingStateHash, shufflingStateHash);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);

    }

    @Step
    public ShufflingDTO getShuffling(String shuffling) {
        addParameters(RequestType.requestType, getShuffling);
        ;
        addParameters(Parameters.shuffling, shuffling);
        addParameters(Parameters.includeHoldingInfo, false);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(ShufflingDTO.class);
    }

    @Step
    public CreateTransactionResponse shufflingRegister(Wallet wallet, String shufflingFullHash) {
        addParameters(RequestType.requestType, shufflingRegister);
        ;
        addParameters(Parameters.shufflingFullHash, shufflingFullHash);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse shufflingProcess(Wallet wallet, String shuffling, String recipientSecretPhrase) {
        addParameters(RequestType.requestType, shufflingProcess);
        addParameters(Parameters.shuffling, shuffling);
        addParameters(Parameters.recipientSecretPhrase, recipientSecretPhrase);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse startShuffler(Wallet wallet, String shufflingFullHash, String recipientSecretPhrase) {
        addParameters(RequestType.requestType, startShuffler);
        addParameters(Parameters.shufflingFullHash, shufflingFullHash);
        addParameters(Parameters.recipientSecretPhrase, recipientSecretPhrase);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse shufflingVerify(Wallet wallet, String shuffling, String shufflingStateHash) {
        addParameters(RequestType.requestType, shufflingVerify);
        addParameters(Parameters.shuffling, shuffling);
        addParameters(Parameters.shufflingStateHash, shufflingStateHash);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsListing(Wallet wallet, String name, String description, String tags, int quantity, int priceATM, File file) {
        addParameters(RequestType.requestType, dgsListing);
        addParameters(Parameters.name, name);
        addParameters(Parameters.messageFile, file);
        addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.tags, tags);
        addParameters(Parameters.quantity, quantity);
        addParameters(Parameters.messageIsText, false);
        addParameters(Parameters.messageIsPrunable, true);
        addParameters(Parameters.priceATM, priceATM + "00000000");
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsDelisting(Wallet wallet, String goods) {
        addParameters(RequestType.requestType, dgsDelisting);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.goods, goods);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsQuantityChange(Wallet wallet, String goods, int deltaQuantity) {
        addParameters(RequestType.requestType, dgsQuantityChange);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.goods, goods);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        addParameters(Parameters.deltaQuantity, deltaQuantity);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsPriceChange(Wallet wallet, String goods, int priceATM) {
        addParameters(RequestType.requestType, dgsPriceChange);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.goods, goods);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsPurchase(Wallet wallet, String goods, long priceATM, int quantity, int deliveryDeadlineTimeInHours) {
        addParameters(RequestType.requestType, dgsPurchase);
        addParameters(Parameters.priceATM, priceATM);
        addParameters(Parameters.quantity, quantity);
        addParameters(Parameters.deliveryDeadlineTimestamp, deliveryDeadlineTimeInHours * 3600000);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.goods, goods);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsDelivery(Wallet wallet, String purchase, String delivery, int discountATM) {
        addParameters(RequestType.requestType, dgsDelivery);
        addParameters(Parameters.purchase, purchase);
        addParameters(Parameters.discountATM, discountATM);
        addParameters(Parameters.goodsToEncrypt, delivery);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsFeedback(Wallet wallet, String purchase, String message) {
        addParameters(RequestType.requestType, dgsFeedback);
        addParameters(Parameters.purchase, purchase);
        addParameters(Parameters.messageIsText, true);
        addParameters(Parameters.message, message);
        addParameters(Parameters.messageToEncrypt, message);
        addParameters(Parameters.messageIsText, true);
        addParameters(Parameters.messageIsPrunable, true);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse uploadTaggedData(Wallet wallet, String name, String description, String tags, String channel, File file) {
        addParameters(RequestType.requestType, uploadTaggedData);
        addParameters(Parameters.name, name);
        addParameters(Parameters.file, file);
        addParameters(Parameters.description, description);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.tags, tags);
        addParameters(Parameters.channel, channel);
        addParameters(Parameters.messageIsText, false);
        addParameters(Parameters.messageIsPrunable, true);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsRefund(Wallet wallet, String purchase, int refundATM, String message) {
        addParameters(RequestType.requestType, dgsRefund);
        addParameters(Parameters.purchase, purchase);
        addParameters(Parameters.refundATM, refundATM);
        addParameters(Parameters.message, message);
        addParameters(Parameters.wallet, wallet);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }


    @Step
    public DGSGoodsDTO getDGSGood(String goods) {
        addParameters(RequestType.requestType, getDGSGood);
        addParameters(Parameters.goods, goods);
        return getInstanse(DGSGoodsDTO.class);
    }

    @Step
    public AllTaggedDataResponse getAllTaggedData() {
        addParameters(RequestType.requestType, getAllTaggedData);
        return getInstanse(AllTaggedDataResponse.class);
    }

    @Step
    public TaggedDataDTO getTaggedData(String transaction) {
        addParameters(RequestType.requestType, getTaggedData);
        addParameters(Parameters.transaction, transaction);
        return getInstanse(TaggedDataDTO.class);
    }

    @Step
    public DataTagCountResponse getDataTagCount() {
        addParameters(RequestType.requestType, getDataTagCount);
        return getInstanse(DataTagCountResponse.class);
    }

    @Step
    public AllTaggedDataResponse searchTaggedDataByName(String query) {
        addParameters(RequestType.requestType, searchTaggedData);
        addParameters(Parameters.query, query);
        return getInstanse(AllTaggedDataResponse.class);
    }

    @Step
    public AllTaggedDataResponse searchTaggedDataByTag(String tag) {
        addParameters(RequestType.requestType, searchTaggedData);
        addParameters(Parameters.tag, tag);
        return getInstanse(AllTaggedDataResponse.class);
    }

    @Step
    public CreateTransactionResponse extendTaggedData(Wallet wallet, String transaction) {
        addParameters(RequestType.requestType, extendTaggedData);
        addParameters(Parameters.transaction, transaction);
        addParameters(Parameters.feeATM, "100000000000");
        addParameters(Parameters.deadline, 1440);
        return getInstanse(CreateTransactionResponse.class);
    }

    @Step
    public PollVotesResponse getPollVotes(String poll) {
        addParameters(RequestType.requestType, getPollVotes);
        addParameters(Parameters.poll, poll);
        return getInstanse(PollVotesResponse.class);
    }

    //get poll results
    @Step
    public PollResultResponse getPollResult(String poll) {
        addParameters(RequestType.requestType, getPollResult);
        addParameters(Parameters.poll, poll);
        return getInstanse(PollResultResponse.class);
    }

    @Step
    public void messagePrunable() {
        String message = RandomStringUtils.randomAlphabetic(3, 5);
        addParameters(Parameters.messageIsPrunable, true);
        addParameters(Parameters.messageIsText, true);
        addParameters(Parameters.messageToEncrypt, message);
        addParameters(Parameters.encryptedMessageIsPrunable, true);
        addParameters(Parameters.compressMessageToEncrypt, message);
        addParameters(Parameters.messageToEncryptToSelf, message);
        addParameters(Parameters.messageToEncryptToSelfIsText, true);
    }

}
