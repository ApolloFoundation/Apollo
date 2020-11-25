package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j
@QuarkusTest
class TimeServiceTest {

    private GenesisImporter genesisImporter = mock(GenesisImporter.class);

/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(genesisImporter, GenesisImporter.class))
        .build();*/

    private NtpTime ntpTime = new NtpTime();
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