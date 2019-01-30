package com.apollocurrency.aplwallet.apl.util;

/**
 * Util class, which provides common validation of strings without outer dependencies
 */
public class StringValidator {

    private static final String DEFAULT_STRING_NAME = "String parameter";
    private static final String BLANK_STRING_EXCEPTION_TEMPLATE = "%s cannot be null or blank!";

    private StringValidator() {}

    public static String requireNonBlank(String s, String name) {
        if (StringUtils.isBlank(s)) {
            throw new IllegalArgumentException(String.format(BLANK_STRING_EXCEPTION_TEMPLATE, name));
        }
        return s;
    }
    public static String requireNonBlank(String s) {
        return requireNonBlank(s, DEFAULT_STRING_NAME);
    }
}
