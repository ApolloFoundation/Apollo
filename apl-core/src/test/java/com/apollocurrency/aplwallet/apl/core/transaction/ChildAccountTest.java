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
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ChildAccountTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    @Mock
    TransactionValidator validator;
    Blockchain blockchain = mock(Blockchain.class);
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    HeightConfig heightConfig;
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    FeeCalculator calculator;
    @Mock
    ReferencedTransactionDao referencedTransactionDao;
    @Mock
    PhasingPollService phasingPollService;
    @Mock
    AccountControlPhasingService accountControlPhasingService;

    AccountService accountService=mock(AccountService.class);
    AccountPublicKeyService accountPublicKeyService=mock(AccountPublicKeyService.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from()
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(accountService, AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(accountPublicKeyService, AccountPublicKeyService.class, AccountPublicKeyServiceImpl.class))
        .build();

    private final String accountRS = "APL-XR8C-K97J-QDZC-3YXHE";
    private final String publicKey = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    final String secretPhrase = "here we go again";
    final Account sender = new Account(Convert.parseAccountId(accountRS), 1000 * Constants.ONE_APL, 100 * Constants.ONE_APL, 0L, 0L, 0);
    final long senderId = sender.getId();

    static String CHILD_SECRET_PHRASE_1 = "1234567890";
    static byte[] CHILD_PUBLIC_KEY_1 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_1);
    static long CHILD_ID_1 = AccountService.getId(CHILD_PUBLIC_KEY_1);

    static String CHILD_SECRET_PHRASE_2 = "0987654321";
    static byte[] CHILD_PUBLIC_KEY_2 = Crypto.getPublicKey(CHILD_SECRET_PHRASE_2);
    static long CHILD_ID_2 = AccountService.getId(CHILD_PUBLIC_KEY_2);

    static final Account child1 = new Account(CHILD_ID_1, 0L,0L, 0L, 0L, 0);
    static final Account child2 = new Account(CHILD_ID_2, 0L,0L, 0L, 0L, 0);

    ChildAccountAttachment attachment = new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2));

    TransactionCreator txCreator;
    TransactionApplier txApplier;
    TransactionValidator txValidator;

    @BeforeEach
    void setUp() {
        txCreator= new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor);
        txApplier = new TransactionApplier(blockchainConfig, referencedTransactionDao, accountService, accountPublicKeyService);
        txValidator = new TransactionValidator(blockchainConfig, phasingPollService, blockchain, calculator, accountControlPhasingService, accountService);

        child1.setParentId(0L);
        child1.setMultiSig(false);
        child2.setParentId(0L);
        child2.setMultiSig(false);

        EcBlockData ecBlockData = new EcBlockData(ECBLOCK_ID, ECBLOCK_HEIGHT);
        when(blockchain.getECBlock(300)).thenReturn(ecBlockData);
        when(accountService.getAccount(senderId)).thenReturn(sender);
        when(accountService.addOrGetAccount(CHILD_ID_1)).thenReturn(child1);
        when(accountService.addOrGetAccount(CHILD_ID_2)).thenReturn(child2);
        when(accountService.getAccount(CHILD_PUBLIC_KEY_1)).thenReturn(child1).thenReturn(null);
    }

    @Test
    void validateAttachment() throws AplException.ValidationException {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(0)
            .attachment(attachment)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);
        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        when(heightConfig.getMaxPayloadLength()).thenReturn(255*Constants.MIN_TRANSACTION_SIZE);

        //WHEN
        txValidator.validate(tx);

        verify(accountControlPhasingService).checkTransaction(tx);
    }

    @Test
    void applyAttachment() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(0)
            .attachment(attachment)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);
        assertNotNull(tx);

        //WHEN
        txApplier.apply(tx);

        //THEN
        assertEquals(sender.getId(), child1.getParentId());
        assertEquals(sender.getId(), child2.getParentId());

        assertTrue(child1.isChild());
        assertTrue(child2.isChild());

        assertTrue(child1.isMultiSig());
        assertTrue(child2.isMultiSig());

        assertEquals(attachment.getAddressScope(), child1.getAddrScope());
        assertEquals(attachment.getAddressScope(), child2.getAddrScope());

        verify(accountPublicKeyService).apply(child1, CHILD_PUBLIC_KEY_1);

        verify(accountPublicKeyService).apply(child2, CHILD_PUBLIC_KEY_2);
    }

    @Test
    void validateAttachment_AmountGTZero() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(0)
            .amountATM(1000L)
            .attachment(attachment)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("Transactions of this type must have recipient == 0, amount == 0"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withWrongChildAccountCount() throws AplException.ValidationException {
        //GIVEN
        int wrongChildCountValue = 1;// 2 is valid
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(new ChildAccountAttachment(AddressScope.IN_FAMILY, wrongChildCountValue, List.of(CHILD_PUBLIC_KEY_1, CHILD_PUBLIC_KEY_2)))
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);


        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("Wrong value of the child count, count=1"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withParentPublicKey() throws AplException.ValidationException {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, Convert.parseHexString(publicKey))))
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
            //THEN
            assertTrue(e.getMessage().contains("a child can't simultaneously be a parent"), "Unexpected exception message.");
        }
    }

    @Test
    void validateAttachment_withChildAlreadyExists() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(attachment)
            .timestamp(300)
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertNotNull(tx);
        when(blockchain.getHeight()).thenReturn(ECBLOCK_HEIGHT+1);
        when(blockchain.getBlockIdAtHeight(ECBLOCK_HEIGHT)).thenReturn(ECBLOCK_ID);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(Long.MAX_VALUE);

        //WHEN
        try {
            txValidator.validate(tx);
            fail("Unexpected flow.");
        } catch (AplException.ValidationException e) {
        //THEN
            assertTrue(e.getMessage().contains("Child account already exists"), "Unexpected exception message.");
        }
    }
}