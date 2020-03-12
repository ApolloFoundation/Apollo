package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.response.AllShufflingsResponse;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.ShufflingDTO;
import com.apollocurrency.aplwallet.api.response.ShufflingParticipantsResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class ShufflingSteps extends StepBase {
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
