package com.apollocurrency.aplwallet.apl.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class NtpTimeTest {

    private NtpTime ntpTime;
    private long currentTime;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @BeforeEach
    void setUp() {
        currentTime = System.currentTimeMillis();
        ntpTime = new NtpTime();
        ntpTime.start(); // emulate @PostConstrust
    }

    @AfterEach
    void tearDown() {
        ntpTime.shutdown();
    }

    @Test
    void getTimeTest() {
        long freshTime = ntpTime.getTime();
        assertTrue(freshTime > currentTime);
        log.info("now : long = {} / formatted = {}", freshTime, dateFormat.format(freshTime));
    }

}