package com.apollocurrrency.aplwallet.inttest.helper;

import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;


public class RestHelper {
    private RequestSpecification spec;
    private RequestSpecification preconditionSpec;
    private String host = TestConfiguration.getTestConfiguration().getBaseURL();
    private String port = TestConfiguration.getTestConfiguration().getPort();

    public RestHelper() {
        spec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(String.format("http://%s:%s", host, port))
                .addFilter(new AllureRestAssured())
                .build();

        preconditionSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(String.format("http://%s:%s", host, port))
                .build();
    }

    public RequestSpecification getSpec() {
        return spec;
    }

    public RequestSpecification getPreconditionSpec() {
        return preconditionSpec;
    }

    public HashMap<String, String> addWalletParameters(HashMap<String, String> param, Wallet wallet) {
        param.put(ReqParam.ACCOUNT, wallet.getUser());
        if (!wallet.isVault()) {
            param.put(ReqParam.SECRET_PHRASE, wallet.getPass());
        } else {
            param.put(ReqParam.SENDER, wallet.getUser());
            param.put(ReqParam.PASSPHRASE, wallet.getPass());
        }
        return param;
    }

}
