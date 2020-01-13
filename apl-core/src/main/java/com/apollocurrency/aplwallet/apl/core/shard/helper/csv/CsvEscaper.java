/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import java.util.Map;

public interface CsvEscaper {
    char DEFAULT_ESCAPE_CHARACTER = '\\';
    char DEFAULT_FIELD_DELIMITER = '\"';
    String QUOTE = "\'";
    String DOUBLE_QUOTE = QUOTE + QUOTE;
    String BYTE_ARRAY_PREFIX = "b"+QUOTE;


    Map<Character, CharSequence> CTRL_CHARS_UNESCAPE = Map.of(
            'b', "\b",
            'n', "\n",
            't', "\t",
            'f', "\f",
            'r', "\r");

    Map<Character, CharSequence> CTRL_CHARS_ESCAPE = Map.of(
                    '\\', "\\\\",
                    '\b', "\\b",
                    '\n', "\\n",
                    '\t', "\\t",
                    '\f', "\\f",
                    '\r', "\\r");

    /**
     * Translate all control characters to Escaped non control characters
     * Example: LF (#10) symbol is translated to sequence "\\n"
     * @param data that is being translated
     * @return string output of translation
     */
    String escape(String data);

    /**
     * Translate all Escaped characters to control characters
     * @param data that is being translated
     * @return string output of translation
     */
    String unEscape(String data);

    /**
     * Translate byte array into string representation
     * @param data that is being translated
     * @return string output of translation
     */
    String translate(byte[] data);

    String quotedText(String o);
}
