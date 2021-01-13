package com.apollocurrency.aplwallet.apl.util;

import javax.enterprise.inject.Vetoed;
import java.util.List;

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

    public static boolean containsIgnoreCase(String str, List<String> l) {
        for (String s : l) {
            if (str.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalsIgnoreCase(String str, List<String> l) {
        for (String s : l) {
            if (str.trim().equalsIgnoreCase(s.trim())) {
                return true;
            }
        }
        return false;
    }
}
