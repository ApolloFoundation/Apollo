package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.TaggedDataDTO;
import com.apollocurrency.aplwallet.api.response.AllTaggedDataResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.DataTagCountResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.io.File;
import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class TaggedDataSteps extends StepBase {
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
}
