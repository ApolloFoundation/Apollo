/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.apollocurrency.aplwallet.apl.Constants;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;


public class NtpTime {

    private static final Logger LOG = getLogger(NtpTime.class);
    private static final NtpTime instance = new NtpTime();
    private static AtomicLong timeOffset = new AtomicLong(0);
            
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
            
            timeOffset = new AtomicLong(offsetValue);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
            timeOffset = new AtomicLong(0);
        }
        client.close();
    }
    
    public static long getTime() {
        return System.currentTimeMillis() + timeOffset.longValue();
    }
    
    public static void init() {        
        setTimeDrift();
        Runnable timeUpdate = () -> { setTimeDrift(); };
        ThreadPool.scheduleThread("NTP Update", timeUpdate, 10, TimeUnit.SECONDS);        
    }


    //never
    
    private static void NtpTime() {
                        
    }
    
            
}
