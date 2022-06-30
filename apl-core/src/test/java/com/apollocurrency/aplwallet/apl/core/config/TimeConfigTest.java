/*
 *  Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.TimeSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrii Boiarskyi
 * @see TimeConfig
 * @see NtpTime
 * @since 1.51.1
 */
class TimeConfigTest {


    @Test
    void createNonNtpTimeSource() {
        final TimeConfig timeConfig = new TimeConfig(false);

        final TimeSource timeSource = timeConfig.timeSource();

        assertTrue(timeSource instanceof TimeConfig.CurrentSystemTimeSource,
            "Non-ntp time source must an instance of CurrentSystemTimeSource class");
        assertTrue(timeSource.currentTime() > 0, "Current time must be > 0");

        timeConfig.shutdown(); // verify no exceptions
    }

    @Test
    void createNtpTimeSource() {
        final TimeConfig timeConfig = new TimeConfig(true);

        final TimeSource timeSource = timeConfig.timeSource();

        assertTrue(timeSource instanceof NtpTime,
            "Ntp time source must an instance of NtpTime class");
        assertTrue(timeSource.currentTime() > 0, "Current NTP time must be > 0");

        timeConfig.shutdown(); // verify no exceptions
    }
}