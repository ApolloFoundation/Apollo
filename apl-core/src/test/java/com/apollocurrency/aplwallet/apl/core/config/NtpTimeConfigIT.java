/*
 *  Copyright © 2018-2022 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.TimeSource;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrii Boiarskyi
 * @see TimeConfig
 * @see NtpTime
 * @since 1.51.1
 */
@EnableWeld
class NtpTimeConfigIT {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(PropertyProducer.class, TimeConfig.class)
        .addBeans(MockBean.of(initProperties(), PropertiesHolder.class))
        .build();

    @Inject
    TimeConfig timeConfig;
    @Inject
    TimeSource timeSource;

    @Test
    void createNtpTimeSource() {
        assertTrue(timeSource instanceof NtpTime, "Ntp time source must of type" +
            " NtpTime when apl.ntp-time.enabled=true");
        assertTrue(timeConfig.timeSource() instanceof NtpTime, "Ntp time source " +
            "created from CDI-managed TimeConfig class must of type" +
            " NtpTime when apl.ntp-time.enabled=true");
        assertTrue(timeSource.currentTime() > 0, "Current time must be > 0");
    }

    private static PropertiesHolder initProperties() {
        final PropertiesHolder holder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("apl.ntp-time.enabled", "true");
        holder.init(properties);
        return holder;
    }
}
