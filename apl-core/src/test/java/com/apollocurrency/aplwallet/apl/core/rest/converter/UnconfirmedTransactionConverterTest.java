/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.converter;

import com.apollocurrency.aplwallet.api.dto.UnconfirmedTransactionDTO;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionJsonSerializerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
@ExtendWith(MockitoExtension.class)
class UnconfirmedTransactionConverterTest {
    String anonymousPrivatePaymentTx = "{\"signature\":\"1575f2dd172efc8af21375065b0e79ff1bc942f51c270479f23e8466d4815602eacb972e12ba1775dd547b7b251a5736b12c0f69d140a2f4af2a10daffdcdce0\",\"transactionIndex\":-1,\"type\":0,\"phased\":false,\"ecBlockId\":\"8643770779200471609\",\"signatureHash\":\"cde7c2b474d43d606fd0940bdfb32a841675cf63175d16ec0007cd16d59b0e68\",\"attachment\":{\"version.PrivatePayment\":0},\"senderRS\":\"APL-559G-5XTX-PL5Z-DJN2F\",\"subtype\":1,\"recipientRS\":\"APL-UPTK-MG4L-DTLP-5UFRR\",\"block\":\"0\",\"blockTimestamp\":-1,\"deadline\":1440,\"timestamp\":102630512,\"height\":15954,\"senderPublicKey\":\"5dc680ad7103d97d7d08bb7ba0a66e73ff7e1d9694550a13f7a65fbea886d0e3\",\"requestProcessingTime\":0,\"confirmations\":0,\"fullHash\":\"2e688a245b7cb28962667790108ef0dbf58e907e54c662262089d5c34347f61a\",\"version\":1,\"amountATM\":\"615071200000000\",\"sender\":\"13151232326636768494\",\"recipient\":\"4313809195207841585\",\"feeATM\":\"100000000\",\"ecBlockHeight\":15231,\"transaction\":\"9922129659947542574\"}";
    String ordinaryPaymentTx = "{\"signature\":\"f7764623c61ed27478917029b19f10d844748864e3090f6186fd92ca08d8aa04e9ed9930b2c9696843a0d4408efbc6be20e95b1b4eca98e3259020769f7de6d4\",\"transactionIndex\":-1,\"type\":0,\"phased\":false,\"ecBlockId\":\"7966702469739265029\",\"signatureHash\":\"e0e05add018f1534ac20a8062f530dad6f9eb8c0cdbeaca5ea32d0e4bbcff8db\",\"attachment\":{\"version.OrdinaryPayment\":0},\"senderRS\":\"APL-JSQ6-CSFM-9L2A-6DLT4\",\"subtype\":0,\"recipientRS\":\"APL-Q7NE-ZHZ5-S6MH-9AYCQ\",\"block\":\"0\",\"blockTimestamp\":-1,\"deadline\":1440,\"timestamp\":102626712,\"height\":334444,\"senderPublicKey\":\"481e8de7d185992ae304c61b447f62c4c8ff1abdaaf3bd2162f135cd33d40674\",\"requestProcessingTime\":0,\"confirmations\":0,\"fullHash\":\"e98e8e2090dd2c5c0705ebe19a755f14a6d9266d4b33053c2933615c0d7c86b4\",\"version\":1,\"amountATM\":\"12839118775\",\"referencedTransactionFullHash\":\"4ee8a4e184c4a597d4d099fb0235797a600c03b9aef5c4d058d8969e0c1adf08\",\"sender\":\"4712536893285753540\",\"recipient\":\"8875408475291326092\",\"feeATM\":\"400000000\",\"ecBlockHeight\":14730,\"transaction\":\"6641927161555881705\"}";

    @Mock
    PrunableLoadingService prunableLoadingService;
    @Mock
    BlockchainConfig config;

    TransactionTestData td = new TransactionTestData();
    TransactionJsonSerializerImpl jsonSerialier;
    TransactionDTOConverter toModelConverter;
    UnconfirmedTransactionDTO expectedPrivDTO;
    Transaction expectedPrivTx;
    UnconfirmedTransactionDTO expectedPubDTO;
    Transaction expectedPubTx;

    UnconfirmedTransactionConverter transactionConverter;
    @BeforeEach
    void setUp() throws JsonProcessingException {
        Convert2.init("APL", 0);
        doReturn(mock(Chain.class)).when(config).getChain();

        transactionConverter = new UnconfirmedTransactionConverter(prunableLoadingService);
        jsonSerialier = new TransactionJsonSerializerImpl(prunableLoadingService, config);
        toModelConverter = new TransactionDTOConverter(new TransactionBuilderFactory(td.getTransactionTypeFactory(), config));

        expectedPrivDTO = JSON.getMapper().readValue(anonymousPrivatePaymentTx, UnconfirmedTransactionDTO.class);
        expectedPrivTx = toModelConverter.convert(expectedPrivDTO);
        expectedPubDTO = JSON.getMapper().readValue(ordinaryPaymentTx, UnconfirmedTransactionDTO.class);
        expectedPubTx = toModelConverter.convert(expectedPubDTO);
    }

    @AfterEach
    void tearDown() {
        Convert2.init("APL", 0);
    }

