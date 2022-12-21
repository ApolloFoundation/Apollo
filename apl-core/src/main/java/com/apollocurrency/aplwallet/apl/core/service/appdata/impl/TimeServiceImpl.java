/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.TimeSource;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Supply time methods within CDI including Unix epoch time in millis/seconds, blockchain epoch time in seconds
 * @author Andrii Boiarskyi
 */
@Slf4j
@Singleton
public class TimeServiceImpl implements TimeService {

    private final TimeSource timeSource;

    @Inject
    public TimeServiceImpl(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    /**
     * Time since genesis block.
     *
     * @return int (time in seconds).
     */
    public int getEpochTime() {
        long ntpTime = this.timeSource.currentTime();
        int toEpochTime = Convert2.toEpochTime(ntpTime);
        log.trace("ntpTime : long = {}, toEpochTime = {}", ntpTime, toEpochTime);
        return toEpochTime;
    }


    /**
     * Returns current time in seconds
     * @return current time in seconds
     */
    @Override
    public long systemTime() {
        return timeSource.currentTime() / 1000;
    }

    /**
     * Returns current time in milliseconds
     * @return current time in milliseconds
     */
    @Override
    public long systemTimeMillis() {
        return timeSource.currentTime();
    }
}

