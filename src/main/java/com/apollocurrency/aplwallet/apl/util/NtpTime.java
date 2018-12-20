/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import com.apollocurrency.aplwallet.apl.Constants;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;


public class NtpTime {

    private static final Logger LOG = getLogger(NtpTime.class);
    private static volatile long timeOffset = 0;
    private static final int DEFAULT_REFRESH_FREQUENCY = 60;
    private static final Runnable timeUpdateTask = () -> { setTimeDrift(); };
    private static void setTimeDrift() {
        NTPUDPClient client = new NTPUDPClient();

        try {
            client.open();
            InetAddress hostAddr = InetAddress.getByName(Constants.TIME_SERVICE);
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
            LOG.error(e.getMessage(), e);
            timeOffset = 0;
        }
        finally {
             client.close();
        }
    }
    
    public static long getTime() {
        return System.currentTimeMillis() + timeOffset;
    }

    private final int refreshFrequency;
    private final TimeUnit timeUnit;
    public NtpTime(int refreshFrequency, TimeUnit timeUnit) {
        this.refreshFrequency = refreshFrequency;
        this.timeUnit = timeUnit;
    }

    public void start() {
        ThreadPool.scheduleThread("NTPUpdate", timeUpdateTask, refreshFrequency, timeUnit);
    }

    public NtpTime() {
        this(DEFAULT_REFRESH_FREQUENCY, TimeUnit.SECONDS);
    }
}
