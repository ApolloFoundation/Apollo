/*
 *  Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.TimeSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrii Boiarskyi
 * @see TimeConfig
 * @see com.apollocurrency.aplwallet.apl.util.NtpTime
 * @since 1.51.1
 */
@EnableWeld
class NonNtpTimeConfigIT {


    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(PropertyProducer.class, TimeConfig.class)
        .addBeans(MockBean.of(initProperties(), PropertiesHolder.class))
        .build();

    @Inject
    TimeConfig timeConfig;
    @Inject
    TimeSource timeSource;

    @Test
    void createNonNtpTimeSource() {
        assertTrue(timeSource instanceof TimeConfig.CurrentSystemTimeSource, "Non-ntp time source must of type" +
            " CurrentSystemTimeSource when apl.ntp-time.enabled=false");
        assertTrue(timeConfig.timeSource() instanceof TimeConfig.CurrentSystemTimeSource, "Non-ntp time source " +
            "created from CDI-managed TimeConfig class  must of type" +
            " CurrentSystemTimeSource when apl.ntp-time.enabled=false");
        assertTrue(timeSource.currentTime() > 0, "Current time must be > 0");
    }

    private static PropertiesHolder initProperties() {
        final PropertiesHolder holder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("apl.ntp-time.enabled", "false");
        holder.init(properties);
        return holder;
    }

}
