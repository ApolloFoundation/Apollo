package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.Currency;
import com.apollocurrency.aplwallet.api.dto.DGSGoodsDTO;
import com.apollocurrency.aplwallet.api.dto.DexOrderDto;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.dto.PeerDTO;
import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.dto.TradingDataOutputDTO;
import com.apollocurrency.aplwallet.api.p2p.PeerInfo;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
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
import com.apollocurrency.aplwallet.api.response.AllShufflingsResponse;
import com.apollocurrency.aplwallet.api.response.AllTaggedDataResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateDexOrderResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.CurrenciesResponse;
import com.apollocurrency.aplwallet.api.response.CurrencyAccountsResponse;
import com.apollocurrency.aplwallet.api.response.DataTagCountResponse;
import com.apollocurrency.aplwallet.api.response.DexAccountInfoResponse;
import com.apollocurrency.aplwallet.api.response.EthGasInfoResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrency.aplwallet.api.response.FilledOrdersResponse;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountBlockCountResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrency.aplwallet.api.response.GetPeersIpResponse;
import com.apollocurrency.aplwallet.api.response.PollResultResponse;
import com.apollocurrency.aplwallet.api.response.PollVotesResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrency.aplwallet.api.response.ShufflingParticipantsResponse;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.jodah.failsafe.Failsafe;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class TestBaseNew extends TestBase {
    private final int ETH = 1;
    private final int PAX = 2;
    private final int ORDER_SELL = 1;
    private final int ORDER_BUY = 0;
    String path = "/apl";

    @Step
    @DisplayName("Get Transaction")
    public TransactionDTO getTransaction(String transaction) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_TRANSACTION);
        param.put(ReqParam.TRANSACTION, transaction);

        return given()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", TransactionDTO.class);
    }

    @Step
    public BlockListInfoResponse getAccountBlocks(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_BLOCKS);
        param.put(ReqParam.ACCOUNT, account);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BlockListInfoResponse.class);
    }

    @Step
    public GetAccountResponse getAccount(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT);
        param.put(ReqParam.ACCOUNT, account);
        param.put(ReqParam.INCLUDE_EFFECTIVE_BALANCE, "true");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", GetAccountResponse.class);
    }

    @Step
    public GetAccountBlockCountResponse getAccountBlockCount(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_BLOCK_COUNT);
        param.put(ReqParam.ACCOUNT, account);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", GetAccountBlockCountResponse.class);
    }

    @Step
    public AccountBlockIdsResponse getAccountBlockIds(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_BLOCK_IDS);
        param.put(ReqParam.ACCOUNT, account);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountBlockIdsResponse.class);
    }

    @Step
    public AccountDTO getAccountId(String secretPhrase) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_ID);
        param.put(ReqParam.SECRET_PHRASE, secretPhrase);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountDTO.class);
    }


    @Step
    public Wallet getNewWallet(String secretPhrase) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_ID);
        param.put(ReqParam.SECRET_PHRASE, secretPhrase);

        AccountDTO accountDTO = given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountDTO.class);
        return new Wallet(accountDTO.getAccountRS(),secretPhrase,false);
    }

    @Step
    public AccountLedgerResponse getAccountLedger(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_LEDGER);
        param.put(ReqParam.FIRST_INDEX, "0");
        param.put(ReqParam.LAST_INDEX, "15");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountLedgerResponse.class);
    }

    @Step
    public AccountPropertiesResponse getAccountProperties(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_PROPERTIES);
        param.put(ReqParam.RECIPIENT, account);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountPropertiesResponse.class);
    }

    @Step
    public SearchAccountsResponse searchAccounts(String searchQuery) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.SEARCH_ACCOUNTS);
        param.put(ReqParam.QUERY,searchQuery);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", SearchAccountsResponse.class);
    }

    @Step
    public TransactionListResponse getUnconfirmedTransactions(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_UNCONFIRMED_TRANSACTIONS);
        param = restHelper.addWalletParameters(param,wallet);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", TransactionListResponse.class);
    }

    @Step
    public AccountTransactionIdsResponse getUnconfirmedTransactionIds(String account) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_UNCONFIRMED_TRANSACTION_IDS);
        param.put(ReqParam.ACCOUNT, account);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountTransactionIdsResponse.class);
    }

    @Step
    public BalanceDTO getGuaranteedBalance(String account, int confirmations) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_GUARANTEED_BALANCE);
        param.put(ReqParam.ACCOUNT, account);
        param.put(ReqParam.NUMBER_OF_CONFIRMATIONS, String.valueOf(confirmations));

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BalanceDTO.class);
    }

    @Step
    public BalanceDTO getBalance(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BALANCE);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BalanceDTO.class);
    }

    @Step
    public EntryDTO getAccountLedgerEntry(String ledgerId) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_LEDGER_ENTRY);
        param.put(ReqParam.LEDGER_ID, ledgerId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", EntryDTO.class);
    }

    @Step
    public CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SEND_MONEY);
        param.put(ReqParam.AMOUNT_ATM, moneyAmount +  "00000000");
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.FEE, "100000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountDTO getAccountPublicKey(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_PUBLIC_KEY);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountDTO.class);
    }

    @Step
    public BlockchainTransactionsResponse getAccountTransaction(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCKCHAIN_TRANSACTIONS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BlockchainTransactionsResponse.class);
    }

    @Step
    public CreateTransactionResponse setAccountInfo(Wallet wallet, String accountName, String accountDescription) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SET_ACCOUNT_INFO);
        param.put(ReqParam.NAME, accountName);
        param.put(ReqParam.DESCRIPTION, accountDescription);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse setAccountProperty(Wallet wallet, String property) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SET_ACCOUNT_PROPERTY);
        param.put(ReqParam.RECIPIENT, wallet.getUser());
        param.put(ReqParam.PROPERTY, property);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteAccountProperty(Wallet wallet, String property) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_ACCOUNT_PROPERTY);
        param.put(ReqParam.PROPERTY, property);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }


    @Step
    public AccountAliasesResponse getAliases(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALIASES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAliasesResponse.class);
    }

    @Step
    public AccountCountAliasesResponse getAliasCount(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALIAS_COUNT);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCountAliasesResponse.class);
    }

    @Step
    public AccountAliasDTO getAlias(String aliasName) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALIAS);
        param.put(ReqParam.ALIAS_NAME,aliasName);
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAliasDTO.class);
    }

    @Step
    public CreateTransactionResponse setAlias(Wallet wallet, String aliasURL, String aliasName) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        param.put(ReqType.REQUEST_TYPE, ReqType.SET_ALIAS);
        param.put(ReqParam.ALIAS_URI, aliasURL);
        param.put(ReqParam.ALIAS_NAME, aliasName);
        param.put(ReqParam.FEE, "1000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteAlias(Wallet wallet, String aliasName) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_ALIAS);
        param.put(ReqParam.ALIAS_NAME, aliasName);
        param.put(ReqParam.FEE, "1000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountAliasesResponse getAliasesLike(String aliaseName) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALIASES_LIKE);
        param.put(ReqParam.ALIAS_PREFIX,aliaseName);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAliasesResponse.class);
    }

    @Step
    public CreateTransactionResponse sellAlias(Wallet wallet, String aliasName, int price) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        param.put(ReqType.REQUEST_TYPE, ReqType.SELL_ALIAS);
        param.put(ReqParam.ALIAS_NAME, aliasName);
        param.put(ReqParam.PRICE, String.valueOf(price));
        param.put(ReqParam.FEE, "1000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse buyAlias(Wallet wallet, String aliasName, int price) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        param.put(ReqType.REQUEST_TYPE, ReqType.BUY_ALIAS);
        param.put(ReqParam.ALIAS_NAME, aliasName);
        param.put(ReqParam.AMOUNT_ATM, String.valueOf(price));
        param.put(ReqParam.FEE, "1000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse sendMoneyPrivate(Wallet wallet, String recipient, int moneyAmount) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SEND_MONEY_PRIVATE);
        param.put(ReqParam.AMOUNT_ATM, moneyAmount +  "00000000");
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step("Generate New Account")
    public Account2FAResponse generateNewAccount() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GENERATE_ACCOUNT);
        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .then()
                .assertThat().statusCode(200)
                .extract().body().jsonPath()
                .getObject("", Account2FAResponse.class);
    }

    @Step("Delete Secret File")
    public Account2FAResponse deleteSecretFile(Wallet wallet) throws JsonProcessingException {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_KEY);
        param.put(ReqParam.ACCOUNT, wallet.getUser());
        param.put(ReqParam.PASS_PHRASE, wallet.getPass());

        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .then()
                .assertThat().statusCode(200)
                .extract().body().jsonPath()
                .getObject("", Account2FAResponse.class);
    }

    @Step("Export Secret File")
    public VaultWalletResponse exportSecretFile(Wallet wallet) {
        String path = "/rest/keyStore/download";
        HashMap<String, String> param = new HashMap();
        param.put("account", wallet.getUser());
        param.put("passPhrase", wallet.getPass());
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path).as(VaultWalletResponse.class);

    }

    @Step("Import Secret File")
    public boolean importSecretFile(String pathToSecretFile, String pass) {
        String path = "/rest/keyStore/upload";
        Response response = given().log().all()
                .spec(restHelper.getSpec())
                .header("Content-Type", "multipart/form-data")
                .multiPart("keyStore", new File(pathToSecretFile))
                .formParam("passPhrase", pass)
                .when()
                .post(path);
        return !response.body().asString().contains("error");
    }

    @Step("Enable 2FA")
    public AccountDTO enable2FA(Wallet wallet) throws JsonProcessingException {
        HashMap<String, String> param = new HashMap();

        param.put(ReqType.REQUEST_TYPE, ReqType.ENABLE_2FA);
        param = restHelper.addWalletParameters(param, wallet);

        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .then()
                .assertThat().statusCode(200)
                .extract().body().jsonPath()
                .getObject("", AccountDTO.class);
    }

    @Step
    @DisplayName("Get All Peers")
    public List<String> getPeers() {
        String path = "/rest/networking/peer/all";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(GetPeersIpResponse.class).getPeers();

    }

    @Step
    @DisplayName("Get Peer")
    public PeerDTO getPeer(String peer) {
        String path = String.format("/rest/networking/peer?peer=%s", peer);
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerDTO.class);
    }

    @Step
    public PeerDTO addPeer(String ip) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.ADD_PEER);
        param.put(ReqParam.PEER, ip);
        param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", PeerDTO.class);
    }

    @Step("Get My Peer Info")
    public PeerInfo getMyInfo() {
        String path = "/rest/networking/peer/mypeerinfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerInfo.class);
    }

    @Step("Get Block")
    public BlockDTO getBlock(String block){
       HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCK);
        param.put(ReqParam.BLOCK, block);
        param.put(ReqParam.INCLUDE_TRANSACTIONS, "true");
        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .then()
                .assertThat().statusCode(200)
                .extract().body().jsonPath()
                .getObject("", BlockDTO.class);
    }

    @Step("Get Block")
    public BlockDTO getBlock() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCK);
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BlockDTO.class);
    }

    @Step("Get Last Block")
    public BlockDTO getLastBlock(String peer){
        String path = "/apl";

        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCK);

        return given()
                .baseUri(String.format("http://%s:%s", peer, TestConfiguration.getTestConfiguration().getPort()))
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .then()
                .assertThat().statusCode(200)
                .extract().body().jsonPath()
                .getObject("", BlockDTO.class);
    }

    //TODO add: boolean isAvailableForNow, int minAskPrice, int maxBidPrice
    @Step("Get Dex Orders with param: Type {orderType}, Pair Currency {pairCurrency}, Order Status {status}, AccountId {accountId}")
    public List<DexOrderDto> getDexOrders(String orderType, String pairCurrency, String status, String accountId) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ORDER_TYPE, orderType);
        param.put(ReqParam.PAIR_CURRENCY, pairCurrency);
        param.put(ReqParam.STATUS, status);
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Orders with param: Order Status {status}, AccountId {accountId}")
    public List<DexOrderDto> getDexOrders(String status, String accountId) {
        String path = "/rest/dex/offers";
        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.STATUS, status);
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .formParams(param)
            .when()
            .get(path)
            .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step
    public List<DexOrderDto> getDexOrders(String accountId) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, accountId);

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Orders")
    public List<DexOrderDto> getDexOrders() {
        String path = "/rest/dex/offers";
        return given().log().all()
                .spec(restHelper.getSpec())
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex Order")
    public DexOrderDto getDexOrder(String orderId) {
        HashMap<String, String> param = new HashMap();
        param.put("orderId", orderId);

        String path = "/rest/dex/orders/" + orderId;
        return given().log().all()
            .spec(restHelper.getSpec())
            .when()
            .get(path)
            .as(DexOrderDto.class);
    }


    @Step("Get Dex History (CLOSED ORDERS) with param: Account: {0}, Pair: {1} , Type: {2}")
    public List<DexOrderDto> getDexHistory(String account, boolean isEth, boolean isSell) {
        String path = "/rest/dex/offers";


        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, account);
        param.put(ReqParam.STATUS, "5");

        int pair = (isEth)? ETH : PAX;
        int orderType = (isSell)? ORDER_SELL : ORDER_BUY;

        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pair));
        param.put(ReqParam.ORDER_TYPE, String.valueOf(orderType));

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Dex History (CLOSED ORDERS) for certain account")
    public List<DexOrderDto> getDexHistory(String account) {
        String path = "/rest/dex/offers";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ACCOUNT_ID, account);
        param.put(ReqParam.STATUS, "5");

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Step("Get Eth Gas Info")
    public EthGasInfoResponse getEthGasInfo() {
        String path = "/rest/dex/ethInfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(EthGasInfoResponse.class);
    }

    @Step("Get history for certain Currency and period")
    public TradingDataOutputDTO getDexTradeInfo(boolean isEth, String resolution) {
        String path = "/rest/dex/history";
        HashMap<String, String> param = new HashMap();
        Date today = Calendar.getInstance().getTime();
        long epochTime = today.getTime();

        param.put(ReqParam.RESOLUTION, resolution);
        param.put(ReqParam.FROM, String.valueOf(epochTime/1000-864000));
        param.put(ReqParam.TO, String.valueOf(epochTime/1000));
        log.info("start = " + (epochTime/1000-864000));
        log.info("finish = " + (epochTime/1000));

        String pair = (isEth)? "APL_ETH" : "APL_PAX";
        param.put(ReqParam.SYMBOL, pair);

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().as(TradingDataOutputDTO.class);
    }

    //TODO: edit to new RESPONSEDTO, not STRING
    @Step("dexGetBalances endpoint returns cryptocurrency wallets' (ETH/PAX) balances")
    public Account2FAResponse getDexBalances(String ethAddress) {
        String path = "/rest/dex/balance";

        HashMap<String, String> param = new HashMap();
        param.put(ReqParam.ETH, ethAddress);

        return given().log().all()
                .spec(restHelper.getSpec())
                .formParams(param)
                .when()
                .get(path)
                .getBody().as(Account2FAResponse.class);
    }

    @Step
    public WithdrawResponse dexWidthraw(String fromAddress, Wallet wallet, String toAddress, String amount, String transferFee, boolean isEth) {
        String path = "/rest/dex/withdraw";

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqParam.FROM_ADDRESS, fromAddress);
        param.put(ReqParam.TO_ADDRESS, toAddress);
        param.put(ReqParam.AMOUNT, amount);
        param.put(ReqParam.TRANSFER_FEE, transferFee);
        final int eth = 1;
        final int pax = 2;

        int cryptocurrency = (isEth)? eth : pax;
        param.put(ReqParam.CRYPTO_CURRENCY, String.valueOf(cryptocurrency));

        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path)
                .as(WithdrawResponse.class);

    }

    @Step
    public CreateTransactionResponse dexCancelOrder(String orderId, Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        String path = "/rest/dex/offer/cancel";

        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqParam.ORDER_ID, orderId);
        param.put(ReqParam.FEE, "100000000");

        return given().log().all()
                .spec(restHelper.getSpec())
                .contentType(ContentType.URLENC)
                .formParams(param)
                .when()
                .post(path).as(CreateTransactionResponse.class);
    }

    @Step
    public CreateDexOrderResponse createDexOrder(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth) {
        String path = "/rest/dex/offer";
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        int offerType = (isBuyOrder) ? ORDER_BUY : ORDER_SELL;
        int pairCurrency = (isEth) ? ETH : PAX;

        param.put(ReqParam.OFFER_TYPE, String.valueOf(offerType));
        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pairCurrency));

        param.put(ReqParam.PAIR_RATE, pairRate);
        param.put(ReqParam.OFFER_AMOUNT, offerAmount + "000000000");
        param.put(ReqParam.ETH_WALLET_ADDRESS, wallet.getEthAddress());
        param.put(ReqParam.AMOUNT_OF_TIME, "86400");
        param.put(ReqParam.FEE, "200000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .as(CreateDexOrderResponse.class);
    }


    @Step
    public CreateDexOrderResponse createDexOrderWithAmountOfTime(String pairRate, String offerAmount, Wallet wallet, boolean isBuyOrder, boolean isEth, String amountOfTime) {
        String path = "/rest/dex/offer";
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        int offerType = (isBuyOrder) ? ORDER_BUY : ORDER_SELL;
        int pairCurrency = (isEth) ? ETH : PAX;

        param.put(ReqParam.OFFER_TYPE, String.valueOf(offerType));
        param.put(ReqParam.PAIR_CURRENCY, String.valueOf(pairCurrency));

        param.put(ReqParam.PAIR_RATE, pairRate);
        param.put(ReqParam.OFFER_AMOUNT, offerAmount + "000000000");
        param.put(ReqParam.ETH_WALLET_ADDRESS, wallet.getEthAddress());
        param.put(ReqParam.AMOUNT_OF_TIME, amountOfTime);
        param.put(ReqParam.FEE, "200000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path).as(CreateDexOrderResponse.class);
    }


    @Step
    public List<FilledOrdersResponse> getFilledOrders(){
        String path = "/rest/dex/eth/filled-orders";
        return given().log().all()
            .spec(restHelper.getSpec())
            .when().log().body()
            .get(path)
            .getBody().jsonPath().getList("", FilledOrdersResponse.class);
    }

    @Step
    public List<FilledOrdersResponse> getActiveDeposits(){
        String path = "/rest/dex/eth/active-deposits";
        return given().log().all()
            .spec(restHelper.getSpec())
            .when().log().body()
            .get(path)
            .getBody().jsonPath().getList("", FilledOrdersResponse.class);
    }

    @Step
    public DexAccountInfoResponse logInDex (Wallet wallet){
        String path = "/rest/keyStore/accountInfo";

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param, wallet);
        param.put(ReqParam.ACCOUNT, wallet.getAccountId());
        param.put(ReqParam.PASS_PHRASE, wallet.getPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .log().body()
            .post(path)
            .as(DexAccountInfoResponse.class);
    }

    @Step
    public GetBlockIdResponse getBlockId(String height) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCK_ID);
        param.put(ReqParam.HEIGHT, height);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", GetBlockIdResponse.class);
    }

    @Step
    public BlockchainInfoDTO getBlockchainStatus() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCKCHAIN_STATUS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", BlockchainInfoDTO.class);
    }

    @Step
    public AccountBlocksResponse getBlocks() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BLOCKS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountBlocksResponse.class);
    }

    @Step
    public void verifyCreatingTransaction(CreateTransactionResponse transaction) {
        assertAll(
            ()->assertNotNull(transaction),
            ()->assertNotNull(transaction.getTransactionJSON()),
            ()->assertNotNull(transaction.getTransactionJSON().getSenderPublicKey()),
            ()->assertNotNull(transaction.getTransactionJSON().getSignature()),
            ()->assertNotNull(transaction.getTransactionJSON().getFullHash()),
            ()->assertNotNull(transaction.getTransactionJSON().getAmountATM()),
            ()->assertNotNull(transaction.getTransactionJSON().getEcBlockId()),
            ()->assertNotNull(transaction.getTransactionJSON().getSenderRS()),
            ()->assertNotNull(transaction.getTransactionJSON().getTransaction()),
            ()->assertNotNull(transaction.getTransactionJSON().getFeeATM()),
            ()->assertNotNull(transaction.getTransactionJSON().getType())
        );
    }

    @Step
    public CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.ISSUE_ASSET);
        param.put(ReqParam.NAME, assetName);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.QUANTITY_ATU, String.valueOf(quantityATU));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.PLACE_BID_ORDER);
        param.put(ReqParam.ASSET, assetID);
        param.put(ReqParam.PRICE, priceATM);
        param.put(ReqParam.QUANTITY_ATU, String.valueOf(quantityATU));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.PLACE_ASK_ORDER);
        param.put(ReqParam.ASSET, assetID);
        param.put(ReqParam.PRICE, priceATM);
        param.put(ReqParam.QUANTITY_ATU, String.valueOf(quantityATU));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CANCEL_BID_ORDER);
        param.put(ReqParam.ORDER, bidOrder);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CANCEL_ASK_ORDER);
        param.put(ReqParam.ORDER, askOrder);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_ASSET_SHARES);
        param.put(ReqParam.ASSET, assetID);
        param.put(ReqParam.QUANTITY_ATU, String.valueOf(quantityATU));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DIVIDEND_PAYMENT);
        param.put(ReqParam.ASSET, assetID);
        param.put(ReqParam.AMOUNT_ATM_PER_ATU, String.valueOf(amountATMPerATU));
        param.put(ReqParam.HEIGHT, String.valueOf(height));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountAssetsResponse getAccountAssets(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_ASSETS);
        param.put(ReqParam.ACCOUNT, wallet.getUser());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetsResponse.class);
    }

    @Step
    public AccountAssetDTO getAccountAssets(Wallet wallet, String asset) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_ASSETS);
        param.put(ReqParam.ACCOUNT, wallet.getUser());
        param.put(ReqParam.ASSET, asset);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then().log().body()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetDTO.class);
    }

    @Step
    public AccountAssetsCountResponse getAccountAssetCount(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_ASSET_COUNT);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetsCountResponse.class);
    }

    @Step
    public AccountAssetDTO getAsset(String asset) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASSET);
        param.put(ReqParam.ASSET, asset);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetDTO.class);
    }

    @Step
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENT_ASK_ORDER_IDS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetAskOrderIdsResponse.class);
    }

    @Step
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENT_BID_ORDER_IDS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetBidOrderIdsResponse.class);
    }

    @Step
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENT_ASK_ORDERS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetAskOrdersResponse.class);
    }

    @Step
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENT_BID_ORDERS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetBidOrdersResponse.class);
    }

    @Step
    public AssetsResponse getAllAssets() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_ASSETS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AssetsResponse.class);
    }

    @Step
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_OPEN_ASK_ORDERS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountOpenAssetOrdersResponse.class);
    }

    @Step
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_OPEN_BID_ORDERS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountOpenAssetOrdersResponse.class);
    }

    @Step
    public AssetTradeResponse getAllTrades() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_TRADES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AssetTradeResponse.class);
    }

    @Step
    public AccountAssetOrderDTO getAskOrder(String askOrder) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASK_ORDER);
        param.put(ReqParam.ORDER, askOrder);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetOrderDTO.class);
    }

    @Step
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASK_ORDER_IDS);
        param.put(ReqParam.ASSET, assetID);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetAskOrderIdsResponse.class);
    }

    @Step
    public AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASK_ORDERS);
        param.put(ReqParam.ASSET, assetID);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetAskOrdersResponse.class);
    }

    @Step
    public AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_BID_ORDERS);
        param.put(ReqParam.ASSET, assetID);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrentAssetBidOrdersResponse.class);
    }

    @Step
    public AssetsAccountsCountResponse getAssetAccountCount(String assetID) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASSET_ACCOUNT_COUNT);
        param.put(ReqParam.ASSET, assetID);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AssetsAccountsCountResponse.class);
    }

    @Step
    public AccountAssetsResponse getAssetAccounts(String assetID) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASSET_ACCOUNTS);
        param.put(ReqParam.ASSET, assetID);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetsResponse.class);
    }

    @Step
    public ExpectedAssetDeletes getAssetDeletes(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASSET_DELETES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ExpectedAssetDeletes.class);
    }

    @Step
    public ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_EXPECTED_ASSET_DELETES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ExpectedAssetDeletes.class);
    }

    @Step
    public AccountAssetsIdsResponse getAssetIds() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ASSET_IDS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountAssetsIdsResponse.class);
    }

    @Step
    public CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.TRANSFER_ASSET);
        param.put(ReqParam.ASSET, asset);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.QUANTITY_ATU, String.valueOf(quantityATU));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public ECBlockDTO getECBlock() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_EC_BLOCK);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ECBlockDTO.class);
    }

    @Step("Get Forging")
    public ForgingResponse getForging() {
        String path = "/rest/nodeinfo/forgers";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(ForgingResponse.class);
    }

    @Step("Get Shards from peer")
    public List<ShardDTO> getShards(String ip) {
        String path = "/rest/shards";
        return given().log().uri()
                .contentType(ContentType.JSON)
                .baseUri(String.format("http://%s:%s", ip, 7876))
                .when()
                .get(path).getBody().jsonPath().getList("", ShardDTO.class);
    }

    @Step
    public ForgingDetails startForging(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.START_FORGING);
        param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ForgingDetails.class);
    }

    @Step
    public ForgingDetails stopForging(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.STOP_FORGING);
        param.put(ReqParam.ADMIN_PASSWORD, getTestConfiguration().getAdminPass());

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ForgingDetails.class);
    }

    @Step
    public CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage) {
        HashMap<String, String> param = new HashMap();

        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SEND_MESSAGE);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.MESSAGE, testMessage);
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");;

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountMessageDTO readMessage(Wallet wallet, String transaction) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.READ_MESSAGE);
        param.put(ReqParam.TRANSACTION, transaction);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountMessageDTO.class);
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

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.ISSUE_CURRENCY);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.CODE, code);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.TYPE, String.valueOf(type));
        param.put(ReqParam.INITIAL_SUPPLY, String.valueOf(initialSupply));
        param.put(ReqParam.DECIMALS, String.valueOf(decimals));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.ISSUANCE_HEIGHT, "0");
        param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply));
        param.put(ReqParam.RESERVE_SUPPLY, "0");

        if ((type & RESERVABLE) == RESERVABLE) {
            param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.RESERVE_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.ISSUANCE_HEIGHT, String.valueOf(issuanceHeight));
            param.put(ReqParam.MIN_RESERVE_PER_UNIT, String.valueOf(1));
        }
        if ((type & CLAIMABLE) == CLAIMABLE) {
            param.put(ReqParam.INITIAL_SUPPLY, "0");
        }
        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) == RESERVABLE) {
            param.put(ReqParam.ALGORITHM, "2");
            param.put(ReqParam.MIN_DIFFICULTY, "1");
            param.put(ReqParam.MAX_DIFFICULTY, "2");
            param.put(ReqParam.MAX_SUPPLY, String.valueOf(maxSupply+50));
            param.put(ReqParam.RESERVE_SUPPLY, String.valueOf(maxSupply+10));
        }

        if ((type & MINTABLE) == MINTABLE && (type & RESERVABLE) != RESERVABLE) {
            param.put(ReqParam.ALGORITHM, "2");
            param.put(ReqParam.MIN_DIFFICULTY, "1");
            param.put(ReqParam.MAX_DIFFICULTY, "2");
            param.put(ReqParam.RESERVE_SUPPLY, "0");
        }

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);


    }

    @Step
    public CurrenciesResponse getAllCurrencies() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_CURRENCIES);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CurrenciesResponse.class);
    }

    @Step
    public Currency getCurrency(String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_CURRENCY);
        param.put(ReqParam.CURRENCY, CurrencyId);
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", Currency.class);
    }

    @Step
    public PollDTO getPoll(String poll) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_POLL);
        param.put(ReqParam.POLL, poll);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", PollResultResponse.class);
    }

    @Step
    public CreateTransactionResponse createPoll(Wallet wallet, int votingModel, String name, int plusFinishHeight, String holding, int minBalance, int maxRangeValue) {
        final int POLL_BY_ACCOUNT = 0;
        final int POLL_BY_ACCOUNT_BALANCE = 1;
        final int POLL_BY_ASSET = 2;
        final int POLL_BY_CURRENCY = 3;

        int currentHeight = getBlock().getHeight();
        int finishHeight = currentHeight + plusFinishHeight;

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        param.put(ReqType.REQUEST_TYPE, ReqType.CREATE_POLL);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.FINISH_HEIGHT, String.valueOf(finishHeight));
        param.put(ReqParam.MIN_NUMBER_OF_OPTIONS, "1");
        param.put(ReqParam.MAX_NUMBER_OF_OPTIONS, "1");
        param.put(ReqParam.IS_CUSTOM_FEE, "true");
        param.put(ReqParam.MIN_RANGE_VALUE,"0");
        param.put(ReqParam.MAX_RANGE_VALUE, String.valueOf(maxRangeValue));
        param.put(ReqParam.ANSWERS, "YES");
        param.put(ReqParam.ANSWERS, "NO");
        param.put(ReqParam.ANSWERS, "MAYBE");
        param.put(ReqParam.CREATE_POLL_ANSWERS, "1");
        param.put(ReqParam.OPTION_0, "YES");
        param.put(ReqParam.OPTION_1, "NO");
        param.put(ReqParam.OPTION_2, "MAYBE");
        param.put(ReqParam.VOTING_MODEL, String.valueOf(votingModel));
        param.put(ReqParam.MIN_BALANCE_MODEL, String.valueOf(votingModel));
        param.put(ReqParam.MIN_BALANCE, String.valueOf(minBalance));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.HOLDING, "");


        switch (votingModel){
            case POLL_BY_ACCOUNT:
                param.put(ReqParam.MIN_BALANCE,"0");
                param.put(ReqParam.DESCRIPTION,"poll by account");
                break;
            case POLL_BY_ACCOUNT_BALANCE:
                param.put(ReqParam.DESCRIPTION,"poll by account balance");
                break;
            case POLL_BY_ASSET:
                param.put(ReqParam.DESCRIPTION,"poll by asset");
                param.put(ReqParam.HOLDING, holding);
                break;
            case POLL_BY_CURRENCY:
                param.put(ReqParam.DESCRIPTION,"poll by currency");
                param.put(ReqParam.HOLDING, holding);
                break;
        }


        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);

    }

    public CreateTransactionResponse castVote(Wallet wallet, String poll, int vote) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);

        param.put(ReqType.REQUEST_TYPE, ReqType.CAST_VOTE);
        param.put(ReqParam.VOTE_0, String.valueOf(vote));
        param.put(ReqParam.VOTE_1, "");
        param.put(ReqParam.VOTE_2, "");
        param.put(ReqParam.POLL, poll);
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CurrencyAccountsResponse getCurrencyAccounts(String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_CURRENCY_ACCOUNTS);
        param.put(ReqParam.CURRENCY, CurrencyId);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CurrencyAccountsResponse.class);
    }

    @Step
    public CreateTransactionResponse deleteCurrency(Wallet wallet, String CurrencyId) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DELETE_CURRENCY);
        param.put(ReqParam.CURRENCY, CurrencyId);
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse transferCurrency(String recipient, String currency, Wallet wallet, int units) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.TRANSFER_CURRENCY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveClaim(String currency, Wallet wallet, int units) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_RESERVE_CLAIM);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyReserveIncrease(String currency, Wallet wallet, int amountPerUnitATM) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_RESERVE_INCREASE);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.AMOUNT_PER_UNIT, String.valueOf(amountPerUnitATM));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse publishExchangeOffer(String currency, Wallet wallet, int buyRateATM, int sellRateATM, int initialBuySupply, int initialSellSupply) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.PUBLISH_EXCHANGE_OFFER);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.BUY_RATE, String.valueOf(buyRateATM));
        param.put(ReqParam.SELL_RATE, String.valueOf(sellRateATM));
        param.put(ReqParam.TOTAL_BUY_LIMIT, String.valueOf(1000));
        param.put(ReqParam.TOTAL_SELL_LIMIT, String.valueOf(1000));
        param.put(ReqParam.INITIAL_BUY_SUPPLY, String.valueOf(initialBuySupply));
        param.put(ReqParam.INITIAL_SELL_SUPPLY, String.valueOf(initialSellSupply));
        param.put(ReqParam.EXPIRATION_HEIGHT, "999999999");
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencySell(String currency, Wallet wallet, int units, int rate) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_SELL);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse currencyBuy(String currency, Wallet wallet, int units, int rate) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_BUY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse scheduleCurrencyBuy(String currency, Wallet wallet, int units, int rate, String offerIssuer) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.CURRENCY_BUY);
        param.put(ReqParam.CURRENCY, currency);
        param.put(ReqParam.RATE, String.valueOf(rate));
        param.put(ReqParam.UNITS, String.valueOf(units));
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AccountCurrencyResponse getAccountCurrencies(Wallet wallet) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ACCOUNT_CURRENCIES);
        param.put(ReqParam.ACCOUNT, wallet.getUser());
        param.put(ReqParam.INCLUDE_CURRENCY_INFO, "true");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AccountCurrencyResponse.class);
        }

    @Step
    public CreateTransactionResponse shufflingCreate(Wallet wallet, int registrationPeriod, int participantCount, int amount, String holding, int holdingType) {
        final int HOLDING_TYPE_BALANCE = 0;
        final int HOLDING_TYPE__ASSET = 1;
        final int HOLDING_TYPE__CURRENCY = 2;

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqParam.AMOUNT, String.valueOf(amount));
        param.put(ReqType.REQUEST_TYPE, ReqType.SHUFFLING_CREATE);
        param.put(ReqParam.DEADLINE, "1440");
        param.put(ReqParam.FEE, "100000000000");

        if (holdingType != HOLDING_TYPE_BALANCE){
            param.put(ReqParam.HOLDING, String.valueOf(holding));
            param.put(ReqParam.HOLDING_TYPE, String.valueOf(holdingType));
        }else {
            param.put(ReqParam.AMOUNT,amount+"00000000");
        }

        param.put(ReqParam.REGISTRATION_PERIOD, String.valueOf(registrationPeriod));
        param.put(ReqParam.PARTICIPANT_COUNT, String.valueOf(participantCount));

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then().log().body()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public PollVotesResponse getPollVotes(String poll) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_POLL_VOTES);
        param.put(ReqParam.POLL,poll);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", PollVotesResponse.class);
    }

    @Step
    public PollResultResponse getPollResult(String poll) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_POLL_RESULT);
        param.put(ReqParam.POLL,poll);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", PollResultResponse.class);
    }

    @Step
    public CreateTransactionResponse uploadTaggedData(Wallet wallet, String name, String description, String tags, String channel, File file) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.UPLOAD_TAGGED_DATA);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.TAGS, tags);
        param.put(ReqParam.CHANNEL,channel);
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
        param.put(ReqParam.MESSAGE_IS_TEXT, "false");
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .multiPart(ReqParam.FILE,file)
            .queryParams(param)
            .header("Content-Type","multipart/form-data")
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public AllTaggedDataResponse getAllTaggedData() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_TAGGED_DATA);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AllTaggedDataResponse.class);
    }

    @Step
    public TaggedDataDTO getTaggedData(String transaction) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_TAGGED_DATA);
        param.put(ReqParam.TRANSACTION,transaction);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", TaggedDataDTO.class);
    }

    @Step
    public DataTagCountResponse getDataTagCount() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_DATA_TAG_COUNT);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", DataTagCountResponse.class);
    }

    @Step
    public AllTaggedDataResponse searchTaggedDataByName(String query) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.SEARCH_TAGGED_DATA);
        param.put(ReqParam.QUERY, query);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AllTaggedDataResponse.class);
    }

    @Step
    public AllTaggedDataResponse searchTaggedDataByTag(String tag) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.SEARCH_TAGGED_DATA);
        param.put(ReqParam.TAG, tag);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AllTaggedDataResponse.class);
    }

    @Step
    public CreateTransactionResponse extendTaggedData(Wallet wallet, String transaction) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.EXTEND_TAGGED_DATA);
        param.put(ReqParam.TRANSACTION, transaction);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public boolean waitForHeight(int height){
        log.info("Wait For Height: {}", height);
        boolean isHeight = false;
        try {
            isHeight = Failsafe.with(retryPolicy).get(() -> getBlock().getHeight() >= height);
        } catch (Exception e) {
            fail(String.format("Height %s  not reached. Exception msg: %s", height, e.getMessage()));
        }
        assertTrue(isHeight, String.format("Height %s not reached: %s", height, getBlock().getHeight()));
        return isHeight;
    }

    @Step
    public CreateTransactionResponse dgsDelisting(Wallet wallet, String transaction) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_DELISTING);
        param.put(ReqParam.GOODS, transaction);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }
    @Step
    public CreateTransactionResponse dgsPriceChange(Wallet wallet, String transaction, int newPrice) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_PRICE_CHANGE);
        param.put(ReqParam.GOODS, transaction);
        param.put(ReqParam.PRICE, String.valueOf(newPrice));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }
    @Step
    public CreateTransactionResponse dgsQuantityChange(Wallet wallet, String transaction, int deltaQuantity) {

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_QUANTITY_CHANGE);
        param.put(ReqParam.GOODS, transaction);
        param.put(ReqParam.DELTA_QUANTITY, String.valueOf(deltaQuantity));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }
    @Step
    public DGSGoodsDTO getDGSGood(String transaction) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_DGS_GOOD);
        param.put(ReqParam.GOODS,transaction);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", DGSGoodsDTO.class);
    }
    @Step
    public CreateTransactionResponse dgsListing(Wallet wallet, String name, String description, String tags, int quantity, int priceATM, File image) {

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_LISTING);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.TAGS, tags);
        param.put(ReqParam.QUANTITY, String.valueOf(quantity));
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
        param.put(ReqParam.MESSAGE_IS_TEXT, "false");
        param.put(ReqParam.PRICE, String.valueOf(priceATM));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .multiPart(ReqParam.MESSAGE_FILE,image)
            .queryParams(param)
            .header("Content-Type","multipart/form-data")
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }
    @Step
    public CreateTransactionResponse dgsRefund(Wallet wallet, String purchase, int refundATM, String message) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_REFUND);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.REFUND, String.valueOf(refundATM));
        param.put(ReqParam.MESSAGE, message);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsFeedback(Wallet wallet, String purchase, String message) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_FEEDBACK);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.MESSAGE_IS_TEXT, "true");
        param.put(ReqParam.MESSAGE, message);
        param.put(ReqParam.MESSAGE_TO_ENCRYPT, message);
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse dgsDelivery(Wallet wallet, String purchase, String delivery, int discountATM) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_DELIVERY);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.DISCOUNT, String.valueOf(discountATM));
        param.put(ReqParam.GOODS_TO_ENCRYPT, delivery);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }
    @Step
    public CreateTransactionResponse dgsPurchase(Wallet wallet, String goods, long priceATM, int quantity, int deliveryDeadlineTimeInHours) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_PURCHASE);
        param.put(ReqParam.GOODS, goods);
        param.put(ReqParam.PRICE, String.valueOf(priceATM));
        param.put(ReqParam.QUANTITY, String.valueOf(quantity));
        param.put(ReqParam.DELIVERY_DEADLINE_TIMESTAMP, String.valueOf(deliveryDeadlineTimeInHours * 3600000));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }


    @Step
    public CreateTransactionResponse leaseBalance(String recipient, Wallet wallet){
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.LEASE_BALANCE);
        param.put(ReqParam.RECIPIENT, recipient);
        param.put(ReqParam.PERIOD, "1500");
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }


    @Step
    public CreateTransactionResponse shufflingRegister(Wallet wallet, String shufflingFullHash) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SHUFFLING_REGISTER);
        param.put(ReqParam.SHUFFLING_FULL_HASH, shufflingFullHash);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse shufflingProcess(Wallet wallet, String shuffling, String recipientSecretPhrase) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SHUFFLING_PROCESS);
        param.put(ReqParam.SHUFFLING, shuffling);
        param.put(ReqParam.RECIPIENT_SECRET_PHRASE, recipientSecretPhrase);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public CreateTransactionResponse shufflingVerify(Wallet wallet, String shuffling, String shufflingStateHash) {
        HashMap<String, String> param = new HashMap();

        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SHUFFLING_VERIFY);
        param.put(ReqParam.SHUFFLING, shuffling);
        param.put(ReqParam.SHUFFLING_STATE_HASH, shufflingStateHash);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then().log().body()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);
    }

    @Step
    public ShufflingDTO getShuffling(String shuffling) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_SHUFFLING);
        param.put(ReqParam.SHUFFLING,shuffling);
        param.put(ReqParam.INCLUDE_HOLDING_INFO,"false");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then().log().body()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ShufflingDTO.class);
    }

    @Step
    public CreateTransactionResponse shufflingCancel(Wallet wallet, String shuffling, String cancellingAccount, String shufflingStateHash) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SHUFFLING_CANCEL);
        param.put(ReqParam.SHUFFLING, shuffling);
        param.put(ReqParam.SHUFFLING_STATE_HASH, shufflingStateHash);
        if (cancellingAccount != null) {
            param.put(ReqParam.CANCELLING_ACCOUNT, cancellingAccount);
        }
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);

    }

    @Step
    public AllShufflingsResponse getAllShufflings() {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_ALL_SHUFFLINGS);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", AllShufflingsResponse.class);
    }

    @Step
    public ShufflingParticipantsResponse getShufflingParticipants(String shuffling) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_SHUFFLING_PARTICIPANTS);
        param.put(ReqParam.SHUFFLING, shuffling);
        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", ShufflingParticipantsResponse.class);
    }

    @Step
    public CreateTransactionResponse startShuffler(Wallet wallet, String shufflingFullHash, String recipientSecretPhrase) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.START_SHUFFLER);
        param.put(ReqParam.RECIPIENT_SECRET_PHRASE, recipientSecretPhrase);
        param.put(ReqParam.SHUFFLING_FULL_HASH, shufflingFullHash);
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .post(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", CreateTransactionResponse.class);

    }

}
