/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.util;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Utility class with small caching abilities and another methods
 *
 * @author yuriy.larin
 */
public class ConversionUtils {

    // memory used by this cache:
    // 4 * 1024 * 2 (strings per pair) * 64 * 2 (bytes per char) = 0.5 MB
    private static final int TO_UPPER_CACHE_LENGTH = 2 * 1024;
    private static final int TO_UPPER_CACHE_MAX_ENTRY_LENGTH = 64;
    private static final String[][] TO_UPPER_CACHE = new String[TO_UPPER_CACHE_LENGTH][];

    /**
     * Convert a string to uppercase using the English locale.
     *
     * @param s the test to convert
     * @return the uppercase text
     */
    public static String toUpperEnglish(String s) {
        if (s.length() > TO_UPPER_CACHE_MAX_ENTRY_LENGTH) {
            return s.toUpperCase(Locale.ENGLISH);
        }
        int index = s.hashCode() & (TO_UPPER_CACHE_LENGTH - 1);
        String[] e = TO_UPPER_CACHE[index];
        if (e != null) {
            if (e[0].equals(s)) {
                return e[1];
            }
        }
        String s2 = s.toUpperCase(Locale.ENGLISH);
        e = new String[]{s, s2};
        TO_UPPER_CACHE[index] = e;
        return s2;
    }

    /**
     * Split a string into an array of strings using the given separator. A null
     * string will result in a null array, and an empty string in a zero element
     * array.
     *
     * @param s             the string to split
     * @param separatorChar the separator character
     * @param trim          whether each element should be trimmed
     * @return the array list
     */
    public static String[] arraySplit(String s, char separatorChar, boolean trim) {
        if (s == null) {
            return null;
        }
        int length = s.length();
        if (length == 0) {
            return new String[0];
        }
        ArrayList<String> list = new ArrayList<>(4);
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == separatorChar) {
                String e = buff.toString();
                list.add(trim ? e.trim() : e);
                buff.setLength(0);
            } else if (c == '\\' && i < length - 1) {
                buff.append(s.charAt(++i));
            } else {
                buff.append(c);
            }
        }
        String e = buff.toString();
        list.add(trim ? e.trim() : e);
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    /**
     * Trim a character from a string.
     *
     * @param s        the string
     * @param leading  if leading characters should be removed
     * @param trailing if trailing characters should be removed
     * @param sp       what to remove (only the first character is used)
     *                 or null for a space
     * @return the trimmed string
     */
    public static String trim(String s, boolean leading, boolean trailing,
                              String sp) {
        char space = (sp == null || sp.length() < 1) ? ' ' : sp.charAt(0);
        if (leading) {
            int len = s.length(), i = 0;
            while (i < len && s.charAt(i) == space) {
                i++;
            }
            s = (i == 0) ? s : s.substring(i);
        }
        if (trailing) {
            int endIndex = s.length() - 1;
            int i = endIndex;
            while (i >= 0 && s.charAt(i) == space) {
                i--;
            }
            s = i == endIndex ? s : s.substring(0, i + 1);
        }
        return s;
    }

    /**
     * Convert a long value to an int value. Values larger than the biggest int
     * value is converted to the biggest int value, and values smaller than the
     * smallest int value are converted to the smallest int value.
     *
     * @param l the value to convert
     * @return the converted int value
     */
    public static int convertLongToInt(long l) {
        if (l <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else if (l >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) l;
        }
    }

}
