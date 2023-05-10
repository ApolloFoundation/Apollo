package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.TimeSource;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Slf4j
@Singleton
public class TimeConfig {
    private NtpTime ntpTime;
    private final boolean ntpEnabled;
    @Inject
    public TimeConfig(@Property(name = "apl.ntp-time.enabled", value = "false") boolean ntpEnabled) {
        this.ntpEnabled = ntpEnabled;
    }

    @Produces
    @Singleton
    public synchronized TimeSource timeSource() {
        if (!ntpEnabled) {
            return new CurrentSystemTimeSource();
        }
        ntpTime = new NtpTime();
        ntpTime.start();
        return ntpTime;
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (ntpEnabled) {
            ntpTime.shutdown();
        }
    }


    @Vetoed
    static class CurrentSystemTimeSource implements TimeSource {

        @Override
        public long currentTime() {
            return System.currentTimeMillis();
        }
    }
}
