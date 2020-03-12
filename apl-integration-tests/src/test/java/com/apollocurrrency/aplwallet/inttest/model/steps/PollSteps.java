package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.PollDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrency.aplwallet.api.response.PollResultResponse;
import com.apollocurrency.aplwallet.api.response.PollVotesResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class PollSteps extends StepBase {
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
}
