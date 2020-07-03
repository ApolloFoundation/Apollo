/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ChildAccountTest {
    private static final int CURRENT_HEIGHT = 1000;
    private static final String PARENT_SECRET_PHRASE = "0000000000";
    private static final byte[] PARENT_PUBLIC_KEY = Crypto.getPublicKey(PARENT_SECRET_PHRASE);

    public static final String CHILD_SECRET_PHRASE_1 = "1234567890";
    private static final byte[] CHILD_PUBLIC_KEY_1 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_1);

    public static final String CHILD_SECRET_PHRASE_2 = "0987654321";
    private static final byte[] CHILD_PUBLIC_KEY_2 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_2);

    final ChildAccountAttachment ATTACHMENT_0 = new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2));

   /* final Transaction TRANSACTION_0 = TransactionTestData.buildTransaction(3444674909301056677L,
        BLOCK_0_HEIGHT, BLOCK_0_ID, BLOCK_0_TIMESTAMP,
        (short) 1440, null, (short) 0, 0L, 2500000000000L,
        "a524974f94f1cd2fcc6f17193477209ca5821d37d391e70ae668dd1c11dd798e",
        "375ef1c05ae59a27ef26336a59afe69014c68b9bf4364d5b1b2fa4ebe302020a868ad365f35f0ca8d3ebaddc469ecd3a7c49dec5e4d2fad41f6728977b7333cc",
        35073712,
        TransactionType.TYPE_CHILD_ACCOUNT, TransactionType.SUBTYPE_CHILD_CREATE,
        9211698109297098287L, null,
        null,
        false, (byte) 1, false, false, false,
        14399, -5416619518547901377L,
        false, false, false, false,
        Convert.toHexString(ATTACHMENT_0.dataBytes()));*/

    ChildAccountAttachment attachment;

    @Mock
    TransactionValidator validator;
    Blockchain blockchain = mock(Blockchain.class);
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    FeeCalculator calculator;

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from().addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class)).build();

    TransactionCreator txCreator;

    private String accountRS = "APL-XR8C-K97J-QDZC-3YXHE";
    Account sender = new Account(Convert.parseAccountId(accountRS), 1000 * Constants.ONE_APL, 100 * Constants.ONE_APL, 0L, 0L, 0);
    private String publicKey = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    private String secretPhrase = "here we go again";

    @BeforeEach
    void setUp() {
        txCreator = new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor);

            Account senderAccount = new Account(AccountService.getId(PARENT_PUBLIC_KEY), 0,0,0,0, BLOCK_1_HEIGHT);
            PublicKey publicKey = new PublicKey(AccountService.getId(PARENT_PUBLIC_KEY), PARENT_PUBLIC_KEY, BLOCK_1_HEIGHT);
            senderAccount.setPublicKey(publicKey);

            CreateTransactionRequest createTransactionRequest = buildRequest(PARENT_SECRET_PHRASE, senderAccount, ATTACHMENT_0, 0L);
            createTransactionRequest.setBroadcast(false);
    }

    @Test
    void testCreateTransactionSuccessful() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(300);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(Constants.ONE_APL)
            .attachment(ATTACHMENT_0)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        //assertSame(transactionType, tx.getType());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof ChildAccountAttachment);
        assertEquals(300, tx.getTimestamp());
        verify(processor).broadcast(tx);
    }

    @Test
    void validateAttachment() {
    }

    @Test
    void applyAttachment() {
    }

    private CreateTransactionRequest buildRequest(String passphrase, Account senderAccount, Attachment attachment, Long feeATM) {
        byte[] keySeed = Crypto.getKeySeed(passphrase);
        CreateTransactionRequest transferMoneyReq = CreateTransactionRequest
            .builder()
            .passphrase(passphrase)
            .deadlineValue("1440")
            .publicKey(Crypto.getPublicKey(keySeed))
            .senderAccount(senderAccount)
            .keySeed(keySeed)
            .broadcast(true)
            .recipientId(0L)
            .ecBlockHeight(0)
            .ecBlockId(0L)
            .build();

        if (attachment != null) {
            transferMoneyReq.setAttachment(attachment);
        }
        if (feeATM != null) {
            transferMoneyReq.setFeeATM(0);
        }

        return transferMoneyReq;
    }
}