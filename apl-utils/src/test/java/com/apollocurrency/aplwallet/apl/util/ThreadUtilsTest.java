/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadUtilsTest {

    @Test
    void lastStacktrace() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int length = stack.length;
        String result0 = ThreadUtils.lastStacktrace();
        assertNotNull(result0);
        String result = ThreadUtils.lastStacktrace(stack, length);
        assertNotNull(result);
        assertTrue(result.length()<result0.length());
        String result2 = ThreadUtils.lastStacktrace(stack, length+10);
        assertNotNull(result2);
        assertEquals(result, result2);
        String result3 = ThreadUtils.lastStacktrace(stack, length-2);
        assertNotNull(result3);
        assertTrue(result3.length()<result.length());
    }

    @Test
    void getStacktraceSpec() {
        StackTraceElement element  = Thread.currentThread().getStackTrace()[0];
        String result = ThreadUtils.getStacktraceSpec(element);
        assertNotNull(result);
        assertTrue(result.contains("."));
        assertTrue(result.contains(":"));
    }
}