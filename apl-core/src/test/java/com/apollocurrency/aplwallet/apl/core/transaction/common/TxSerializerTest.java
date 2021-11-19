/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class TxSerializerTest {

    @Mock
    Chain chain;
    TxBContext context;

    TransactionTestData td = new TransactionTestData();

    @BeforeEach
    void setUp() {
        context = TxBContext.newInstance(chain);
    }

    @Test
    void serializeV1toByteArray() {
        //GIVEN
        TxSerializer serializer = context.createSerializer(2);
        Transaction t1 = td.TRANSACTION_14;
        Result result = PayloadResult.createLittleEndianByteArrayResult();

        //WHEN
        serializer.serialize(t1, result);

        //THEN
        assertNotNull(result.array());
        /*System.out.println(Convert.toHexString(t1.bytes()));
        System.out.println(Convert.toHexString(result.array()));
        assertArrayEquals(t1.bytes(), result.array());*/
    }

    @Test
    void serializeV1toJson() {
        //GIVEN
        TxSerializer serializer = context.createSerializer(2);
        Transaction t1 = td.TRANSACTION_14;
        PayloadResult result = PayloadResult.createJsonResult();

        //WHEN
        serializer.serialize(t1, result);

        //THEN
        assertNotNull(result.array());

    }
}