/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcBlockchainIntegratorFactory;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcCallMethodTransactionTypeTest {

    private static final String CHAIN_ID_TN1 = "a2e9b946-290b-48b6-9985-dc2e5a5860a1";
    private static final String SMC_CALL_METHOD_ATTACHMENT_JSON = "{" +
        " \"version.SmcCallMethod\":1," +
        " \"contractMethod\":\"purchase\"," +
        " \"params\": \"\\\"123\\\",\\\"0x0A0B0C0D0E0F\\\"\"" +
        " \"fuelLimit\": 5000, " +
        " \"fuelPrice\": 100, " +
        "}";

    @Mock
    Chain chain;
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AccountService accountService;
    @Mock
    SmcContractService contractService;
    @Mock
    SmcBlockchainIntegratorFactory integratorFactory;

    private SmcCallMethodTransactionType smcCallMethodTransactionType;

    @BeforeEach
    void setUp() {
        initMocks(this);
        GenesisImporter.CREATOR_ID = 1739068987193023818L;//TN1
        doReturn(UUID.fromString(CHAIN_ID_TN1)).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();
        smcCallMethodTransactionType = new SmcCallMethodTransactionType(blockchainConfig, accountService, contractService, new SmcFuelValidator(blockchainConfig), integratorFactory, new SmcConfig());
    }

    @SneakyThrows
    @Test
    void parseAttachment_Json() {
        //GIVEN
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(SMC_CALL_METHOD_ATTACHMENT_JSON);
        //WHEN
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) smcCallMethodTransactionType.parseAttachment(jsonObject);

        //THEN
        assertNotNull(attachment);
        assertEquals("purchase", attachment.getMethodName());
        assertEquals("\"123\",\"0x0A0B0C0D0E0F\"", attachment.getMethodParams());
        assertEquals( BigInteger.valueOf(5000L), attachment.getFuelLimit());
        assertEquals( BigInteger.valueOf(100L), attachment.getFuelPrice());
    }

    @SneakyThrows
    @Test
    void parseAttachment_Rlp() {
        //GIVEN
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(SMC_CALL_METHOD_ATTACHMENT_JSON);
        SmcCallMethodAttachment att1 = (SmcCallMethodAttachment) smcCallMethodTransactionType.parseAttachment(jsonObject);
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        RlpList.RlpListBuilder listBuilder = RlpList.builder();
        att1.putBytes(listBuilder);
        buffer.write(listBuilder.build());
        byte[] input = buffer.toByteArray();

        //WHEN
        RlpReader reader = new RlpReader(input).readListReader().readListReader();
        reader.readByte();//read appendix flag = 0x00
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) smcCallMethodTransactionType.parseAttachment(reader);

        //THEN
        assertNotNull(attachment);
        assertEquals("purchase", attachment.getMethodName());
        assertEquals("\"123\",\"0x0A0B0C0D0E0F\"", attachment.getMethodParams());
        assertEquals( BigInteger.valueOf(5000L), attachment.getFuelLimit());
        assertEquals( BigInteger.valueOf(100L), attachment.getFuelPrice());
    }

}