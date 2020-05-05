package com.apollocurrency.aplwallet.apl.util;

import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit5.EnableWeld;

@Slf4j
@EnableWeld
class NtpTimeTest {

//    @WeldSetup
//    public WeldInitiator weld = WeldInitiator.from(NtpTime.class)
//        .build();
//
//    private NtpTime ntpTime;
//    private long currentTime;
//    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//
//    @BeforeEach
//    void setUp() {
//        currentTime = System.currentTimeMillis();
//    }
//
//    @Test
//    void getTimeTest() {
//        ntpTime = new NtpTime();
//        ntpTime.start(); // emulate @PostConstrust
//        long freshTime = ntpTime.getTime();
//        assertTrue(freshTime > currentTime);
//        log.info("now : long = {} / formatted = {}", freshTime, dateFormat.format(freshTime));
//    }

}