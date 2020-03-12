package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.AccountDTO;
import com.apollocurrency.aplwallet.api.dto.BalanceDTO;
import com.apollocurrency.aplwallet.api.dto.EntryDTO;
import com.apollocurrency.aplwallet.api.dto.ForgingDetails;
import com.apollocurrency.aplwallet.api.response.Account2FAResponse;
import com.apollocurrency.aplwallet.api.response.AccountBlockIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountLedgerResponse;
import com.apollocurrency.aplwallet.api.response.AccountPropertiesResponse;
import com.apollocurrency.aplwallet.api.response.AccountTransactionIdsResponse;
import com.apollocurrency.aplwallet.api.response.BlockListInfoResponse;
import com.apollocurrency.aplwallet.api.response.BlockchainTransactionsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ForgingResponse;
import com.apollocurrency.aplwallet.api.response.GetAccountResponse;
import com.apollocurrency.aplwallet.api.response.SearchAccountsResponse;
import com.apollocurrency.aplwallet.api.response.TransactionListResponse;
import com.apollocurrency.aplwallet.api.response.VaultWalletResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.util.HashMap;

import static com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration.getTestConfiguration;
import static io.restassured.RestAssured.given;

public class AccountSteps extends StepBase {



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
    @DisplayName("Get account blocks: {0}")
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
    @DisplayName("Get account: {0}")
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


    @Step("Delete Secret File")
    public Account2FAResponse deleteSecretFile(Wallet wallet) {
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

    @Step("Get Forging")
    public ForgingResponse getForging() {
        String path = "/rest/nodeinfo/forgers";
        return given().log().uri()
            .spec(restHelper.getSpec())
            .when()
            .get(path).as(ForgingResponse.class);
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
