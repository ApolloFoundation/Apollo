package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.AccountAliasDTO;
import com.apollocurrency.aplwallet.api.response.AccountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.AccountCountAliasesResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class AliasSteps extends StepBase {

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
}
