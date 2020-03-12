package com.apollocurrrency.aplwallet.inttest.model.steps;

import com.apollocurrency.aplwallet.api.dto.DGSGoodsDTO;
import com.apollocurrency.aplwallet.api.response.CreateTransactionResponse;
import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.ReqType;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;

import java.io.File;
import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class MarketplaceSteps extends StepBase {
    @Step
    public CreateTransactionResponse dgsDelisting(Wallet wallet, String transaction) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_DELISTING);
        param.put(ReqParam.GOODS, transaction);
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
    public CreateTransactionResponse dgsPriceChange(Wallet wallet, String transaction, int newPrice) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_PRICE_CHANGE);
        param.put(ReqParam.GOODS, transaction);
        param.put(ReqParam.PRICE, String.valueOf(newPrice));
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
    public CreateTransactionResponse dgsQuantityChange(Wallet wallet, String transaction, int deltaQuantity) {

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_QUANTITY_CHANGE);
        param.put(ReqParam.GOODS, transaction);
        param.put(ReqParam.DELTA_QUANTITY, String.valueOf(deltaQuantity));
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
    public DGSGoodsDTO getDGSGood(String transaction) {
        HashMap<String, String> param = new HashMap();
        param.put(ReqType.REQUEST_TYPE, ReqType.GET_DGS_GOOD);
        param.put(ReqParam.GOODS,transaction);

        return given().log().all()
            .spec(restHelper.getSpec())
            .contentType(ContentType.URLENC)
            .formParams(param)
            .when()
            .get(path)
            .then()
            .assertThat().statusCode(200)
            .extract().body().jsonPath()
            .getObject("", DGSGoodsDTO.class);
    }
    @Step
    public CreateTransactionResponse dgsListing(Wallet wallet, String name, String description, String tags, int quantity, int priceATM, File image) {

        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_LISTING);
        param.put(ReqParam.NAME, name);
        param.put(ReqParam.DESCRIPTION, description);
        param.put(ReqParam.TAGS, tags);
        param.put(ReqParam.QUANTITY, String.valueOf(quantity));
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
        param.put(ReqParam.MESSAGE_IS_TEXT, "false");
        param.put(ReqParam.PRICE, String.valueOf(priceATM));
        param.put(ReqParam.FEE, "100000000000");
        param.put(ReqParam.DEADLINE, "1440");

        return given().log().all()
            .spec(restHelper.getSpec())
            .multiPart(ReqParam.MESSAGE_FILE,image)
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
    public CreateTransactionResponse dgsRefund(Wallet wallet, String purchase, int refundATM, String message) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_REFUND);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.REFUND, String.valueOf(refundATM));
        param.put(ReqParam.MESSAGE, message);
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
    public CreateTransactionResponse dgsFeedback(Wallet wallet, String purchase, String message) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_FEEDBACK);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.MESSAGE_IS_TEXT, "true");
        param.put(ReqParam.MESSAGE, message);
        param.put(ReqParam.MESSAGE_TO_ENCRYPT, message);
        param.put(ReqParam.MESSAGE_IS_PRUNABLE, "true");
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
    public CreateTransactionResponse dgsDelivery(Wallet wallet, String purchase, String delivery, int discountATM) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_DELIVERY);
        param.put(ReqParam.PURCHASE, purchase);
        param.put(ReqParam.DISCOUNT, String.valueOf(discountATM));
        param.put(ReqParam.GOODS_TO_ENCRYPT, delivery);
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
    public CreateTransactionResponse dgsPurchase(Wallet wallet, String goods, long priceATM, int quantity, int deliveryDeadlineTimeInHours) {
        HashMap<String, String> param = new HashMap();
        param = restHelper.addWalletParameters(param,wallet);
        param.put(ReqType.REQUEST_TYPE, ReqType.DGS_PURCHASE);
        param.put(ReqParam.GOODS, goods);
        param.put(ReqParam.PRICE, String.valueOf(priceATM));
        param.put(ReqParam.QUANTITY, String.valueOf(quantity));
        param.put(ReqParam.DELIVERY_DEADLINE_TIMESTAMP, String.valueOf(deliveryDeadlineTimeInHours * 3600000));
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
