/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.rlp;

import org.junit.jupiter.api.Test;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.utils.Numeric;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals(value, reader.readByte());
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

    @Test
    void testBoolean() {
        //GIVEN
        final boolean valueT = true;
        final boolean valueF = false;
        //WHEN
        RlpReader reader = new RlpReader(
            new RlpWriteBuffer()
                .write(123L)
                .write(valueT)
                .write("123")
                .write(valueF)
                .toByteArray()
        );

        //THEN
        reader.readLong();
        assertEquals(valueT, reader.readBoolean());
        reader.readString();
        assertEquals(valueF, reader.readBoolean());
    }

    @Test
    void testTx() {
        //type=11, version=3, subtype=0
        String hexTx = "0b30a432663262363134392d643239652d343163612d386330642d66333334336635353430633682518084058831b9830a20b28837c756067b480e8e01a094c4e9ef9d92275e659aa7f26dcf56d54568739813d2c34559266c8150fabd46808064821388018d636c617373204465616c207b7dc786737472696e67b8524d53494700000000010094c4e9ef9d92275eb35588b8164b43d011bfaa20ecf69561871036d0a99688af42932725e57b3507e395a34b8a35281a500df3627e4cb35e3f3ba5b014fc7615ccbf4d8004f9db64";
        byte[] input = Numeric.hexStringToByteArray(hexTx);
        RlpList list = RlpDecoder.decode(input);
        assertNotNull(list);

    }

    @Test
    void testTx2() {
        String hexTx = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a08405c8ea668201218001a094c4e9ef9d92275e659aa7f26dcf56d54568739813d2c34559266c8150fabd4680808405f5e1000580f83dd880018d636c617373204465616c207b7dc786737472696e67e30401a0f93a3d6e1026f945a9c5a57af09e8c452e87f003d87f7c7e8d4325b00eef047ac0f84df84b8894c4e9ef9d92275eb8403cc80cffa8f8b97fa3858cb4f0e7309322591949b72ff49aec1d4bf9153d9c036fa1324513c0b67afa3ac6f93e143b70ba4df49e2ad12ab1097358c902d7a6f6";
        byte[] input = Numeric.hexStringToByteArray(hexTx);
        RlpList list = RlpDecoder.decode(input);
        assertNotNull(list);
    }

}