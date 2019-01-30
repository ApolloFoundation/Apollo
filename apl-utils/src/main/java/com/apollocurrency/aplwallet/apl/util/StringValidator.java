package com.apollocurrency.aplwallet.apl.util;

public class StringValidator {
    private StringValidator() {}

    public static String requireNonBlank(String s, String exceptionMessage) {
        if (StringUtils.isBlank(s)) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return s;
    }
    public static String requireNonBlank(String s) {
        return requireNonBlank(s, "String is empty or null");
    }
}
