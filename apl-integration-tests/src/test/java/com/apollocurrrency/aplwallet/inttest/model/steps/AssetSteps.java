package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.AccountAssetDTO;
import com.apollocurrency.aplwallet.api.dto.AccountAssetOrderDTO;
import com.apollocurrency.aplwallet.api.response.AccountAssetsCountResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountAssetsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetAskOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrderIdsResponse;
import com.apollocurrency.aplwallet.api.response.AccountCurrentAssetBidOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AccountOpenAssetOrdersResponse;
import com.apollocurrency.aplwallet.api.response.AssetTradeResponse;
import com.apollocurrency.aplwallet.api.response.AssetsAccountsCountResponse;
import com.apollocurrency.aplwallet.api.response.AssetsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ExpectedAssetDeletes;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class AssetSteps extends StepBase {

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

}
