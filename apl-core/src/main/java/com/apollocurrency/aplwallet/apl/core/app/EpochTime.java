/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author al
 */
@Singleton
public class EpochTime implements Time {
    
    private final NtpTime ntpTime;

    @Inject
    public EpochTime(NtpTime ntpTime) {
        this.ntpTime = ntpTime;
    }

    /**
     *  Time since genesis block.
     * @return int (time in seconds).
     */
    public int getEpochTime() {
        return Convert2.toEpochTime(ntpTime.getTime());
    }
    
}
