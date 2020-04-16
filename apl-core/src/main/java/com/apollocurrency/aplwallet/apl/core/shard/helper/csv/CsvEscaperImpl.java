/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CsvEscaperImpl implements CsvEscaper {

    private final char[] ctrlCharacters = {DEFAULT_FIELD_DELIMITER, DEFAULT_ESCAPE_CHARACTER, '\b', '\n', '\t', '\f', '\r'};
    private final Map<Character, CharSequence> translateMap;

    public CsvEscaperImpl() {
        this(DEFAULT_FIELD_DELIMITER);
    }

    public CsvEscaperImpl(char fieldDelimiter) {
        if (fieldDelimiter != 0) {
            ctrlCharacters[0] = fieldDelimiter;
        }
        this.translateMap = new HashMap<>(CTRL_CHARS_ESCAPE);
        translateMap.put(fieldDelimiter, String.valueOf(DEFAULT_ESCAPE_CHARACTER) + fieldDelimiter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String escape(String data) {
        if (data == null) {
            return null;
        }
        if (!StringUtils.containsAny(data, ctrlCharacters)) {
            return data;
        }

        int length = data.length();
        StringBuilder buff = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = data.charAt(i);
            CharSequence seq = translateMap.get(ch);
            if (seq != null) {
                buff.append(seq);
            } else {
                buff.append(ch);
            }
        }
        return buff.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unEscape(String data) {
        if (data == null) {
            return null;
        }
        int idx = data.indexOf(DEFAULT_ESCAPE_CHARACTER);
        if (idx < 0) {
            return data;
        }
        StringBuilder buff = new StringBuilder(data.length());
        int start = 0;
        char ch;
        char[] chars = data.toCharArray();
        do {
            buff.append(chars, start, idx - start);
            if (idx == data.length() - 1) {
                start = data.length();
                break;
            }
            ch = chars[idx + 1];
            CharSequence seq = CTRL_CHARS_UNESCAPE.get(ch);
            if (seq != null) {
                buff.append(seq);
            } else {
                buff.append(ch);
            }
            start = idx + 2;
            idx = data.indexOf(DEFAULT_ESCAPE_CHARACTER, start);
        } while (idx >= 0);

        buff.append(data.substring(start));
        return buff.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String translate(byte[] data) {
        return BYTE_ARRAY_PREFIX + Base64.getEncoder().encodeToString(data) + QUOTE;
    }

    @Override
    public String quotedText(String o) {
        return QUOTE + escapeQuote(o) + QUOTE;
    }

    private String escapeQuote(String o) {
        if (o == null) {
            return "";
        } else {
            return o.replace(QUOTE, DOUBLE_QUOTE);
        }
    }

}
