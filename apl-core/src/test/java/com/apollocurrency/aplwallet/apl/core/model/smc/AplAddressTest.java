/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.model.smc;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
class AplAddressTest {

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
}