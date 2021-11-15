/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;


import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.PostponedContractService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.CachedTransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcPublishContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.types.smc.SmcPublishContractTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.JsonBuffer;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.smc.contract.fuel.FuelValidator;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
    @Mock
    PostponedContractService contractService;
    @Mock
    ContractToolService contractToolService;
    @Mock
    SmcBlockchainIntegratorFactory integratorFactory;

    TxBContext context;
    TransactionTypeFactory transactionTypeFactory;
    TransactionBuilderFactory transactionBuilderFactory;
    FuelValidator fuelValidator;

    @BeforeAll
    static void beforeAll() {
        Convert2.init("APL", 1739068987193023818L);
    }

    @BeforeEach
    void setUp() {
        doReturn(chain).when(blockchainConfig).getChain();
        fuelValidator = new SmcFuelValidator(blockchainConfig);
        context = TxBContext.newInstance(chain);
        transactionTypeFactory = new CachedTransactionTypeFactory(List.of(
            new SmcPublishContractTransactionType(blockchainConfig, accountService, contractService, contractToolService, fuelValidator, integratorFactory, new SmcConfig())
        ));
        transactionBuilderFactory = new TransactionBuilderFactory(transactionTypeFactory, blockchainConfig);
    }

    @Disabled
    @SneakyThrows
    @Test
    void serializeV3toByteArray() {
        //GIVEN
        //Rlp encoded Tx V3
        //TODO fix tx bytes, cause the structure was changed
        String expectedTxBytes = "0b30a466666666663662642d303061332d333436622d616164362d3631666566633062643163368205a084060246ca82014b887acb9b4da22ff07001a039dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d3796015288461282f08a3184740a6482138880f861f83b8001844465616c8d636c617373204465616c207b7d9a226669727374506172616d222c3132332c2230783938373635228a6a617661736372697074e30401a030b7c63fde11877d7affd4c64f47f314dc1d7baaa7faf4fab905a8bd61e6a732c0f84df84b8839dc2e813bb45ff0b84055719abd90fb66d63da4d2fafc88b7f270edb7ffe399fd569b3d4478cbd3300fe6092c96b309de1adbf9a80fa04e67d1fa5dd68e60901fd81640c772e87f520e";

        //WHEN
        Transaction tx = transactionBuilderFactory.newTransaction(Convert.parseHexString(expectedTxBytes));

        //THEN
        assertEquals(3, tx.getVersion());
        assertEquals(11, tx.getType().getSpec().getType());
        assertEquals(5049242101858010228L, tx.getRecipientId());

        assertEquals(1440, tx.getDeadline());
        assertEquals("APL-X5JH-TJKJ-DVGC-5T2V8", Convert2.rsAccount(tx.getSenderId()));//3705364957971254799L

        SmcPublishContractAttachment contractAttachment = ((SmcPublishContractAttachment) tx.getAttachment());
        assertEquals("class Deal {}", contractAttachment.getContractSource());
        assertEquals("Deal", contractAttachment.getContractName());
        assertEquals("\"firstParam\",123,\"0x98765\"", contractAttachment.getConstructorParams());
        assertEquals(5000L, contractAttachment.getFuelLimit().longValue());
        assertEquals(100L, contractAttachment.getFuelPrice().longValue());

        for (AbstractAppendix appendage : tx.getAppendages()) {
            Class clazz = appendage.getClass();
        }

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