/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.inject.Vetoed;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@Slf4j
@Vetoed
public class RuntimeUtils {
    private RuntimeUtils() {
    }

    public static boolean isEnoughMemory(long limit) {
        long memoryTotal = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            Object attribute = mBeanServer.getAttribute(new ObjectName("java.lang", "type", "OperatingSystem"), "TotalPhysicalMemorySize");
            memoryTotal = (Long) attribute;
        } catch (Exception ignored) {
        }
        boolean res;
        if (memoryTotal == 0) {
            log.warn("Can not calculate physical RAM size");
            res = true;
        } else {
            res = (memoryTotal >= limit);
        }
        if (!res) {
            log.warn("Not enough system memory.");
            log.debug("Required memory: {}, Available: {} ", limit, memoryTotal);
        }
        return res;
    }
}
