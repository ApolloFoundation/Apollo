package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class NtpTimeConfig {
    private volatile NtpTime time;
    public NtpTimeConfig() {
    }

    @Produces
    @Singleton
    public NtpTime time() {
        time = new NtpTime();
        time.start();
        return time;
    }

    @PreDestroy
    public void shutdown() {
        time.shutdown();
    }
}
