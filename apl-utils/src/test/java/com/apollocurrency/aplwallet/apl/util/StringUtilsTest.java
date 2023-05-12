package com.apollocurrency.aplwallet.apl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void test_byPrecedenceFirst() {
        String result = StringUtils.byPrecedence(null, "1", "2", "3");
        assertEquals("1", result);
    }

    @Test
    void test_byPrecedenceSecond() {
        String result = StringUtils.byPrecedence(null, "", "2", "3");
        assertEquals("2", result);
    }

    @Test
    void test_byPrecedenceThird() {
        String result = StringUtils.byPrecedence(null, "", " ", null, "3");
        assertEquals("3", result);
    }
}