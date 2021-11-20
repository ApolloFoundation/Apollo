/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
class MessagesTest {

    @Test
    void format() {
        //GIVEN
        var format0 = "Unable to extract valid account credentials, ";
        var pattern = format0 + "''{0}''";
        var arg0 = "0x1234567890";
        //WHEN
        var formatted = Messages.format(pattern, new Object[]{arg0});
        //THEN
        assertEquals(format0 + "'" + arg0 + "'", formatted);
    }

    @Test
    void format1Quotes() {
        //GIVEN
        var format0 = "Unable to extract valid account credentials, ";
        var pattern = format0 + "'{0}'";
        var arg0 = "0x1234567890";
        //WHEN
        var formatted = Messages.format(pattern, new Object[]{arg0});
        //THEN
        assertEquals(format0 + "{0}", formatted);
    }

}