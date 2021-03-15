/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author andrew.zinchenko@gmail.com
 */
class RlpReaderTest {

    @Test
    void decodeWithNumberFormatException() {
        //GIVEN
        long value1 = 0L;
        BigInteger value2 = new BigInteger("123");
        BigInteger value3 = new BigInteger("123456789012345678901234567890");

        String hexInput = Numeric.toHexString(RlpEncoder.encode(RlpString.create(value1))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value3)));
        //WHEN
        RlpReader reader = new RlpReader(hexInput);

        //THEN
        assertNotNull(reader);

        assertEquals(value1, reader.readLong());
        assertEquals(value2.longValue(), reader.readLong());
        assertThrows(NumberFormatException.class, reader::readLong);
    }

    @Test
    void decodeWithNoSuchElementException() {
        //GIVEN
        long value1 = 0L;

        //WHEN
        RlpReader reader = new RlpReader(Numeric.toHexString(RlpEncoder.encode(RlpString.create(value1))));

        //THEN
        assertNotNull(reader);

        assertEquals(value1, reader.readLong());
        assertThrows(NoSuchElementException.class, reader::readLong);
    }

    @Test
    void decode() {
        //GIVEN
        long value1 = 0L;
        long value2 = 1L;
        long value3 = 0xFFL;
        long value4 = 1234567890L;
        BigInteger value5 = new BigInteger("123");
        BigInteger value6 = new BigInteger("123456789012345678901234567890");
        String value7 = "TheFirstShortString";
        byte[] value8 = new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

        String hexInput = Numeric.toHexString(RlpEncoder.encode(RlpString.create(value1))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value3))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value4))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value5))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value6))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value7))) +
            Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value8)));

        //WHEN
        RlpReader reader = new RlpReader(hexInput);

        //THEN
        assertNotNull(reader);
        assertEquals(8, reader.size());

        assertEquals(value1, reader.readLong());
        assertEquals(value2, reader.readLong());
        assertEquals(value3, reader.readLong());
        assertEquals(value4, reader.readLong());
        assertEquals(value5, reader.readBigInteger());
        assertEquals(value6, reader.readBigInteger());
        assertEquals(value7, reader.readString());
        assertArrayEquals(value8, reader.read());
    }

    @Test
    void decodeLoop() {
        //GIVEN
        long value1 = 0xFFL;
        BigInteger value2 = new BigInteger("123456789012345678901234567890");

        String hexInput = Numeric.toHexString(RlpEncoder.encode(RlpString.create(value1)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)))
            + Numeric.toHexStringNoPrefix(RlpEncoder.encode(RlpString.create(value2)));

        //WHEN
        RlpReader reader = new RlpReader(hexInput);

        //THEN
        assertNotNull(reader);
        assertEquals(10, reader.size());

        assertEquals(value1, reader.readLong());

        int i=0;
        while (reader.hasNext()) {
            i++;
            assertEquals(value2, reader.readBigInteger());
        }
        assertEquals(9, i);
    }

    @Test
    void decodeWithList() {
        //GIVEN
        byte[] value1 = new byte[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        String value2 = "The short example string.";
        long value3 = 123456L;
        BigInteger value4 = new BigInteger("1234567890123456789012345678901234567890");

        String hexInput =
              Numeric.toHexString(Arrays.concatenate(new byte[]{(byte) 0x8B}, value1))
            + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x99}, value2.getBytes(StandardCharsets.UTF_8)))
                + Numeric.toHexStringNoPrefix(new byte[]{(byte) 0xc0+48})
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x83}, BigInteger.valueOf(value3).toByteArray()))
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x99}, value2.getBytes(StandardCharsets.UTF_8)))
                    + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x91}, value4.toByteArray()))
            + Numeric.toHexStringNoPrefix(Arrays.concatenate(new byte[]{(byte) 0x91}, value4.toByteArray()));

        //WHEN
        RlpReader reader = new RlpReader(hexInput);

        //THEN
        assertNotNull(reader);
        assertEquals(4, reader.size());

        assertArrayEquals(value1, reader.read());
        assertEquals(value2, reader.readString());
        RlpReader innerReader = reader.readListReader();
        assertEquals(value4, reader.readBigInteger());

        assertNotNull(innerReader);
        assertEquals(3, innerReader.size());
        assertEquals(value3, innerReader.readLong());
        assertEquals(value2, innerReader.readString());
        assertEquals(value4, innerReader.readBigInteger());
    }
}