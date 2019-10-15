package com.apollocurrency.aplwallet.apl.core.utils;

import javax.enterprise.inject.Vetoed;
import java.util.concurrent.TimeUnit;

@Vetoed
public class ThreadUtils {
    public static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ThreadUtils() {
    }
}
