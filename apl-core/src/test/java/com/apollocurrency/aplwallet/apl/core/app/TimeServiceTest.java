package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
@EnableWeld
class TimeServiceTest {

    private GenesisImporter genesisImporter = mock(GenesisImporter.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class, TimeServiceImpl.class)
            .addBeans(MockBean.of(genesisImporter, GenesisImporter.class))
            .build();

    @Inject
    private NtpTime ntpTime;
    private TimeService timeService;

    @BeforeEach
    void setUp() {
        genesisImporter.EPOCH_BEGINNING = 1515931200000L; // emulate json loading and CDI component internal initialization
        assertNotNull(ntpTime);
    }

    @Test
    void getEpochTimeTest() {
        timeService = new TimeServiceImpl(ntpTime);
        int epohTime = timeService.getEpochTime(); // emulate @PostConstrust
        assertTrue(epohTime > 0);
        log.info("now epohTime : {}", epohTime);
    }

}