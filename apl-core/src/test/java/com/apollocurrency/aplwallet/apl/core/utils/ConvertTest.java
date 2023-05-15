/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.utils;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.smc.util.HexUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
class ConvertTest {

    @Test
    void convertLong() {
        //GIVEN
        var s = "13087999612749769618";//Unsigned long

        var l0 = Long.parseUnsignedLong(s);
        //WHEN
        var big = BigInteger.valueOf(l0);
        //THEN
        assertNotEquals(big, new BigInteger(s));
        assertEquals(l0, big.longValueExact());

        //WHEN
        var rc3 = Convert.longToBytes(l0);
        var rc4 = Convert.bytesToLong(rc3);
        //THEN
        assertEquals(l0, rc4);

        //WHEN
        final long actual = Convert.bytesToLong(big.toByteArray());
        //THEN
        assertEquals(l0, actual);

        //WHEN
        var rc = Convert.toHexString(big.toByteArray());
        //THEN
        assertEquals(big, new BigInteger(Convert.parseHexString(rc)));
        assertNotEquals(big, new BigInteger(rc, 16));
        assertNotEquals(big, new BigInteger("-" + rc, 16));
    }

    @ValueSource(strings = {"0x123456789", "0x41d6e1396b8ebe2", "0xb5a1ea4dfa666b92"})
    @ParameterizedTest
    void convertHexParseLong(String s) {
        //GIVEN
        final byte[] bytes = HexUtils.parseHex(s);
        var big = new BigInteger(bytes);
        var l0 = big.longValueExact();

        //WHEN
        var rc4 = Convert.bytesToLong(Convert.longToBytes(l0));
        //THEN
        assertEquals(l0, rc4);

        //WHEN
        var rc = Convert.toHexString(big.toByteArray());
        //THEN
        assertEquals(big, new BigInteger(Convert.parseHexString(rc)));
        assertEquals(big, new BigInteger(HexUtils.parseHex(rc)));

        //WHEN
        //THEN
        assertEquals(l0, HexUtils.toLong(bytes));
    }

}

