/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializer;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class TxSerializerV3FromJsonTest {
    @Mock
    PrunableLoadingService prunableLoadingService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Chain chain;
    TxBContext context;
    TransactionJsonSerializer jsonSerializer;

    TransactionTestData td = new TransactionTestData();

    @BeforeEach
    void setUp() {
        context = TxBContext.newInstance(chain);
        doReturn(chain).when(blockchainConfig).getChain();
        jsonSerializer = new TransactionJsonSerializerImpl(prunableLoadingService, blockchainConfig);
        GenesisImporter.CREATOR_ID = 1739068987193023818L;
    }

    @Disabled
    //TODO not implemented yest, adjust signature field - change to RLP-encoded one
    @SneakyThrows
    @ParameterizedTest(name = "[{index}] tx")
    @ValueSource(strings = {
        //#1
        "{" +
            "  \"senderPublicKey\":\"39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152\"," +
            "  \"signature\":\"c0f84df84b8839dc2e813bb45ff0b840ad329b7d044a1afc5f7329a472e37008275b383283097423c57db44cae246c01934859980f781899dbfcfd4577e415217cfbd6f993a76fb7bd94b8ae7009cd62\"," +
            "  \"type\":11," +
            "  \"version\":3," +
            "  \"amountATM\":10," +
            "  \"ecBlockId\":\"8848336650635309168\"," +
            "  \"attachment\":{" +
            "     \"version.PublicKeyAnnouncement\":1," +
            "     \"recipientPublicKey\":\"114b9482b83043c3e850da9e9e8a497d3e3ba673c8b68ea6c42c6f157af6a764\"," +
            "     \"name\":\"Deal\"," +
            "     \"version.SmcPublish\":1," +
            "     \"language\":\"javascript\"," +
            "     \"source\":\"class Deal {}\"," +
            "     \"params\":[\"123\",\"0x98765\"]}," +
            "  \"subtype\":0," +
            "  \"recipient\":\"224212675506935204\"," +
            "  \"feeATM\":500000," +
            "  \"ecBlockHeight\":331," +
            "  \"id\":\"13070961154492438733\"," +
            "  \"deadline\":1440," +
            "  \"timestamp\":100667711" +
            "}"
    })
    void jsonDeSerializer_Tx(String jsonTx) {
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonTx);
        //long txId= Long.parseUnsignedLong((String) jsonObject.get("id"));
        TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
        Transaction txFromJson = transactionBuilderFactory.newTransaction(jsonObject);
        log.info("                  next tx");
        log.info("txId={} unsigned={}", txFromJson.getId(), Long.toUnsignedString(txFromJson.getId()));

        JSONObject jsonLegacy = jsonSerializer.toLegacyJsonFormat(txFromJson);

        assertNotNull(jsonLegacy);
        //assertEquals(txId, txFromJson.getId());
        for (Object key : jsonLegacy.keySet()) {
            String strKey = (String) key;
            Object value = jsonObject.get(key);
            log.info("key={} typeExpected={}, typeActual={}", key, value != null ? value.getClass().getSimpleName() : "null", jsonLegacy.get(key).getClass().getSimpleName());
            assertEquals(String.valueOf(value), String.valueOf(jsonLegacy.get(key)));
        }
    }
}
