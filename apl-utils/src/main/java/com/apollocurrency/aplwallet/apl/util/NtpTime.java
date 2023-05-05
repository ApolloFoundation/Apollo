/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class NtpTime implements TimeSource {

    private static final Logger LOG = getLogger(NtpTime.class);

    private static final int REFRESH_FREQUENCY = 60;
    private static final String TIME_SERVICE = "pool.ntp.org";
    private static final int DEFAULT_TIMEOUT = 5000; // in millis
    private volatile long timeOffset = 0;
    private NTPUDPClient client;

    private void setTimeDrift() {
        try {
            InetAddress hostAddr = InetAddress.getByName(TIME_SERVICE);
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails(); // compute offset/delay if not already done
            Long offsetValue = info.getOffset();
            Long delayValue = info.getDelay();
            String delay = (delayValue == null) ? "N/A" : delayValue.toString();
            String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

            LOG.debug("Roundtrip delay(ms)={}, clock offset(ms)={}", delay, offset); // offset in ms
            if (offsetValue != null) {
                timeOffset = offsetValue;
            }
        } catch (SocketTimeoutException | UnknownHostException e) {
            LOG.debug("Exception: {}. Keep prev offset: {}", e.getMessage(), timeOffset);
        } catch (IOException e) {
            LOG.debug("NTP exception: {}", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * This implemetation uses received from NTP time offset in millis to adjust time to the global clock when system
     * time is out of sync for some reason
     */
    @Override
    public long currentTime() {
        return System.currentTimeMillis() + timeOffset;
    }

    public void start() {
        setUpClient();
        Runnable timeUpdate = this::setTimeDrift;
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NTP-Update"));
        scheduledThreadPool.scheduleWithFixedDelay(timeUpdate, 0, REFRESH_FREQUENCY, TimeUnit.SECONDS);
    }

    private void setUpClient() {
        try {
            client = new NTPUDPClient();
            client.setDefaultTimeout(DEFAULT_TIMEOUT);
            client.open();
        } catch (SocketException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void shutdown() {
        client.close();
    }
}
