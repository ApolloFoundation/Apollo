/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author andrew.zinchenko@gmail.com
 */
class RlpWriteBufferTest {
    @Test
    void writeArray() {
        //GIVEN
        byte[] value = new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        byte[] expected = Arrays.concatenate(new byte[]{(byte) 0x8B}, value);
        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        buffer.write(value);
        byte[] out = buffer.toByteArray();
        //THEN
        assertArrayEquals(expected, out);
    }

    @Test
    void writeString() {
        //GIVEN
        String value = "The short example string.";
        byte[] expected = Arrays.concatenate(new byte[]{(byte) 0x99}, value.getBytes(StandardCharsets.UTF_8));
        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        buffer.write(value);
        byte[] out = buffer.toByteArray();
        //THEN
        assertArrayEquals(expected, out);
    }

    @Test
    void writeInt() {
        //GIVEN
        int value = 1234;//two bytes
        byte[] expected = Arrays.concatenate(new byte[]{(byte) 0x80 + 2}, BigInteger.valueOf(value).toByteArray());
        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        buffer.write(value);
        byte[] out = buffer.toByteArray();
        //THEN
        assertArrayEquals(expected, out);
    }

    @Test
    void writeLong() {
        //GIVEN
        long value = 123456L;//three bytes
        byte[] expected = Arrays.concatenate(new byte[]{(byte) 0x80 + 3}, BigInteger.valueOf(value).toByteArray());
        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        buffer.write(value);
        byte[] out = buffer.toByteArray();
        //THEN
        assertArrayEquals(expected, out);
    }

    @Test
    void writeBigInteger() {
        //GIVEN
        BigInteger value = new BigInteger("1234567890123456789012345678901234567890");
        byte[] expected = Arrays.concatenate(new byte[]{(byte) 0x91}, value.toByteArray());
        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        buffer.write(value);
        byte[] out = buffer.toByteArray();
        //THEN
        assertArrayEquals(expected, out);
    }

    @Test
    void write() {
        //GIVEN
        byte[] value1 = new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        String value2 = "The short example string.";
        long value3 = 123456L;
        BigInteger value4 = new BigInteger("1234567890123456789012345678901234567890");

        String expected =
              Numeric.toHexString(Arrays.concatenate(new byte[]{(byte) 0x8B}, value1))
            + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x99}, value2.getBytes(StandardCharsets.UTF_8)))
                + Numeric.toHexStringNoPrefix(new byte[]{(byte) 0xc0+48})
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x83}, BigInteger.valueOf(value3).toByteArray()))
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x99}, value2.getBytes(StandardCharsets.UTF_8)))
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x91}, value4.toByteArray()))
            + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x91}, value4.toByteArray()));

        //WHEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        List<RlpType> list = new RlpListBuilder()
            .add(value3)
            .add(value2)
            .add(value4)
            .build();

        byte[] out = buffer
                    .write(value1)
                    .write(value2)
                    .write(list)
                    .write(value4)
                    .toByteArray();

        //THEN
        assertNotNull(out);
        System.out.println(Numeric.toHexString(out));

        assertEquals(expected, Numeric.toHexString(out));
    }
}