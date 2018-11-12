/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;
import java.util.concurrent.atomic.AtomicLong;



public class NtpTime {

    private static final Logger LOG = getLogger(NtpTime.class);
    private static AtomicLong timeOffset = new AtomicLong(0);
    final private static NtpTime instance = new NtpTime();
            
    private static void setTimeDrift()
    {
        try
        {
            NTPUDPClient client = new NTPUDPClient();
            client.open();
            InetAddress hostAddr = InetAddress.getByName("pool.ntp.org");
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails(); // compute offset/delay if not already done
            Long offsetValue = info.getOffset();
            Long delayValue = info.getDelay();
            String delay = (delayValue == null) ? "N/A" : delayValue.toString();
            String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();
            
            LOG.info(" Roundtrip delay(ms)=" + delay
                    + ", clock offset(ms)=" + offset); // offset in ms
            client.close();
            timeOffset = new AtomicLong(offsetValue);
        }
        catch (Exception e)
        {
            LOG.error(e.getMessage());
            timeOffset = new AtomicLong(0);
        }
    }
    
    public static long getTime()
    {
        return System.currentTimeMillis() + timeOffset.longValue();
    }
    
    
    private static void NtpTime()
    {
                        
    }
    
    public static void init()
    {        
        setTimeDrift();
        Runnable timeUpdate = () -> { setTimeDrift(); };
        ThreadPool.scheduleThread("NTP Update", timeUpdate, 10, TimeUnit.SECONDS);        
    }
            
}
