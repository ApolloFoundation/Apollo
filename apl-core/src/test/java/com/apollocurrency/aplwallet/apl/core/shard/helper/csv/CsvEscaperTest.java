/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class CsvEscaperTest {
    private CsvEscaper translator = new CsvEscaperImpl();

    @Test
    void escape() {
        String[] str = {"AbcDef \" 12345", "\\AbcDef \\\\ 12345", "AbcDef \n 12345\f", "AbcDef \r 12345", "AbcDef \r 123 \n 45", "AbcDef \r\n 12345"};
        String[] escapedStr = {"AbcDef \\\" 12345", "\\\\AbcDef \\\\\\\\ 12345", "AbcDef \\n 12345\\f", "AbcDef \\r 12345", "AbcDef \\r 123 \\n 45", "AbcDef \\r\\n 12345"};
        for (int i = 0; i < str.length; i++) {
            String es = translator.escape(str[i]);
            String us = translator.unEscape(es);
            log.debug(" escaped string=[{}]", es);
            assertEquals(escapedStr[i], es);
            assertEquals(str[i], us);
        }
    }
}
