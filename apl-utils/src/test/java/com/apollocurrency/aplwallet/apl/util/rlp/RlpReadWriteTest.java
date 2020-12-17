/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
class RlpReadWriteTest {

    @Test
    void testByte() {
        //GIVEN
        byte value = 24;
        //WHEN
        RlpReader reader = new RlpReader(
            new RlpWriteBuffer()
                .write(value)
                .toByteArray()
        );

        //THEN
        assertEquals(value, reader.read()[0]);
    }

    @Test
    void testByteArray() {
        //GIVEN
        byte[] value = {10, 11, 12, 13, 14, 15};
        //WHEN
        RlpReader reader = new RlpReader(
            new RlpWriteBuffer()
                .write(value)
                .toByteArray()
        );

        //THEN
        assertArrayEquals(value, reader.read());
    }

    @Test
    void testLong() {
        //GIVEN
        long value = 987654321L;
        //WHEN
        RlpReader reader = new RlpReader(
            new RlpWriteBuffer()
                .write(value)
                .toByteArray()
        );

        //THEN
        assertEquals(value, reader.readLong());
    }

}