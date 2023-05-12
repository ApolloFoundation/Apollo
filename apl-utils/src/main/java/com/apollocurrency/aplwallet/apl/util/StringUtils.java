package com.apollocurrency.aplwallet.apl.util;

import java.util.List;

/**
 * Util class for common string checks, used instead of apache-commons
 */
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

    /**
     * Takes first not-empty value and ignores the rest of values
     * @param values strings or nulls
     * @return first not-empty value
     */
    public static String byPrecedence(String ...values){
        String res ="";
        String[] va = values;
        for (int i = 0; i<va.length; i++){
            if(!isBlank(va[i])){
               res=va[i];
               break;
            }
        }
        return res;
    }
}
