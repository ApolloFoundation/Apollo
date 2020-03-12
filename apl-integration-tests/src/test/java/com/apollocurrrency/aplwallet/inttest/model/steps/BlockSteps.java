package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.ECBlockDTO;
import com.apollocurrency.aplwallet.api.response.AccountBlocksResponse;
import com.apollocurrency.aplwallet.api.response.GetBlockIdResponse;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class BlockSteps extends StepBase {

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
}
