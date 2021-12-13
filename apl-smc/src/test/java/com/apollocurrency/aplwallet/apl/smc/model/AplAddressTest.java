/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.model;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
class AplAddressTest {
    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {1234567890L, -987654321L, 0L})
    void getLongId(long id) {
        //GIVEN
        //WHEN
        AplAddress address = new AplAddress(id);

        //THEN
        assertEquals(id, address.getLongId());
    }

    @ParameterizedTest
    @ValueSource(longs = {1234567890L, -987654321L, 0L})
    void get(long id) {
        //GIVEN
        //WHEN
        AplAddress address = new AplAddress(id);
        byte[] b = address.get();

        //THEN
        assertEquals(id, new BigInteger(b).longValueExact());
    }

    @ParameterizedTest
    @ValueSource(longs = {1234567890L, -987654321L, 0L})
    void getHex(long id) {
        //GIVEN

        //WHEN
        AplAddress address = new AplAddress(id);
        String hex = address.getHex().substring(2);

        //THEN
        BigInteger bi = new BigInteger(Convert.parseHexString(hex));
        assertEquals(id, bi.longValueExact());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0x0", "0xffffffffb669fd2e", "0xf74cf6524db7ad47", "0xe74cf6524db7ad47"})
    void newInstance(String id) {
        //GIVEN
        //WHEN
        var address = AplAddress.valueOf(id);
        var longId = address.getLongId();
        var bytes = address.get();
        //THEN
        assertEquals(address, new AplAddress(longId));
        assertEquals(address, new AplAddress(address));
        assertEquals(address, new AplAddress(bytes));
        assertEquals("0x" + Convert.toHexString(bytes), address.getHex());
    }

    @Test
    void newInstanceZero() {
        //GIVEN
        //WHEN
        var address = AplAddress.valueOf("0x0");
        var addressZ = new AplAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
        //THEN
        assertEquals(address, addressZ);
        assertEquals(0L, address.getLongId());
        assertEquals(0L, addressZ.getLongId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0x00e74cf6524db7ad47", "0x00b5a1ea4dfa666b92"})
    void newInstanceOutOfRange(String s) {
        //GIVEN //WHEN //THEN
        assertThrows(ArithmeticException.class, () -> AplAddress.valueOf(s));
    }
}
