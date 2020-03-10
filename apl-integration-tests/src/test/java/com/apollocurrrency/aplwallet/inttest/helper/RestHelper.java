package com.apollocurrrency.aplwallet.inttest.helper;

import com.apollocurrrency.aplwallet.inttest.model.ReqParam;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
;


public class RestHelper {
    private static RestHelper restHelper;
    private RequestSpecification spec;
    private RequestSpecification preconditionSpec;

    private RestHelper() {
        String host = TestConfiguration.getTestConfiguration().getBaseURL();
        String port = TestConfiguration.getTestConfiguration().getPort();
        String url = String.format("http://%s:%s", host, port);


        spec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(url)
                .addFilter(new AllureRestAssured())
                .build();

        preconditionSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(url)
                .build();
    }

    public RequestSpecification getSpec() {
        return spec;
    }

    public static RestHelper getRestHelper() {
        if (restHelper == null){
            synchronized (RestHelper.class){
                restHelper = new RestHelper();
            }
        }
        return restHelper;
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
            param.put(ReqParam.PASS_PHRASE, wallet.getPass());
        }
        return param;
    }

}
