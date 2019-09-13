package com.apollocurrency.aplwallet.apl.util;

import javax.enterprise.inject.Vetoed;

/**
 * Util class for common string checks, used instead of apache-commons
 */
@Vetoed
public class StringUtils {

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
