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
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.SneakyThrows;
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
        String expectedTxBytes = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a08405fd846e82014b887acb9b4da22ff07001a039dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d3796015288759dabed86da1fe80a6482138880f857f28001844465616c8d636c617373204465616c207b7dcd883078313233343536833132338a6a61766173637269707482138864e30401a07653845d30f81cef94756ac619684bbe2f306239bd40133c353004dabaa8f909c0f84df84b8839dc2e813bb45ff0b840480c3708fa937e37d7406f4524ed9a530ed3a076384ff09ac9d8f529d58e7901d6c1e65cf117d2c7452c02d50e42c3a43bb8e227521b591d632b6b91fa703ad3";

        //WHEN
        Transaction tx = transactionBuilderFactory.newTransaction(Convert.parseHexString(expectedTxBytes));

        //THEN
        assertEquals(3, tx.getVersion());
        assertEquals(11, tx.getType().getSpec().getType());
        assertEquals(8475119110439182312L, tx.getRecipientId());

        assertEquals(1440, tx.getDeadline());
        assertEquals(5000L, tx.getFuelLimit().longValue());
        assertEquals(100L, tx.getFuelPrice().longValue());
        assertEquals("APL-X5JH-TJKJ-DVGC-5T2V8", Convert2.rsAccount(tx.getSenderId()));//3705364957971254799L

        SmcPublishContractAttachment contractAttachment = ((SmcPublishContractAttachment) tx.getAttachment());
        assertEquals("class Deal {}", contractAttachment.getContractSource());
        assertEquals("Deal", contractAttachment.getContractName());
        assertEquals(List.of("0x123456", "123"), contractAttachment.getConstructorParams());

        //WHEN
        TxSerializer serializer = context.createSerializer(tx.getVersion());
        Result result = PayloadResult.createLittleEndianByteArrayResult();
        serializer.serialize(tx, result);

        //THEN
        assertNotNull(result.array());
        assertEquals(expectedTxBytes, Convert.toHexString(result.array()));
    }

}