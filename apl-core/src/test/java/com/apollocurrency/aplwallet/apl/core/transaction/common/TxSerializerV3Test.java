/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;


import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class TxSerializerV3Test {
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Chain chain;
    TxBContext context;

    TransactionTestData td = new TransactionTestData();

    @BeforeEach
    void setUp() {
        context = TxBContext.newInstance(chain);
        doReturn(chain).when(blockchainConfig).getChain();
    }

    @SneakyThrows
    @Test
    void serializeV3toByteArray() {
        //GIVEN
        //Rlp encoded Tx V3
        String expected = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a08405c8ff3d82012188c673b858266226e601a094c4e9ef9d92275e659aa7f26dcf56d54568739813d2c34559266c8150fabd46889c0370240713f694808405f5e1000580f83dd880018d636c617373204465616c207b7dc786737472696e67e30401a015f174a507a4c6256f20af0d01b592b495fbeeaa34220c6c26d87b1e9d0bc031c0f84df84b8894c4e9ef9d92275eb84024695281c4dbe2f63d64a167f659e00753ded5ccfba4cb41c4daa339f97c070c11e5b033bc0b35d6cdcf012985c97ad34895dedf219e8448cd1cb103c5e4af21";

        //WHEN
        TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
        Transaction tx = transactionBuilderFactory.newTransaction(Convert.parseHexString(expected));

        //THEN
        assertEquals(3, tx.getVersion());
        assertEquals(11, tx.getType().getSpec().getType());
        assertEquals(-7204791678822779244L, tx.getRecipientId());
        assertEquals(1440, tx.getDeadline());


        //WHEN
        TxSerializer serializer = context.createSerializer(tx.getVersion());
        Result result = PayloadResult.createLittleEndianByteArrayResult();
        serializer.serialize(tx, result);

        //THEN
        assertNotNull(result.array());
        assertEquals(expected, Convert.toHexString(result.array()));
    }

}