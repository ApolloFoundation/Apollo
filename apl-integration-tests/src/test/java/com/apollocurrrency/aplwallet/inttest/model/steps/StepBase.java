package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.BlockDTO;
import com.apollocurrency.aplwallet.api.dto.TransactionDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.params.CoreConnectionPNames;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class StepBase {
    String path = "/apl";
    final Logger log = LoggerFactory.getLogger(this.getClass());
    RestHelper restHelper = RestHelper.getRestHelper();
    private RetryPolicy retryPolicy = new RetryPolicy()
                                     .retryWhen(false)
                                     .withMaxRetries(50)
                                     .withDelay(10, TimeUnit.SECONDS);



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
    public boolean verifyTransactionInBlock(String transaction) {
        boolean inBlock = false;
        if (transaction != null) {
            try {
                inBlock = Failsafe.with(retryPolicy).get(() -> getTransaction(transaction).getConfirmations() >= 0);
            } catch (Exception e) {
                fail("Transaction does't add to block. Transaction " + transaction + " Exception: " + e.getMessage());
            }
            assertTrue(inBlock, String.format("Transaction %s in block: ", transaction));
        }else{
            fail("Transaction is null");
        }
        return inBlock;
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

}
