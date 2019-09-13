package com.apollocurrency.aplwallet.apl.util;

import java.util.Objects;
import javax.enterprise.inject.Vetoed;

/**
 * Util class, which provides common validation of strings without outer dependencies
 */
@Vetoed
public class StringValidator {

    private static final String DEFAULT_STRING_NAME = "String parameter";
    private static final String BLANK_STRING_EXCEPTION_TEMPLATE = "%s cannot be blank!";
    private static final String NULL_STRING_EXCEPTION_TEMPLATE = "%s cannot be null!";

    public static String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, String.format(NULL_STRING_EXCEPTION_TEMPLATE, name));
        if (s.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format(BLANK_STRING_EXCEPTION_TEMPLATE, name));
        }
        return s;
    }
    public static String requireNonBlank(String s) {
        return requireNonBlank(s, DEFAULT_STRING_NAME);
    }
}
