package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionTypeTest {
    public static final long SENDER_ID = 1L;
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Transaction transaction;
    @Mock
    Account sender;
    @Mock
    Account recipient;
    @Mock
    HeightConfig heightConfig;

    @Test
    void isDuplicate_withTwoMaxPossible() {
        // GIVEN
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();
        // WHEN
        boolean duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.ACCOUNT_INFO, "1", duplicatesMap, 2);
        // THEN
        assertFalse(duplicate, "After first duplicate verification, key '1' for ACCOUNT_INFO tx type should not " +
            "be a duplicate (only third that type tx with a key '1' should be duplicate)");

        // WHEN
        duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.ACCOUNT_INFO, "1", duplicatesMap, 2);

        // THEN
        assertFalse(duplicate, "After second duplicate verification, key '1' for ACCOUNT_INFO tx type should not " +
            "be a duplicate (only third that type tx with a key '1' should be duplicate)");

        // WHEN
        duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.ACCOUNT_INFO, "1", duplicatesMap, 2);

        // THEN
        assertTrue(duplicate, "Tx of ACCOUNT_INFO type should be a duplicate after second tx for '1' key allowed");
    }


    @Test
    void isDuplicate_exclusive() {
        // GIVEN
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();
        // WHEN
        boolean duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, "2", duplicatesMap, true);
        // THEN
        assertFalse(duplicate, "After first duplicate verification, key '2' for ORDINARY_PAYMENT tx type should not " +
            "be a duplicate, only following txs of the same type and key will be duplicates");

        // WHEN
        duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, "2", duplicatesMap, true);

        // THEN
        assertTrue(duplicate, "After second duplicate verification, key '2' for ORDINARY_PAYMENT tx type " +
            "must be a duplicate, since the only one tx with a key '2' and type ORDINARY_PAYMENT allowed at a time");
    }

    @Test
    void isDuplicate_noLimit_withOuterExclusiveAccess() {
        // GIVEN
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();
        // WHEN
        boolean duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT, "3", duplicatesMap, false);
        // THEN
        assertFalse(duplicate, "Not exclusive first duplicate verification should not lead to a duplicate occurrence");

        // WHEN
        duplicate = TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT, "3", duplicatesMap, true);

        // THEN
        assertTrue(duplicate, "Tx of PRIVATE_PAYMENT type with a key '3', which require exclusive duplicate verification must be a duplicate, since" +
            "the other transaction with the same type and key was already passed the verification");
    }

    @SneakyThrows
    @Test
    void validateAtFinishStateDependent() {
        FallibleTransactionType type = prepareTransactionType();

        type.validateStateDependentAtFinish(transaction);

        assertEquals(1, type.getDependentValidationCounter());
    }

    @SneakyThrows
    @Test
    void validateStateIndependent() {
        FallibleTransactionType type = prepareTransactionType();

        type.validateStateIndependent(transaction);

        assertEquals(1, type.getIndependentValidationCounter());
    }

    @SneakyThrows
    @Test
    void validateStateDependent_OK() {
        mockTxAndSender();
        doReturn(110L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        type.validateStateDependent(transaction);

        assertEquals(1, type.getDependentValidationCounter());
    }

    @Test
    void validateStateDependent_NoAccount() {
        doReturn(SENDER_ID).when(transaction).getSenderId();
        FallibleTransactionType type = prepareTransactionType();

        assertThrows(AplUnacceptableTransactionValidationException.class, ()-> type.validateStateDependent(transaction));

        assertEquals(0, type.getDependentValidationCounter());
    }

    @Test
    void validateStateDependent_notEnoughFunds_enoughToPayFee() {
        mockTxAndSender();
        doReturn(109L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        AplAcceptableTransactionValidationException ex = assertThrows(AplAcceptableTransactionValidationException.class, () -> type.validateStateDependent(transaction));

        assertEquals(0, type.getDependentValidationCounter());
        assertEquals("Transaction 'transaction' failed with message: 'Not enough apl balance on account: 1 to " +
            "pay transaction both amount: 100 and fee: 10, only fee may be paid, balance: 109'", ex.toString());
    }

    @Test
    void validateStateDependent_notEnoughFunds_notEnoughToPayFee() {
        mockTxAndSender();
        doReturn(9L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        AplUnacceptableTransactionValidationException ex = assertThrows(AplUnacceptableTransactionValidationException.class, () -> type.validateStateDependent(transaction));

        assertEquals(0, type.getDependentValidationCounter());
        assertEquals("Transaction 'transaction' failed with message: 'Not enough apl balance on account: 1, required at least 110, but only got 9'", ex.toString());
    }

    @Test
    void apply_undoApply() {
        mockTxAmounts();
        doReturn(1L).when(sender).getId();
        doReturn(2L).when(recipient).getId();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100, -10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, 100);
        assertEquals(1, type.getApplicationCounter());

        type.undoApply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, 100, 10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, -100);
        assertEquals(0, type.getApplicationCounter());
    }

    @Test
    void apply_undoApply_phasing_noRecipient() {
        doReturn(true).when(transaction).attachmentIsPhased();
        doReturn(100L).when(transaction).getAmountATM();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, null);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100);
        assertEquals(1, type.getApplicationCounter());

        type.undoApply(transaction, sender, null);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, 100);
        assertEquals(0, type.getApplicationCounter());
    }

    @Test
    void apply_recipientAndSenderAreDifferent() {
        mockTxAmounts();
        doReturn(SENDER_ID).when(sender).getId();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100, -10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, 100);
        verify(recipient, never()).setBalanceATM(anyLong());
        assertEquals(1, type.getApplicationCounter());
    }

    @Test
    void apply_recipientAndSenderAreEquals() {
        mockTxAmounts();
        doReturn(200L).when(sender).getBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, recipient);

        verify(type.getAccountService()).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100, -10);
        verify(type.getAccountService()).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, 100);
        verify(recipient).setBalanceATM(200);
        assertEquals(1, type.getApplicationCounter());
    }

    @Test
    void applyUnconfirmedOK_withReferencedAdditionalUnconfirmedFee() {
        mockReferencedWithAmountTx();
        doReturn(170L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        boolean applied = type.applyUnconfirmed(transaction, sender);

        assertTrue(applied, "For sender's balance '170' and total tx amount with fee 160, " +
            "applyUnconfirmed operation should be successful");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, 0, -100, -60);
        assertEquals(1, type.getUnconfirmedCounter());
    }

    @Test
    void applyUnconfirmed_notEnoughFunds() {
        mockTxAmounts();
        doReturn(100L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        boolean applied = type.applyUnconfirmed(transaction, sender);

        assertFalse(applied, "For sender's balance '100' and total tx amount with fee 110, " +
            "applyUnconfirmed operation must be failed");
        verifyNoInteractions(accountService);
        assertEquals(0, type.getUnconfirmedCounter());
    }

    @Test
    void applyUnconfirmed_attachmentUnconfirmedFailed() {
        mockTxAmounts();
        doReturn(150L).when(sender).getUnconfirmedBalanceATM();
        BasicTestTransactionType type = new BasicTestTransactionType(blockchainConfig, accountService);

        boolean applied = type.applyUnconfirmed(transaction, sender);

        assertFalse(applied, "For sender's balance '150' and total tx amount with fee 110, " +
            "applyUnconfirmed operation must be failed, since the applyAttachmentUnconfirmed operation was not successful");
        verify(accountService).addToUnconfirmedBalanceATM(sender, null, 0L, -100, -10); // charge unconfirmed
        verify(accountService).addToUnconfirmedBalanceATM(sender, null, 0L, 100, 10); // refund unconfirmed
    }

    @Test
    void undoApply_sameSenderAndRecipientAccount() {
        mockTxAmounts();
        doReturn(210L).when(sender).getBalanceATM();
        doReturn(false).when(transaction).attachmentIsPhased();
        FallibleTransactionType type = prepareTransactionType();

        type.undoApply(transaction, sender, recipient);

        assertEquals(-1, type.getApplicationCounter());
        verify(accountService).addToBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, 0L, 100, 10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, LedgerEvent.ORDINARY_PAYMENT, 0L, -100);
        verify(recipient).setBalanceATM(210L); // update balance of the same account
    }

    @Test
    void undoUnconfirmed() {
        mockReferencedWithAmountTx();
        FallibleTransactionType type = prepareTransactionType();

        type.undoUnconfirmed(transaction, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, 0L, 100, 60L);
    }

    @Test
    void isDuplicate_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean duplicate = type.isDuplicate(transaction, Map.of());

        assertFalse(duplicate, "Default isDuplicate impl should return false");
    }

    @Test
    void isBlockDuplicate_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean duplicate = type.isBlockDuplicate(transaction, Map.of());

        assertFalse(duplicate, "Default isBlockDuplicate impl should return false");
    }

    @Test
    void isUnconfirmedDuplicate_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean duplicate = type.isUnconfirmedDuplicate(transaction, Map.of());

        assertFalse(duplicate, "Default isUnconfirmedDuplicate impl should return false");
    }

    @Test
    void isPruned_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean pruned = type.isPruned(0L);

        assertFalse(pruned, "Default isPruned impl should return false, since the implementation should treat " +
            "about pruning status of the transaction");
    }

    @Test
    void mustHaveRecipient_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean mustHaveRecipient = type.mustHaveRecipient();

        assertTrue(mustHaveRecipient, "Default mustHaveRecipient impl should return 'true', " +
            "when 'canHaveRecipient' return 'true'");
    }

    @Test
    void isPhasable_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();

        boolean phasable = type.isPhasable();

        assertTrue(phasable, "By default each transaction should be phasable");
    }

    @Test
    void getBaselineFee_defaultImpl() {
        FallibleTransactionType type = prepareTransactionType();
        mockFeeConfigs();

        Fee fee = type.getBaselineFee(transaction);

        // ten whole units multiplied by the 100 fractions in each
        assertEquals(1000L, fee.getFee(transaction, mock(Appendix.class)));
    }

    @Test
    // same impl as for getBaselineFee
    void getNextFee_defaultImpl() {
        mockFeeConfigs();
        FallibleTransactionType type = prepareTransactionType();

        Fee nextFee = type.getNextFee(transaction);

        assertEquals(1000L, nextFee.getFee(transaction, mock(Appendix.class)));
    }

    @Test
    void getBaselineFeeHeight() {
        FallibleTransactionType type = prepareTransactionType();

        assertEquals(1, type.getBaselineFeeHeight(), "Fee applied by the baseline height should be started from the blockchain beginning");
    }

    @Test
    void getNextFeeHeight() {
        FallibleTransactionType type = prepareTransactionType();

        assertEquals(Integer.MAX_VALUE, type.getNextFeeHeight(), "Next fee height should never be reached");
    }

    @Test
    void getBackFees() {
        FallibleTransactionType type = prepareTransactionType();

        assertArrayEquals(new long[0], type.getBackFees(transaction), "By default back fee should be of length 0, " +
            "intended to not apply any back fees for the tx type");
    }

    @Test
    void typeToString() {
        assertEquals("TestPayment ORDINARY_PAYMENT", prepareTransactionType().toString());
    }

    @Test
    void undoApplyAttachment_NotAllowed() {
        BasicTestTransactionType type = new BasicTestTransactionType(blockchainConfig, accountService);

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
            () -> type.undoApply(transaction, sender, null));

        assertEquals("undoApplyAttachment is not supported for transaction type: null, transaction: null, sender: 0", ex.getMessage());
    }

    @Test
    void createSizeBasedFee_customUnitSize() {
        FallibleTransactionType type = prepareTransactionType();
        Appendix attachment = mock(Appendix.class);
        mockFeeConfigs();
        doReturn(new BigDecimal("0.33")).when(heightConfig).getSizeBasedFee(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, new BigDecimal("0.33"));
        doReturn(100).when(attachment).getFullSize();

        Fee sizeBasedFee = type.getFeeFactory().createSizeBased(BigDecimal.ONE, new BigDecimal("0.33"), (transaction, appendix) -> appendix.getFullSize(), 33);

        assertEquals(sizeBasedFee.getFee(transaction, attachment), 1099); // TEN fixed units with 100 fractions in each + 3 size based units 0.99 of the whole unit
    }

    @Test
    void createSizeBasedFee_defaultUnitSize() {
        FallibleTransactionType type = prepareTransactionType();
        mockFeeConfigs();
        doReturn(new BigDecimal("0.2")).when(heightConfig).getSizeBasedFee(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, new BigDecimal("0.33"));

        Fee sizeBasedFee = type.getFeeFactory().createSizeBased(BigDecimal.ONE, new BigDecimal("0.33"), (transaction, appendix) -> 96);

        assertEquals(sizeBasedFee.getFee(transaction, mock(Appendix.class)), 1040); // TEN fixed units with 100 fractions in each + 2 size based units 0.4 of the whole unit
    }

    @Test
    void createCustomFee() {
        FallibleTransactionType type = prepareTransactionType();

        Fee customFee = type.getFeeFactory().createCustom((transaction, appendix) -> 100L);

        long fee = customFee.getFee(transaction, mock(Appendix.class));

        assertEquals(100, fee);
    }


    private void mockReferencedWithAmountTx() {
        mockTxAmounts();
        mockReferencedTx();
    }

    private void mockTxAndSender() {
        mockTxAmounts();
        doReturn(SENDER_ID).when(transaction).getSenderId();
        doReturn(sender).when(accountService).getAccount(SENDER_ID);
    }

    private void mockTxAmounts() {
        doReturn(100L).when(transaction).getAmountATM();
        doReturn(10L).when(transaction).getFeeATM();
    }

    private void mockReferencedTx() {
        doReturn(new byte[32])
            .when(transaction).referencedTransactionFullHash();
        doReturn(50L).when(blockchainConfig).getUnconfirmedPoolDepositAtm();
    }

    private void mockFeeConfigs() {
        doReturn(100L).when(blockchainConfig).getOneAPL();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(BigDecimal.TEN).when(heightConfig).getBaseFee(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, BigDecimal.ONE);
    }


    private FallibleTransactionType prepareTransactionType() {
        return new FallibleTransactionType(blockchainConfig, accountService);
    }


    @EqualsAndHashCode(callSuper = false)
    @Getter
    private class FallibleTransactionType extends TransactionType {
        long unconfirmedCounter = 0;
        long applicationCounter = 0;
        long independentValidationCounter = 0;
        long dependentValidationCounter = 0;

        public FallibleTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
            super(blockchainConfig, accountService);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getSpec() {
            return TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.ORDINARY_PAYMENT;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return new EmptyAttachment() {
                @Override
                public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
                    return null;
                }
            };
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return new EmptyAttachment() {
                @Override
                public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
                    return null;
                }
            };
        }

        @Override
        public boolean canFailDuringExecution() {
            return true;
        }

        @Override
        protected void undoApplyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            super.undoApplyAttachment(transaction, senderAccount, recipientAccount); // additional verification
            applicationCounter--;
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            unconfirmedCounter++;
            return true;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            applicationCounter++;
        }

        @Override
        public @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE) void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            unconfirmedCounter--;
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }

        @Override
        public String getName() {
            return "TestPayment";
        }

        @Override
        protected void doStateIndependentValidation(Transaction transaction) {
            independentValidationCounter++;
        }

        @Override
        protected void doStateDependentValidation(Transaction transaction) {
            dependentValidationCounter++;
        }
    }

    private class BasicTestTransactionType extends TransactionType {

        public BasicTestTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
            super(blockchainConfig, accountService);
        }

        @Override
        public TransactionTypes.TransactionTypeSpec getSpec() {
            return null;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(ByteBuffer buffer) {
            return null;
        }

        @Override
        public AbstractAttachment parseAttachment(JSONObject attachmentData) {
            return null;
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return false;
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

        }

        @Override
        public @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE) void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return false;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        protected void doStateIndependentValidation(Transaction transaction) {

        }

        @Override
        protected void doStateDependentValidation(Transaction transaction) {

        }
    }
}