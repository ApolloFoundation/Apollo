/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;


import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.types.smc.SmcPublishTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
class TxSerializerV3Test {
    @Mock
    AccountService accountService;
    @Mock
    AccountPublicKeyService accountPublicKeyService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Chain chain;
    TxBContext context;
    TransactionTypeFactory transactionTypeFactory;
    TransactionBuilderFactory transactionBuilderFactory;

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 1739068987193023818L);
    }

    @BeforeEach
    void setUp() {
        doReturn(chain).when(blockchainConfig).getChain();
        context = TxBContext.newInstance(chain);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(
            new SmcPublishTransactionType(blockchainConfig, accountService, accountPublicKeyService)
        ));
        transactionBuilderFactory = new TransactionBuilderFactory(transactionTypeFactory, blockchainConfig);
    }

    @SneakyThrows
    @Test
    void serializeV3toByteArray() {
        //GIVEN
        //Rlp encoded Tx V3
        String expectedTxBytes = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a0840600113f82014b887acb9b4da22ff07001a039dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d3796015288031c903d8dbb15a40a6482138880f852ed8001844465616c8d636c617373204465616c207b7dcc8331323387307839383736358a6a617661736372697074e30401a0114b9482b83043c3e850da9e9e8a497d3e3ba673c8b68ea6c42c6f157af6a764c0f84df84b8839dc2e813bb45ff0b840ad329b7d044a1afc5f7329a472e37008275b383283097423c57db44cae246c01934859980f781899dbfcfd4577e415217cfbd6f993a76fb7bd94b8ae7009cd62";

        //WHEN
        Transaction tx = transactionBuilderFactory.newTransaction(Convert.parseHexString(expectedTxBytes));

        //THEN
        assertEquals(3, tx.getVersion());
        assertEquals(11, tx.getType().getSpec().getType());
        assertEquals(224212675506935204L, tx.getRecipientId());

        assertEquals(1440, tx.getDeadline());
        assertEquals(5000L, tx.getFuelLimit().longValue());
        assertEquals(100L, tx.getFuelPrice().longValue());
        assertEquals("APL-X5JH-TJKJ-DVGC-5T2V8", Convert2.rsAccount(tx.getSenderId()));//3705364957971254799L

        SmcPublishContractAttachment contractAttachment = ((SmcPublishContractAttachment) tx.getAttachment());
        assertEquals("class Deal {}", contractAttachment.getContractSource());
        assertEquals("Deal", contractAttachment.getContractName());
        assertEquals(List.of("123", "0x98765"), contractAttachment.getConstructorParams());

        //WHEN
        TxSerializer serializer = context.createSerializer(tx.getVersion());
        Result result = PayloadResult.createLittleEndianByteArrayResult();
        serializer.serialize(tx, result);

        //THEN
        assertNotNull(result.array());
        assertEquals(expectedTxBytes, Convert.toHexString(result.array()));

        //WHEN
        PayloadResult jsonResult = PayloadResult.createJsonResult();
        serializer.serialize(tx, jsonResult);

        //THEN
        assertNotNull(jsonResult.getBuffer());
        JSONObject object = ((JsonBuffer) jsonResult.getBuffer()).getJsonObject();
        assertEquals((short) 1440, object.get("deadline"));
    }

}