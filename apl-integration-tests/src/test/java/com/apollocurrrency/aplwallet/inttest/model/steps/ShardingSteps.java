package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.List;

import static io.restassured.RestAssured.given;

public class ShardingSteps extends StepBase {

    @Step("Get Shards from peer")
    public List<ShardDTO> getShards(String ip) {
        String path = "/rest/shards";
        return given().log().uri()
            .contentType(ContentType.JSON)
            .baseUri(String.format("http://%s:%s", ip, 7876))
            .when()
            .get(path).getBody().jsonPath().getList("", ShardDTO.class);
    }
}
