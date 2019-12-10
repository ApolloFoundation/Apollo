/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

/**
 * Utility for string operations
 *
 *      escapeCharacter = '\"'
 *      fieldDelimiter = '\"'
 */
public class CsvStringUtils {
    public static final char DEFAULT_ESCAPE_CHARACTER = '\"';
    public static final char DEFAULT_FIELD_DELIMITER = '\"';

    /**
     * UnEscape the fieldDelimiter character that already escaped with the escape character.
     * @param data source escaped string
     * @return unescaped string
     */
    public static String unEscape(String data, char escapeCharacter, char fieldDelimiter) {
        if (data == null){
            return null;
        }
        StringBuilder buff = new StringBuilder(data.length());
        int start = 0;
        char[] chars = null;
        while (true) {
            int idx = data.indexOf(escapeCharacter, start);
            if (idx < 0) {
                idx = data.indexOf(fieldDelimiter, start);
                if (idx < 0) {
                    break;
                }
            }
            if (chars == null) {
                chars = data.toCharArray();
            }
            buff.append(chars, start, idx - start);
            if (idx == data.length() - 1) {
                start = data.length();
                break;
            }
            buff.append(chars[idx + 1]);
            start = idx + 2;
        }
        buff.append(data.substring(start));
        return buff.toString();
    }

    public static String unEscape(String data) {
        return unEscape(data, DEFAULT_ESCAPE_CHARACTER, DEFAULT_FIELD_DELIMITER);
    }

    /**
     * Escape the fieldDelimiter character with the escape character.
     * @param data source string
     * @return escaped string
     */
    public static String escape(String data, char escapeCharacter, char fieldDelimiter) {
        if (data == null){
            return null;
        }
        if (data.indexOf(fieldDelimiter) < 0) {
            if (escapeCharacter == fieldDelimiter || data.indexOf(escapeCharacter) < 0) {
                return data;
            }
        }
        int length = data.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = data.charAt(i);
            if (ch == fieldDelimiter || ch == escapeCharacter) {
                buff.append(escapeCharacter);
            }
            buff.append(ch);
        }
        return buff.toString();
    }

    public static String escape(String data) {
        return escape(data, DEFAULT_ESCAPE_CHARACTER, DEFAULT_FIELD_DELIMITER);
    }

}
