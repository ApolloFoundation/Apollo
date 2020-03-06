package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.AccountMessageDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.BlockchainInfoDTO;
import com.apollocurrency.aplwallet.api.dto.Currency;
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
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.jodah.failsafe.Failsafe;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.DisplayName;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Calendar;

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
        return given().log().all()
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    @Step
    public CreateTransactionResponse sendMoney(Wallet wallet, String recipient, int moneyAmount) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.SEND_MONEY);
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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


    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    @Step
    @DisplayName("Get All Peers")
    public List<String> getPeers() {
        String path = "/rest/networking/peer/all";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(GetPeersIpResponse.class).getPeers();

    }

    @Override
    @Step
    @DisplayName("Get Peer")
    public PeerDTO getPeer(String peer) {
        String path = String.format("/rest/networking/peer?peer=%s", peer);
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerDTO.class);
    }

    @Override
    public PeerDTO addPeer(String ip) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    @Step("Get My Peer Info")
    public PeerInfo getMyInfo() {
        String path = "/rest/networking/peer/mypeerinfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(PeerInfo.class);
    }

    @Override
    @Step("Get Block")
    public BlockDTO getBlock(String block){
        String path = "/apl";

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
    @Override
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

    @Override
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

    @Override
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

    @Override
    @Step("Get Dex Orders")
    public List<DexOrderDto> getDexOrders() {
        String path = "/rest/dex/offers";
        return given().log().all()
                .spec(restHelper.getSpec())
                .when()
                .get(path)
                .getBody().jsonPath().getList("", DexOrderDto.class);
    }

    @Override
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


    @Override
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

    @Override
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

    @Override
    @Step("Get Eth Gas Info")
    public EthGasInfoResponse getEthGasInfo() {
        String path = "/rest/dex/ethInfo";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(EthGasInfoResponse.class);
    }

    @Override
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
    @Override
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

    @Override
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

    @Override
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

    @Override
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
            .post(path).as(CreateDexOrderResponse.class);
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

    @Override
    public GetBlockIdResponse getBlockId(String height) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public BlockchainInfoDTO getBlockchainStatus() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountBlocksResponse getBlocks() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
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

    @Override
    public CreateTransactionResponse issueAsset(Wallet wallet, String assetName, String description, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse placeBidOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse placeAskOrder(Wallet wallet, String assetID, String priceATM, Integer quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse cancelBidOrder(Wallet wallet, String bidOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse cancelAskOrder(Wallet wallet, String askOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse deleteAssetShares(Wallet wallet, String assetID, String quantityATU) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse dividendPayment(Wallet wallet, String assetID, Integer amountATMPerATU, Integer height) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsResponse getAccountAssets(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsCountResponse getAccountAssetCount(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetDTO getAsset(String asset) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAccountCurrentAskOrderIds(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrderIdsResponse getAccountCurrentBidOrderIds(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAccountCurrentAskOrders(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getAccountCurrentBidOrders(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetsResponse getAllAssets() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenAskOrders() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountOpenAssetOrdersResponse getAllOpenBidOrders() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetTradeResponse getAllTrades() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetOrderDTO getAskOrder(String askOrder) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrderIdsResponse getAskOrderIds(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetAskOrdersResponse getAskOrders(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrentAssetBidOrdersResponse getBidOrders(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AssetsAccountsCountResponse getAssetAccountCount(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsResponse getAssetAccounts(String assetID) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ExpectedAssetDeletes getAssetDeletes(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ExpectedAssetDeletes getExpectedAssetDeletes(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountAssetsIdsResponse getAssetIds() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse transferAsset(Wallet wallet, String asset, Integer quantityATU, String recipient) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ECBlockDTO getECBlock() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    @Step("Get Forging")
    public ForgingResponse getForging() {
        String path = "/rest/nodeinfo/forgers";
        return given().log().uri()
                .spec(restHelper.getSpec())
                .when()
                .get(path).as(ForgingResponse.class);
    }

    @Override
    @Step("Get Shards from peer")
    public List<ShardDTO> getShards(String ip) {
        String path = "/rest/shards";
        return given().log().uri()
                .contentType(ContentType.JSON)
                .baseUri(String.format("http://%s:%s", ip, 7876))
                .when()
                .get(path).getBody().jsonPath().getList("", ShardDTO.class);
    }

    @Override
    public ForgingDetails startForging(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ForgingDetails stopForging(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse sendMessage(Wallet wallet, String recipient, String testMessage) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountMessageDTO readMessage(Wallet wallet, String transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void createPhasingVote(long phasingFinishHeight, Parameters votingModel, int phasingQuorum, Long phasingMinBalance, Long phasingMinBalanceModel, String phasingHolding) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse issueCurrency(Wallet wallet, int type, String name, String description, String code, int initialSupply, int maxSupply, int decimals) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CurrenciesResponse getAllCurrencies() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public Currency getCurrency(String CurrencyId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public PollDTO getPoll(String poll) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse createPoll(Wallet wallet, int votingModel, String name, int plusFinishHeight, String holding, int minBalance, int maxRangeValue) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse castVote(Wallet wallet, String poll, int vote) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CurrencyAccountsResponse getCurrencyAccounts(String CurrencyId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse deleteCurrency(Wallet wallet, String CurrencyId) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse transferCurrency(String recipient, String currency, Wallet wallet, int units) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse currencyReserveClaim(String currency, Wallet wallet, int units) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse currencyReserveIncrease(String currency, Wallet wallet, int amountPerUnitATM) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse publishExchangeOffer(String currency, Wallet wallet, int buyRateATM, int sellRateATM, int initialBuySupply, int initialSellSupply) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse currencySell(String currency, Wallet wallet, int units, int rate) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse currencyBuy(String currency, Wallet wallet, int units, int rate) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse scheduleCurrencyBuy(String currency, Wallet wallet, int units, int rate, String offerIssuer) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AccountCurrencyResponse getAccountCurrencies(Wallet wallet) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse shufflingCreate(Wallet wallet, int registrationPeriod, int participantCount, int amount, String holding, int holdingType) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public PollVotesResponse getPollVotes(String poll) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public PollResultResponse getPollResult(String poll) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse uploadTaggedData(Wallet wallet, String name, String description, String tags, String channel, File file) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AllTaggedDataResponse getAllTaggedData() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public TaggedDataDTO getTaggedData(String transaction) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public DataTagCountResponse getDataTagCount() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AllTaggedDataResponse searchTaggedDataByName(String query) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public AllTaggedDataResponse searchTaggedDataByTag(String tag) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public CreateTransactionResponse extendTaggedData(Wallet wallet, String transaction) {
        throw new NotImplementedException("Not implemented");
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

}
