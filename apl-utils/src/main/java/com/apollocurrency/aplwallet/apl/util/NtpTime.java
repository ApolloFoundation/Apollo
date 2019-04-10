/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

@Singleton
public class NtpTime {

    private static final Logger LOG = getLogger(NtpTime.class);

    private static final int REFRESH_FREQUENCY = 60;
    private static final String TIME_SERVICE = "pool.ntp.org";
    private static final int DEFAULT_TIMEOUT = 3000; // in millis
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
            
            LOG.info(" Roundtrip delay(ms)=" + delay
                    + ", clock offset(ms)=" + offset); // offset in ms
            
            timeOffset = offsetValue;
        }
        catch (IOException e ) {
            LOG.warn(e.getMessage(), e);
            timeOffset = 0;
        }
    }
    
    public long getTime() {
        return System.currentTimeMillis() + timeOffset;
    }

    public NtpTime() {}

    @PostConstruct
    public void start() {
        Runnable timeUpdate = this::setTimeDrift;
        ThreadPool.scheduleThread("NTP Update", timeUpdate, REFRESH_FREQUENCY, TimeUnit.SECONDS);
        setUpClient();
    }

    private void setUpClient() {
        try {
            client = new NTPUDPClient();
            client.setDefaultTimeout(DEFAULT_TIMEOUT);
            client.open();
        }
        catch (SocketException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        client.close();
    }
}
