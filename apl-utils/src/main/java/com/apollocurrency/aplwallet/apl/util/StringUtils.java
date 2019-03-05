package com.apollocurrency.aplwallet.apl.util;

/**
 * Util class for common string checks, used instead of apache-commons
 */
public class StringUtils {
    private StringUtils() {} //never

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
