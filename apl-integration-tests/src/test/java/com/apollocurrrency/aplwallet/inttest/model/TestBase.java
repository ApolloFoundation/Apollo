package com.apollocurrrency.aplwallet.inttest.model;

import com.apollocurrrency.aplwallet.inttest.helper.TestConfiguration;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;



public abstract class TestBase implements ITest {
    public static final Logger log = LoggerFactory.getLogger(TestBase.class);
    protected TestInfo testInfo;
    protected static RetryPolicy retryPolicy;

    @BeforeAll
    static void initAll() {
        TestConfiguration.getTestConfiguration();
        retryPolicy = new RetryPolicy()
                     .retryWhen(false)
                     .withMaxRetries(20)
                     .withDelay(1, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUP(TestInfo testInfo){
        this.testInfo = testInfo;
    }


    @AfterEach
    void testEnd(){
        TestBaseOld.testInfo = null;
    }

    @AfterAll
    static void afterAll() {

    }
}
