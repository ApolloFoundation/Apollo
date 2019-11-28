/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import com.apollocurrency.aplwallet.apl.core.shard.helper.ValueParserImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase.EOT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueParserTest {
    private ValueParser parser = new ValueParserImpl();

    @ParameterizedTest
    @ValueSource(strings = {"'APL-ZW95-E7B5-MVVP-CCBDT'", "APL-ZW95-E7B5-MVVP-CCBDT"})
    void parseStringObject(String input) {
        String expected = "APL-ZW95-E7B5-MVVP-CCBDT";
        assertEquals(expected, parser.parseStringObject(input));
    }

    @Test
    void parseStringObjectWithImbalancedQuotes() {
        String o = "'APL-ZW95-E7B5-MVVP-CCBDT";
        assertThrows(RuntimeException.class, () -> parser.parseStringObject(o));
    }

    @Test
    void parseArrayObject() {
        String o = "'tag1'"+EOT+"'tag2'"+EOT+"'batman'"+EOT;
        Object[] expected = {"tag1", "tag2","batman"};
        assertArrayEquals(expected, parser.parseArrayObject(o));
    }

    @Test
    void parseArrayObjectWrongFormat() {
        String o = "('tag1','tag2','batman')";
        assertThrows(RuntimeException.class, () -> parser.parseArrayObject(o));
    }

    @Test
    void parseBinaryObject() {
        String binary = "OTMxYTgwMTFmNGJhMWNkYzBiY2FlODA3MDMyZmUxOGIxZTRmMGI2MzRmOGRhNjAxNmU0MjFkMDZjN2UxMzY5Mw==";
        byte[] actual = parser.parseBinaryObject(binary);
        byte[] expected = {57, 51, 49, 97, 56, 48, 49, 49, 102, 52, 98, 97, 49, 99, 100, 99, 48, 98, 99, 97, 101,
                56, 48, 55, 48, 51, 50, 102, 101, 49, 56, 98, 49, 101, 52, 102, 48, 98, 54, 51, 52, 102,
                56, 100, 97, 54, 48, 49, 54, 101, 52, 50, 49, 100, 48, 54, 99, 55, 101, 49, 51, 54, 57, 51};
        assertArrayEquals(expected, actual);
    }

    @Test
    void parseObject() {
        Object o = new BigInteger("1234567890");
        Object actual = parser.parseObject(o);
        assertEquals(o, actual);
    }
}