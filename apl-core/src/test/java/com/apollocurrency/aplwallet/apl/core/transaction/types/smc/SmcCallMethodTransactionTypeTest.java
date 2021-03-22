/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;


import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.DocumentSigner;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.SmcCallMethodAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigInteger;
import java.util.UUID;

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
        " \"amount\":\"1\"," +
        " \"contractSource\":\"purchase\"," +
        " \"fuelPrice\":\"2\",\"fuelLimit\":\"10\"," +
        " \"params\":[\"123\",\"0x0A0B0C0D0E0F\"]" +
        "}";

    TransactionTestData td = new TransactionTestData();

    @Mock
    Chain chain;
    @Mock
    private BlockchainConfig blockchainConfig;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionValidator transactionValidator;

    private SmcCallMethodTransactionType smcCallMethodTransactionType = new SmcCallMethodTransactionType(blockchainConfig, accountService);

    @BeforeEach
    void setUp() {
        initMocks(this);
        GenesisImporter.CREATOR_ID = 1739068987193023818L;//TN1
        doReturn(UUID.fromString(CHAIN_ID_TN1)).when(chain).getChainId();
        doReturn(chain).when(blockchainConfig).getChain();

    }

    @SneakyThrows
    @Test
    void parseAttachment() {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(SMC_CALL_METHOD_ATTACHMENT_JSON);
        SmcCallMethodAttachment attachment = (SmcCallMethodAttachment) smcCallMethodTransactionType.parseAttachment(jsonObject);
        TransactionBuilderFactory transactionBuilderFactory = new TransactionBuilderFactory(td.getTransactionTypeFactory(), blockchainConfig);
        byte[] senderPublicKey = Convert.parseHexString("39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152");

        Transaction.Builder builder = transactionBuilderFactory.newUnsignedTransactionBuilder(
            blockchainConfig.getChain().getChainId().toString()
            , smcCallMethodTransactionType
            , (byte) 3
            , senderPublicKey
            , BigInteger.ONE
            , BigInteger.TEN
            , BigInteger.TEN
            , BigInteger.TWO
            , 1440
            , System.currentTimeMillis()
            , attachment
        );
        builder.ecBlockId(4407210215527895706L);
        builder.ecBlockHeight(552605);

        TransactionImpl tx = (TransactionImpl) builder.build();


        String secretPhrase = "topSecret";
        Credential signCredential = SignatureToolFactory.createCredential(tx.getVersion(), Crypto.getKeySeed(secretPhrase));
        DocumentSigner documentSigner = SignatureToolFactory.selectSigner(tx.getVersion()).get();
        TxBContext txBContext = TxBContext.newInstance(blockchainConfig.getChain());

        PayloadResult unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(tx.getVersion())
            .serialize(
                TransactionWrapperHelper.createUnsignedTransaction(tx)
                , unsignedTxBytes
            );
        byte[] document = unsignedTxBytes.array();

        //WHEN
        Signature signature = documentSigner.sign(document, signCredential);
        builder.signature(signature);
        TransactionImpl tx2 = (TransactionImpl) builder.build();

        //THEN
        assertNotNull(signature);
        assertNotNull(tx);
        assertNotNull(tx2);
    }
}