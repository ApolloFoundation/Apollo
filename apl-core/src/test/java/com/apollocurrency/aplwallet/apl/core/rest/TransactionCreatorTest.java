package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@EnableWeld
class TransactionCreatorTest {
    private static CustomTransactionType transactionType = new CustomTransactionType();
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
    TransactionCreator txCreator;
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from().addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class)).build();
    private String accountRS = "APL-XR8C-K97J-QDZC-3YXHE";
    Account sender = new Account(Convert.parseAccountId(accountRS), 1000 * Constants.ONE_APL, 100 * Constants.ONE_APL, 0L, 0L, 0);
    private String publicKey = "d52a07dc6fdf9f5c6b547ccb11444ce7bba73a99014eb9ac647b6971bee9263c";
    private String secretPhrase = "here we go again";

    @BeforeEach
    void setUp() {
        txCreator = new TransactionCreator(validator, propertiesHolder, timeService, calculator, blockchain, processor);
    }

    @Test
    void testCreateTransactionSuccessful() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(300);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(Constants.ONE_APL)
            .attachment(new CustomAttachment())
            .timestamp(300)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .build();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(300, tx.getTimestamp());
        verify(processor).broadcast(tx);
    }

    @Test
    void testCreateTransaction_setEcBlock() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(Constants.ONE_APL)
            .attachment(new CustomAttachment())
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .ecBlockId(1)
            .ecBlockHeight(100)
            .build();
        doReturn(1L).when(blockchain).getBlockIdAtHeight(100);
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(1, tx.getECBlockId());

        verify(processor).broadcast(tx);
    }

    @Test
    void testCreateTransaction_setEcBlock_usingHeight() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .feeATM(Constants.ONE_APL)
            .attachment(new CustomAttachment())
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .broadcast(true)
            .ecBlockHeight(100)
            .build();
        doReturn(2L).when(blockchain).getBlockIdAtHeight(100);
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);
        assertEquals(2, tx.getECBlockId());

        verify(processor).broadcast(tx);
    }

    @Test
    void createTransaction_missingPassphrase() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .deadlineValue("1440")
            .attachment(new CustomAttachment())
            .feeATM(Constants.ONE_APL)
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.MISSING_SECRET_PHRASE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1440", "0", "100.0", "5f"})
    void createTransaction_incorrectDeadline(String deadline) throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .deadlineValue(deadline)
            .attachment(new CustomAttachment())
            .feeATM(Constants.ONE_APL)
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.INCORRECT_DEADLINE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {100_000_000L * 100, Long.MAX_VALUE})
    void createTransaction_notEnoughFunds(long amount) throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .amountATM(amount)
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1)
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.NOT_ENOUGH_APL, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }

    @Test
    void createTransaction_missingDeadline() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .attachment(new CustomAttachment())
            .feeATM(Constants.ONE_APL)
            .build();
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.MISSING_DEADLINE, data.getErrorType());
        verify(processor, never()).broadcast(any(Transaction.class));
    }


    @Test
    void createTransaction_correctFee() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(secretPhrase))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(0)
            .broadcast(true)
            .build();
        doReturn(200_000_000L).when(calculator).getMinimumFeeATM(any(Transaction.class), anyInt());

        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertEquals(200_000_000, tx.getFeeATM());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);

        verify(processor).broadcast(tx);
    }

    @Test
    void createTransaction_correctFee_when_correction_enabled() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .publicKey(Convert.parseHexString(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1)
            .validate(true)
            .build();
        doReturn(200_000_000L).when(calculator).getMinimumFeeATM(any(Transaction.class), anyInt());
        doReturn(true).when(propertiesHolder).correctInvalidFees();
        Transaction tx = txCreator.createTransactionThrowingException(request);

        assertSame(transactionType, tx.getType());
        assertEquals(200_000_000, tx.getFeeATM());
        assertNull(tx.getBlock());
        assertTrue(tx.getAttachment() instanceof EmptyAttachment);

        verify(processor, never()).broadcast(tx);
        verify(validator).validate(tx);
    }

    @Test
    void testCreateTransaction_not_valid_on_unconfirmed() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doThrow(new AplException.InsufficientBalanceException("Test. Not enough funds")).when(processor).broadcast(any(Transaction.class));
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.INSUFFICIENT_BALANCE_ON_APPLY_UNCONFIRMED, data.getErrorType());
    }

    @Test
    void testCreateTransaction_featureNotEnabled() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doThrow(new AplException.NotYetEnabledException("Test. Not enabled")).when(processor).broadcast(any(Transaction.class));
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.FEATURE_NOT_AVAILABLE, data.getErrorType());
    }

    @Test
    void testCreateTransaction_general_validation_failed() throws AplException.ValidationException {
        EcBlockData ecBlockData = new EcBlockData(121, 100_000);
        doReturn(ecBlockData).when(blockchain).getECBlock(0);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doThrow(new AplException.NotValidException("Test. Not valid")).when(processor).broadcast(any(Transaction.class));
        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.VALIDATION_FAILED, data.getErrorType());
        assertEquals("Test. Not valid", data.getError());
    }

    @Test
    void testCreateTransaction_ecBlock_notValid() throws AplException.ValidationException {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
            .senderAccount(sender)
            .keySeed(Crypto.getKeySeed(publicKey))
            .deadlineValue("1")
            .ecBlockHeight(100)
            .ecBlockId(2)
            .attachment(new CustomAttachment())
            .feeATM(1000000)
            .broadcast(true)
            .build();
        doReturn(3L).when(blockchain).getBlockIdAtHeight(100);

        assertThrows(RestParameterException.class, () -> txCreator.createTransactionThrowingException(request));

        TransactionCreator.TransactionCreationData data = txCreator.createTransaction(request);
        assertEquals(TransactionCreator.TransactionCreationData.ErrorType.INCORRECT_EC_BLOCK, data.getErrorType());

        verify(processor, never()).broadcast(any(Transaction.class));
        verify(validator, never()).validate(any(Transaction.class));
    }


    private static class CustomAttachment extends EmptyAttachment {

        @Override
        public TransactionType getTransactionType() {
            return transactionType;
        }
    }

    private static class CustomTransactionType extends TransactionType {

        @Override
        public byte getType() {
            return 10;
        }

        @Override
        public byte getSubtype() {
            return 1;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.PRIVATE_PAYMENT;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return null;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {

        }

        @Override
        public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }

        @Override
        public String getName() {
            return "CustomTestType";
        }
    }
}