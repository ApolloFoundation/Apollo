/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AddressScope;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
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

import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_ACCOUNT_ATTACHMENT;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_ID_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_ID_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_PUBLIC_KEY_1;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.CHILD_PUBLIC_KEY_2;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER_ID;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER_PUBLIC_KEY;
import static com.apollocurrency.aplwallet.apl.core.transaction.ChildAccountTestData.SENDER_SECRET_PHRASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
public class ChildAccountTest {

    public static final int ECBLOCK_HEIGHT = 100_000;
    public static final long ECBLOCK_ID = 121L;

    @Mock
    HeightConfig heightConfig;
    @Mock
    TimeService timeService;
    @Mock
    TransactionProcessor processor;
    @Mock
    PropertiesHolder propertiesHolder;
    @Mock
    ReferencedTransactionDao referencedTransactionDao;
    @Mock
    PhasingPollService phasingPollService;

    AccountControlPhasingService accountControlPhasingService=mock(AccountControlPhasingService.class);
    BlockchainConfig blockchainConfig=mock(BlockchainConfig.class);
    Blockchain blockchain = mock(Blockchain.class);
    AccountService accountService=mock(AccountService.class);
    AccountPublicKeyService accountPublicKeyService=mock(AccountPublicKeyService.class);
    FeeCalculator calculator=mock(FeeCalculator.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from()
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(accountService, AccountService.class, AccountServiceImpl.class))
        .addBeans(MockBean.of(accountPublicKeyService, AccountPublicKeyService.class, AccountPublicKeyServiceImpl.class))
        .build();

    TransactionVersionValidator txVersionValidator = new TransactionVersionValidator(blockchainConfig, blockchain);
    TransactionApplier txApplier = new TransactionApplier(blockchainConfig, referencedTransactionDao, accountService, accountPublicKeyService);
    TransactionValidator txValidator = new TransactionValidator(blockchainConfig, phasingPollService, blockchain, calculator, txVersionValidator, accountControlPhasingService, accountService);
    TransactionCreator txCreator = new TransactionCreator(txValidator, propertiesHolder, timeService, calculator, blockchain, processor);

    @BeforeEach
    void setUp() {
        CHILD_1.setParentId(0L);
        CHILD_1.setMultiSig(false);
        CHILD_2.setParentId(0L);
        CHILD_2.setMultiSig(false);

        EcBlockData ecBlockData = new EcBlockData(ECBLOCK_ID, ECBLOCK_HEIGHT);
        when(blockchain.getECBlock(300)).thenReturn(ecBlockData);
        when(accountService.getAccount(SENDER_ID)).thenReturn(SENDER);
        when(accountService.addOrGetAccount(CHILD_ID_1)).thenReturn(CHILD_1);
        when(accountService.addOrGetAccount(CHILD_ID_2)).thenReturn(CHILD_2);
        when(accountService.getAccount(CHILD_PUBLIC_KEY_1)).thenReturn(CHILD_1).thenReturn(null);
    }

    @Test
    void applyAttachment() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(SENDER)
            .deadlineValue("1440")
            .feeATM(0)
            .attachment(CHILD_ACCOUNT_ATTACHMENT)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(SENDER_SECRET_PHRASE))
            .broadcast(false)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);
        assertNotNull(tx);

        byte[] txBytes = tx.getBytes();
        byte[] txUnsignedBytes = tx.getUnsignedBytes();

        String txStr = Convert.toHexString(txBytes);
        String txUnsignedStr = Convert.toHexString(txUnsignedBytes);

        //WHEN
        txApplier.apply(tx);

        //THEN
        assertEquals(SENDER.getId(), CHILD_1.getParentId());
        assertEquals(SENDER.getId(), CHILD_2.getParentId());

        assertTrue(CHILD_1.isChild());
        assertTrue(CHILD_2.isChild());

        assertTrue(CHILD_1.isMultiSig());
        assertTrue(CHILD_2.isMultiSig());

        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), CHILD_1.getAddrScope());
        assertEquals(CHILD_ACCOUNT_ATTACHMENT.getAddressScope(), CHILD_2.getAddrScope());

        //Don't remove, sounds weird, but this snippet doesn't work in batch mode.
        /*ArgumentCaptor<Account> argument = ArgumentCaptor.forClass(Account.class);
        verify(accountService, times(2)).update(argument.capture(), eq(false));
        List<Long> args = argument.getAllValues().stream().map(Account::getId).collect(Collectors.toUnmodifiableList());
        assertTrue(args.contains(CHILD_ID_1));
        assertTrue(args.contains(CHILD_ID_2));*/

        verify(accountPublicKeyService).apply(CHILD_1, CHILD_PUBLIC_KEY_1);
        verify(accountPublicKeyService).apply(CHILD_2, CHILD_PUBLIC_KEY_2);
    }

    @Test
    void validateAttachment() throws AplException.ValidationException {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(SENDER)
            .deadlineValue("1440")
            .feeATM(0)
            .attachment(CHILD_ACCOUNT_ATTACHMENT)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(SENDER_SECRET_PHRASE))
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
    void validateAttachment_AmountGTZero() {
        //GIVEN
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(SENDER)
            .deadlineValue("1440")
            .feeATM(0)
            .amountATM(1000L)
            .attachment(CHILD_ACCOUNT_ATTACHMENT)
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(SENDER_SECRET_PHRASE))
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
            .senderAccount(SENDER)
            .publicKey(Convert.parseHexString(SENDER_PUBLIC_KEY))
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
            .senderAccount(SENDER)
            .publicKey(Convert.parseHexString(SENDER_PUBLIC_KEY))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(new ChildAccountAttachment(AddressScope.IN_FAMILY, 2, List.of(CHILD_PUBLIC_KEY_1, Convert.parseHexString(SENDER_PUBLIC_KEY))))
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
            .senderAccount(SENDER)
            .publicKey(Convert.parseHexString(SENDER_PUBLIC_KEY))
            .deadlineValue("1440")
            .feeATM(0L)
            .amountATM(0L)
            .attachment(CHILD_ACCOUNT_ATTACHMENT)
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