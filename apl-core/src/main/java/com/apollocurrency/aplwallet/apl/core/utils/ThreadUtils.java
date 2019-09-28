package com.apollocurrency.aplwallet.apl.core.utils;

import java.util.concurrent.TimeUnit;
import javax.enterprise.inject.Vetoed;

@Vetoed
public class ThreadUtils {
    public static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(time);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
        private ThreadUtils() {}
}
