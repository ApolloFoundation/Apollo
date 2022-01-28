/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.SmcConfig;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.ContractToolService;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcContractRepository;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.SmcFuelValidator;
import com.apollocurrency.aplwallet.apl.core.service.state.smc.impl.SmcBlockchainIntegratorFactoryCreator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author andrew.zinchenko@gmail.com
 */
@ExtendWith(MockitoExtension.class)
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
    private BlockchainConfig blockchainConfig;
    @Mock
    private Blockchain blockchain;
    @Mock
    private AccountService accountService;
    @Mock
    SmcContractRepository contractService;
    @Mock
    ContractToolService contractToolService;
    @Mock
    SmcBlockchainIntegratorFactoryCreator integratorFactory;

    private SmcCallMethodTransactionType smcCallMethodTransactionType;

    @BeforeEach
    void setUp() {
        GenesisImporter.CREATOR_ID = 1739068987193023818L;//TN1
        smcCallMethodTransactionType = new SmcCallMethodTransactionType(blockchainConfig,
            blockchain,
            accountService,
            contractService, contractToolService,
            new SmcFuelValidator(blockchainConfig),
            integratorFactory,
            new SmcConfig());
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

}