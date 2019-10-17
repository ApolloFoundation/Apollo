package com.apollocurrrency.aplwallet.inttest.helper;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class RestHelper {
    public RequestSpecification spec;
    private String host = TestConfiguration.getTestConfiguration().getBaseURL();
    private String port = TestConfiguration.getTestConfiguration().getPort();

    public RestHelper() {
        spec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setBaseUri(String.format("http://%s:%s",host,port))
                .build();
    }
}
