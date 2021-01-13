/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author al
 */
@Slf4j
@Singleton
public class TimeServiceImpl implements TimeService {

    private final NtpTime ntpTime;

    @Inject
    public TimeServiceImpl(NtpTime ntpTime) {
        this.ntpTime = ntpTime;
    }

    /**
     * Time since genesis block.
     *
     * @return int (time in seconds).
     */
    public int getEpochTime() {
        long ntpTime = this.ntpTime.getTime();
        int toEpochTime = Convert2.toEpochTime(ntpTime);
        log.trace("ntpTime : long = {}, toEpochTime = {}", ntpTime, toEpochTime);
        return toEpochTime;
    }

    @Override
    public long systemTime() {
        return ntpTime.getTime() / 1000;
    }

    @Override
    public long systemTimeMillis() {
        return ntpTime.getTime();
    }
}
