package com.apollocurrrency.aplwallet.inttest.model;

import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestBase implements ITest {
    public static final Logger log = LoggerFactory.getLogger(TestBase.class);
    protected static TestInfo testInfo;
    protected static RetryPolicy retryPolicy;

    @BeforeAll
    static void initAll() {

    }

    @BeforeEach
    void setUP(TestInfo testInfo){

    }


    @AfterEach
    void testEnd(){

    }

    @AfterAll
    static void afterAll() {

    }
}
