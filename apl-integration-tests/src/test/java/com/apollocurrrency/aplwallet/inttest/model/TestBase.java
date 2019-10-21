package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrrency.aplwallet.inttest.helper.RestHelper;
import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.apollocurrrency.aplwallet.inttest.model.TestBaseOld.setUpTestData;
import static com.apollocurrrency.aplwallet.inttest.model.TestBaseOld.startForgingSetUp;
import static io.restassured.RestAssured.given;


public abstract class TestBase implements ITest {
    public static TestInfo testInfo;
    protected static RetryPolicy retryPolicy;
    protected static RestHelper restHelper;

    @BeforeAll
    static void initAll() {
        TestConfiguration.getTestConfiguration();
        retryPolicy = new RetryPolicy()
                     .retryWhen(false)
                     .withMaxRetries(20)
                     .withDelay(1, TimeUnit.SECONDS);
        restHelper = new RestHelper();
        startForgingSetUp();
        setUpTestData();
        ClassLoader classLoader = TestBase.class.getClassLoader();
        String secretFilePath = Objects.requireNonNull(classLoader.getResource("APL-MK35-9X23-YQ5E-8QBKH")).getPath();
        importSecretFileSetUp(secretFilePath,"1");
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

    //Static need for a BeforeAll method
    private static void importSecretFileSetUp(String pathToSecretFile, String pass) {
        String path = "/rest/keyStore/upload";
        given().log().all()
                .spec(restHelper.getSpec())
                .header("Content-Type", "multipart/form-data")
                .multiPart("keyStore", new File(pathToSecretFile))
                .formParam("passPhrase", pass)
                .when()
                .post(path);
    }
}