    @SneakyThrows
    @Test
    void convert_private_tx() {
        transactionConverter.setPriv(false);

        UnconfirmedTransactionDTO actualTxDTO = transactionConverter.convert(expectedPrivTx);

        openDataEquals(expectedPrivDTO, actualTxDTO);
        privDataEquals(expectedPrivDTO, actualTxDTO);
        affectedDataNotEquals(expectedPrivDTO, actualTxDTO);
        Transaction actualTx = toModelConverter.convert(actualTxDTO);
        assertEquals(expectedPrivTx, actualTx); // priv data synchronized into byte array
        JSONObject actualJson = jsonSerialier.toJson(actualTx);
        JSONAssert.assertNotEquals(anonymousPrivatePaymentTx, actualJson.toJSONString(), true); // different fullhash/id
    }

    @SneakyThrows
    @Test
    void convertPrivatePaymentTx_with_priv() {
        transactionConverter.setPriv(true);

        UnconfirmedTransactionDTO actualTxDTO = transactionConverter.convert(expectedPrivTx);

        openDataEquals(expectedPrivDTO, actualTxDTO);
        privDataNotEquals(expectedPrivDTO, actualTxDTO);
        affectedDataNotEquals(expectedPrivDTO, actualTxDTO);

        Transaction actualTx = toModelConverter.convert(actualTxDTO);
        assertNotEquals(expectedPrivTx, actualTx); // different priv data
        JSONObject actualJson = jsonSerialier.toJson(actualTx);
        JSONAssert.assertNotEquals(anonymousPrivatePaymentTx, actualJson.toJSONString(), true); // different fullhash/id and priv data
    }

    @Test
    void convertOrdinaryPaymant_with_priv() {
        transactionConverter.setPriv(true);

        UnconfirmedTransactionDTO actualTxDTO = transactionConverter.convert(expectedPubTx);

        openDataEquals(expectedPubDTO, actualTxDTO);
        privDataEquals(expectedPubDTO, actualTxDTO);
        affectedDataEquals(expectedPubDTO, actualTxDTO);

        Transaction actualTx = toModelConverter.convert(actualTxDTO);
        assertEquals(expectedPubTx, actualTx);
    }

    void openDataEquals(UnconfirmedTransactionDTO expected, UnconfirmedTransactionDTO actual) {
        equals(expected.getType(), actual.getType());
            equals(expected.getSubtype(), actual.getSubtype()) ;
            equals(expected.getPhased(), actual.getPhased()) ;
            equals(expected.getTimestamp(), actual.getTimestamp()) ;
            equals(expected.getDeadline(), actual.getDeadline()) ;
            equals(expected.getFeeATM(), actual.getFeeATM()) ;
            equals(expected.getReferencedTransactionFullHash(), actual.getReferencedTransactionFullHash()) ;
            equals(expected.getSignature(), actual.getSignature()) ;
            equals(expected.getSignatureHash(), actual.getSignatureHash()) ;

            expected.getAttachment().forEach((k, v)-> {
                Object o = actual.getAttachment().get(k);
                assertNotNull(o);
                if (o instanceof Number && v instanceof Number) {
                    assertEquals(o.toString(), ((Number) v).toString());
                } else {
                    assertEquals(o, v);
                }

            });
//            equals(expected.getHeight(), actual.getHeight()) ;
            equals(expected.getVersion(), actual.getVersion()) ;
            equals(expected.getEcBlockId(), actual.getEcBlockId()) ;
            equals(expected.getEcBlockHeight(), actual.getEcBlockHeight());
    }

    void privDataEquals(UnconfirmedTransactionDTO expected, UnconfirmedTransactionDTO actual) {
            equals(expected.getSenderPublicKey(), actual.getSenderPublicKey()) ;
            equals(expected.getRecipient(), actual.getRecipient()) ;
            equals(expected.getRecipientRS(), actual.getRecipientRS()) ;
            equals(expected.getAmountATM(), actual.getAmountATM()) ;
            equals(expected.getSender(), actual.getSender()) ;
            equals(expected.getSenderRS(), actual.getSenderRS()) ;
    }
    void privDataNotEquals(UnconfirmedTransactionDTO expected, UnconfirmedTransactionDTO actual) {
        notEquals(expected.getSenderPublicKey(), actual.getSenderPublicKey()) ;
        notEquals(expected.getRecipient(), actual.getRecipient()) ;
        notEquals(expected.getRecipientRS(), actual.getRecipientRS()) ;
        notEquals(expected.getAmountATM(), actual.getAmountATM()) ;
        notEquals(expected.getSender(), actual.getSender()) ;
        notEquals(expected.getSenderRS(), actual.getSenderRS()) ;
    }

    void affectedDataNotEquals(UnconfirmedTransactionDTO expected, UnconfirmedTransactionDTO actual) {
        notEquals(expected.getFullHash(), actual.getFullHash()) ;
        notEquals(expected.getTransaction(), actual.getTransaction()) ;
    }
    void affectedDataEquals(UnconfirmedTransactionDTO expected, UnconfirmedTransactionDTO actual) {
        equals(expected.getFullHash(), actual.getFullHash()) ;
        equals(expected.getTransaction(), actual.getTransaction()) ;
    }


    void equals(Object o1, Object o2) {
        assertEquals(o1, o2);
    }
    void notEquals(Object o1, Object o2) {
        assertNotEquals(o1, o2);
    }
}