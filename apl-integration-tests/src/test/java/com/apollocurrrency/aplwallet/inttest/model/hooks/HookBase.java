package com.apollocurrrency.aplwallet.inttest.model.hooks;

import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.model.TestBase;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HookBase {
    final Logger log = LoggerFactory.getLogger(this.getClass());
    private RetryPolicy retryPolicy = new RetryPolicy()
        .retryWhen(false)
        .withMaxRetries(50)
        .withDelay(10, TimeUnit.SECONDS);
    private RestHelper restHelper = RestHelper.getRestHelper();
    private String path = "/apl";
    private RestAssuredConfig config = RestAssured.config()
                                      .httpClient(HttpClientConfig.httpClientConfig()
                                      .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
                                      .setParam(CoreConnectionPNames.SO_TIMEOUT, 10000));

}
