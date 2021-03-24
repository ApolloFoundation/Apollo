/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
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

    //TODO not implemented yest, adjust signature field - change to RLP-encoded one
    @Disabled
    @SneakyThrows
    @ParameterizedTest(name = "[{index}] tx")
    @ValueSource(strings = {
        //#1
        "{ \"id\": \"9175410632340250178\"," + //actual id=16091200120166840182
            "    \"version\": 3,\n" +
            "    \"type\": 11,\n" +
            "    \"subtype\": 1,\n" +
            "    \"senderPublicKey\": \"39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152\",\n" +
            "    \"signature\": \"7ecae5825a24dedc42dd11e2239ced7ad797c6d6c9aedc3d3275204630b7e20832f9543d1063787ea1f32ab0993ea733aa46a52664755d9e54f211cdc3c5c5fd\",\n" +
            "    \"transactionIndex\": 0,\n" +
            "    \"requestProcessingTime\": 6,\n" +
            "    \"confirmations\": 3597408,\n" +
            "    \"fullHash\": \"429efb505b9b557f5d2a1d6d506cf75de6c3692ca1a21217ae6160c7658c7312\",\n" +
            "    \"amountATM\": \"0\",\n" +
            "    \"phased\": false,\n" +
            "    \"ecBlockId\": \"4407210215527895706\",\n" +
            "    \"signatureHash\": \"09683525960033f729de68e079dcc52428d9f63729bb663f158b1abb5ad956e2\",\n" +
            "    \"attachment\": {\n" +
            "        \"version.SmcCallMethod\":1," +
            "        \"amount\":\"1\"," +
            "        \"contractMethod\":\"purchase\"," +
            "        \"fuelPrice\":\"2\",\"fuelLimit\":\"10\"," +
            "        \"params\":[\"123\",\"0x0A0B0C0D0E0F\"]" +
            "    }," +
            "    \"senderRS\": \"APL-X5JH-TJKJ-DVGC-5T2V8\",\n" +
            "    \"sender\": \"3705364957971254799\",\n" +
            "    \"feeATM\": \"1000000000\",\n" +
            "    \"ecBlockHeight\": 552605,\n" +
            "    \"block\": \"12480056480475132870\",\n" +
            "    \"blockTimestamp\": 41974339,\n" +
            "    \"deadline\": 1440,\n" +
            "    \"transaction\": \"9175410632340250178\",\n" +
            "    \"timestamp\": 41974329,\n" +
            "    \"height\": 553326\n" +
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
