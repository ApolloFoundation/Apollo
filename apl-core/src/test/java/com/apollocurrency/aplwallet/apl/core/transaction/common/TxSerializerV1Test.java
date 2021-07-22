/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class TxSerializerV1Test {

    @Mock
    Chain chain;
    TxBContext context;

    TransactionTestData td = new TransactionTestData();

    @BeforeAll
    static void beforeAll() {
        GenesisImporter.CREATOR_ID = 1739068987193023818L;
    }

    @BeforeEach
    void setUp() {
        context = TxBContext.newInstance(chain);
    }

    @Test
    void serializeV1toByteArray() {
        //GIVEN
        String expected = "0310397a8002a00539dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d379601524add89a5076a2218000000000000000000ca9a3b0000000000000000000000000000000000000000000000000000000000000000000000007ecae5825a24dedc42dd11e2239ced7ad797c6d6c9aedc3d3275204630b7e20832f9543d1063787ea1f32ab0993ea733aa46a52664755d9e54f211cdc3c5c5fd200000009d6e08009a520333578e293d010c00546573742070726f647563741500546573742070726f6475637420666f722073616c650c007461672074657374646174610200000000e40b540200000001b9dd15475e2f8da755f1b63933051dede676b223c86e70f54c7182b976d2f86d";
        TxSerializer serializer = context.createSerializer(2);
        Transaction t1 = td.TRANSACTION_14;
        Result result = PayloadResult.createLittleEndianByteArrayResult();
        System.out.println("generator_id=" + Long.toHexString(GenesisImporter.CREATOR_ID));

        //WHEN
        serializer.serialize(t1, result);

        //THEN
        assertNotNull(result.array());
        assertTrue(result.size() > 0);
        assertTrue(result.size() >= result.payloadSize());
        assertEquals(expected, Convert.toHexString(result.array()));
    }

}