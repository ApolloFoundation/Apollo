package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
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

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionTypesTest {
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

    @Test
    void findTransactionType() {
        assertThrows(IllegalArgumentException.class, () -> TransactionTypes.find((byte) -1, (byte) -1));

        TransactionTypes.TransactionTypeSpec ordinaryPayment = TransactionTypes.find((byte) 0, (byte) 0);
        assertNotNull(ordinaryPayment);
        assertEquals(TransactionTypes.TransactionTypeSpec.ORDINARY_PAYMENT, ordinaryPayment);
    }

    @Test
    void apply_undoApply() {
        createTxAmountsMocks();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100, -10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, 100);
        verify(recipient).setBalanceATM(0);
        assertEquals(1, type.getApplicationCounter());

        type.undoApply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, 100, 10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, -100);
        verify(recipient, times(2)).setBalanceATM(0);
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
        createTxAmountsMocks();
        doReturn(1L).when(sender).getId();
        FallibleTransactionType type = prepareTransactionType();

        type.apply(transaction, sender, recipient);

        verify(accountService).addToBalanceATM(sender, type.getLedgerEvent(), 0, -100, -10);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, type.getLedgerEvent(), 0, 100);
        verify(recipient, never()).setBalanceATM(anyLong());
        assertEquals(1, type.getApplicationCounter());
    }

    @SneakyThrows
    @Test
    void validateAtFinishStateDependent() {
        FallibleTransactionType type = prepareTransactionType();

        type.validateStateDependentAtFinish(transaction);

        assertEquals(1, type.getDependentValidationCounter());
    }

    @Test
    void undoApply_default() {
        BasicTestTransactionType type = new BasicTestTransactionType(blockchainConfig, accountService);

        assertThrows(UnsupportedOperationException.class, () -> type.undoApply(transaction, sender, recipient));
    }

    @SneakyThrows
    @Test
    void validateStateDependent_OK() {
        createTransctionAndSenderMocks();
        doReturn(110L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        type.validateStateDependent(transaction);

        assertEquals(1, type.getDependentValidationCounter());
    }

    @Test
    void validateStateDependent_NoAccount() {
        doReturn(1L).when(transaction).getSenderId();
        FallibleTransactionType type = prepareTransactionType();

        assertThrows(AplUnacceptableTransactionValidationException.class, ()-> type.validateStateDependent(transaction));

        assertEquals(0, type.getDependentValidationCounter());
    }

    @Test
    void validateStateDependent_notEnoughFunds() {
        createTransctionAndSenderMocks();
        doReturn(109L).when(sender).getUnconfirmedBalanceATM();
        FallibleTransactionType type = prepareTransactionType();

        assertThrows(AplAcceptableTransactionValidationException.class, () -> type.validateStateDependent(transaction));

        assertEquals(0, type.getDependentValidationCounter());
    }

    @Test
    void isDuplicate() {
        FallibleTransactionType type = prepareTransactionType();

//        type.is
    }


    private void createTransctionAndSenderMocks() {
        createTxAmountsMocks();
        doReturn(1L).when(transaction).getSenderId();
        doReturn(sender).when(accountService).getAccount(1L);
    }

    private void createTxAmountsMocks() {
        doReturn(100L).when(transaction).getAmountATM();
        doReturn(10L).when(transaction).getFeeATM();
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