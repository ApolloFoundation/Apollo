/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureVerifier;
import com.apollocurrency.aplwallet.apl.core.transaction.types.child.CreateChildTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.OrdinaryPaymentTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TransactionSignatureTest {

    String StrtxJsonString = "{\n" +
        "\"senderPublicKey\":\"7364efd4df79887bd79771e00aabf711d5ce3208520a93f04c25d9a695e63a06\",\n" +
        "\"signature\":\"ec159f1a6900248e431bf5eb0f33aced48c94a37cd1af4632cc123e167297d03cf2c6273426b54f3738a20f3a35efd3554c4b210e46aaa42296fd085c390b969\",\n" +
        "\"type\":0,\"version\":1,\n" +
        "\"amountATM\":4000000000,\n" +
        "\"ecBlockId\":\"18338875302302929178\",\n" +
        "\"attachment\":{\"version.OrdinaryPayment\":0},\n" +
        "\"subtype\":0,\n" +
        "\"recipient\":\"7176168619783413675\",\n" +
        "\"feeATM\":400000000,\n" +
        "\"ecBlockHeight\":0,\n" +
        "\"id\":\"12814669673005965607\",\n" +
        "\"deadline\":1440,\n" +
        "\"timestamp\":78881629\n" +
        "}";

    JSONObject txJsonObject = (JSONObject) new JSONParser().parse(StrtxJsonString);

    Transaction transaction;
    SignatureVerifier signatureVerifier;
    Credential credential;

    TransactionSignatureTest() throws ParseException {
    }

    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AccountService accountService;
    @Mock
    AccountPublicKeyService accountPublicKeyService;


    @SneakyThrows
    @BeforeEach
    void setUp() {
        CreateChildTransactionType createChildTransactionType = new CreateChildTransactionType(blockchainConfig, accountService, accountPublicKeyService);
        OrdinaryPaymentTransactionType paymentTransactionType = new OrdinaryPaymentTransactionType(blockchainConfig, accountService);
        TransactionBuilder builder = new TransactionBuilder(new CachedTransactionTypeFactory(List.of(createChildTransactionType, paymentTransactionType)));
        transaction = builder.newTransactionBuilder(txJsonObject).build();
        signatureVerifier = SignatureToolFactory.selectValidator(1).get();
        credential = SignatureToolFactory.createCredential(1, transaction.getSenderPublicKey());
    }

    @Test
    void verifySignature() throws AplException.NotValidException {
        //GIVEN
        Signature signature = transaction.getSignature();
        String sigJsonStr = signature.getJsonString();
        String sigStr = Convert.toHexString(signature.bytes());

        //WHEN

        boolean rc = signatureVerifier.verify(transaction.getUnsignedBytes(), signature, credential);

        //THEN
        assertNotNull(signature);
        assertTrue(rc);

    }

}