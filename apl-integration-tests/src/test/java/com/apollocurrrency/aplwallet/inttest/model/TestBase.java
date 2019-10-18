package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.model.TestBaseOld.setUpTestData;
import static com.apollocurrrency.aplwallet.inttest.model.TestBaseOld.startForgingSetUp;


public abstract class TestBase implements ITest {
    public static TestInfo testInfo;
    protected static RetryPolicy retryPolicy;
    protected static RestHelper restHelper;

    @BeforeAll
    static void initAll() {
        System.out.println("--------------1----------------");
        TestConfiguration.getTestConfiguration();
        retryPolicy = new RetryPolicy()
                     .retryWhen(false)
                     .withMaxRetries(20)
                     .withDelay(1, TimeUnit.SECONDS);
        restHelper = new RestHelper();
        startForgingSetUp();
        setUpTestData();
    }

    @BeforeEach
    void setUP(TestInfo testInfo){
        this.testInfo = testInfo;
    }


    @AfterEach
    void testEnd(){
        this.testInfo = null;
    }

    @AfterAll
    static void afterAll() {

    }
}
