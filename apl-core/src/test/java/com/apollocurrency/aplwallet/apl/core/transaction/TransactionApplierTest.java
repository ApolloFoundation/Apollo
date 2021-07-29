/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionApplierTest {
    @Mock BlockchainConfig blockchainConfig;
    @Mock ReferencedTransactionDao referencedTransactionDao;
    @Mock AccountPublicKeyService accountPublicKeyService;
    @Mock PrunableLoadingService prunableService;
    @Mock AppendixApplierRegistry applierRegistry;
    @Mock Blockchain blockchain;
    @Mock AppendixApplier appendixApplier;

    @Mock Transaction tx;
    @Mock AbstractAttachment attachment;

    AccountService accountService;


    TransactionApplier applier;
    TransactionTestData td;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
        accountService = td.getAccountService();
        applier = new TransactionApplier(blockchainConfig, referencedTransactionDao, accountService, accountPublicKeyService, prunableService, applierRegistry, blockchain);
    }

    @Test
    void applyUnconfirmed_failedTx_OK() {
        Account sender = new Account(td.TRANSACTION_10.getSenderId(), 200_000_000, 100_000_000, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_10.getSenderId());

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_10);

        assertTrue(applied, "Failed transaction #10 with id " + td.TRANSACTION_10.getId() + " should be appliedUnconfirmed successfully");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.FAILED_VALIDATION_TRANSACTION_FEE, td.TRANSACTION_10.getId(), 0, -100_000_000L);
    }

    @Test
    void applyUnconfirmed_failedTx_doubleSpending() {
        Account sender = new Account(td.TRANSACTION_10.getSenderId(), 200_000_000, 99_999_999, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_10.getSenderId());

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_10);

        assertFalse(applied, "Failed transaction #10 with id " + td.TRANSACTION_10.getId() + " should NOT be appliedUnconfirmed successfully");
        verify(accountService, never()).addToUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

    @Test
    void applyUnconfirmed_notFailedTx_ok() {
        Account sender = new Account(td.TRANSACTION_2.getSenderId(), 200000000000000000L,200000000000000000L, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_2.getSenderId());

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_2);

        assertTrue(applied, "Transaction #2 with id " + td.TRANSACTION_2.getId() + " should be appliedUnconfirmed successfully");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.UPDATE_CRITICAL, td.TRANSACTION_2.getId(), -100_000_000_000_000_000L, -100_000_000L);
    }

    @Test
    void applyUnconfirmed_notFailedTx_doubleSpending_failedTxsAcceptanceDisabled() {
        Account sender = new Account(td.TRANSACTION_2.getSenderId(), 200000000000000000L,0L, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_2.getSenderId());
        doReturn(false).when(blockchainConfig).isFailedTransactionsAcceptanceActiveAtHeight(0);

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_2);

        assertFalse(applied, "Transaction #2 with id " + td.TRANSACTION_2.getId() + " should not be appliedUnconfirmed successfully, when failed txs acceptance is not enabled");
        verify(accountService).getAccount(td.TRANSACTION_2.getSenderId());
        verifyNoMoreInteractions(accountService);
    }


    @Test
    void applyUnconfirmed_notFailedTx_noAccount() {
        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_2);

        assertFalse(applied, "Transaction #2 with id " + td.TRANSACTION_2.getId() + " should not be appliedUnconfirmed successfully, when account is not found");
        verify(accountService).getAccount(td.TRANSACTION_2.getSenderId());
        verifyNoMoreInteractions(accountService);
    }

    @Test
    void applyUnconfirmed_notFailedTx_doubleSpending_failedTxsAcceptanceEnabled_OK() {
        Account sender = new Account(td.TRANSACTION_2.getSenderId(), 200000000000000000L,100_000_000L, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_2.getSenderId());
        doReturn(true).when(blockchainConfig).isFailedTransactionsAcceptanceActiveAtHeight(0);

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_2);

        assertTrue(applied, "Transaction #2 with id " + td.TRANSACTION_2.getId() + " should be appliedUnconfirmed successfully, when failed txs acceptance is enabled");
        assertEquals("Double spending", td.TRANSACTION_2.getErrorMessage()
            .orElseThrow(()-> new IllegalStateException("Previously not failed transaction should be failed after fallback to applyUncofirmedFailed")));
        verify(accountService, times(2)).getAccount(td.TRANSACTION_2.getSenderId());
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.FAILED_VALIDATION_TRANSACTION_FEE, td.TRANSACTION_2.getId(), 0, -100_000_000L);
    }

    @Test
    void applyUnconfirmed_notFailedTx_doubleSpending_failedTxsAcceptanceEnabled_notOK() {
        Account sender = new Account(td.TRANSACTION_2.getSenderId(), 200000000000000000L,10_000_000L, 0, 0, 0);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_2.getSenderId());
        doReturn(true).when(blockchainConfig).isFailedTransactionsAcceptanceActiveAtHeight(0);

        boolean applied = applier.applyUnconfirmed(td.TRANSACTION_2);

        assertFalse(applied, "Transaction #2 with id " + td.TRANSACTION_2.getId() + " should NOT be appliedUnconfirmed successfully");
        assertTrue(td.TRANSACTION_2.getErrorMessage().isEmpty(), "No error message should be present for #2 transaction when fallback applyUnconfirmedFailed finished with false");
        verify(accountService, times(2)).getAccount(td.TRANSACTION_2.getSenderId());
        verify(accountService, never()).addToUnconfirmedBalanceATM(any(Account.class), any(LedgerEvent.class), anyLong(), anyLong(), anyLong());
    }

    @Test
    void apply_notFailedPaymentWithRecipientAndReferencedHash_OK() {
        Account sender = new Account(td.TRANSACTION_4.getSenderId(), 200000000000000000L,10_000_000L, 0, 0, 0);
        Account recipient = new Account(td.TRANSACTION_4.getRecipientId(), 0L,0L, 0, 0, 0);
        doReturn(recipient).when(accountService).addAccount(td.TRANSACTION_4.getRecipientId(), false);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_4.getSenderId());
        doReturn(100L).when(blockchainConfig).getUnconfirmedPoolDepositAtm();

        applier.apply(td.TRANSACTION_4);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, td.TRANSACTION_4.getId(), 0, 100);
        verify(referencedTransactionDao).insert(new ReferencedTransaction(0L, td.TRANSACTION_4.getId(), Convert.transactionFullHashToId(td.TRANSACTION_4.referencedTransactionFullHash()), td.TRANSACTION_4.getHeight()));
        verify(prunableService).loadPrunable(td.TRANSACTION_4, td.TRANSACTION_4.getAttachment(), false);
        verify(accountService).addToBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, td.TRANSACTION_4.getId(), -td.TRANSACTION_4.getAmountATM(), -td.TRANSACTION_4.getFeeATM());
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(recipient, LedgerEvent.ORDINARY_PAYMENT, td.TRANSACTION_4.getId(), td.TRANSACTION_4.getAmountATM());
    }

    @Test
    void apply_failedDuringExecution_OK() {
        Account sender = new Account(1L, 200000000000000000L,10_000_000L, 0, 0, 0);
        doReturn(1L).when(tx).getSenderId();
        doReturn(10L).when(tx).getFeeATM();
        List<AbstractAppendix> appendages = new ArrayList<>();
        appendages.add(attachment);
        doReturn(List.of(attachment)).when(tx).getAppendages();
        doReturn(sender).when(accountService).getAccount(1L);
        doReturn(true).when(tx).canFailDuringExecution();
        doThrow(new AplTransactionExecutionException("Test tx execution error", td.TRANSACTION_4)).when(attachment).apply(tx, sender, null);
        doReturn(true).when(blockchainConfig).isFailedTransactionsAcceptanceActiveAtHeight(0);

        applier.apply(tx);

        verify(tx).fail("Test tx execution error");
        verify(blockchain).updateTransaction(tx);
        verify(accountService).addToBalanceAndUnconfirmedBalanceATM(sender, LedgerEvent.FAILED_EXECUTION_TRANSACTION_FEE, 0, 0, -10);
    }

    @Test
    void apply_notFailedPhasingPaymentWithRecipient_OK() {
        Account sender = new Account(td.TRANSACTION_13.getSenderId(), 200000000000000000L,10_000_000L, 0, 0, 0);
        Account recipient = new Account(td.TRANSACTION_13.getRecipientId(), 0L,0L, 0, 0, 0);
        doReturn(recipient).when(accountService).addAccount(td.TRANSACTION_13.getRecipientId(), false);
        doReturn(sender).when(accountService).getAccount(td.TRANSACTION_13.getSenderId());
        doReturn(appendixApplier).when(applierRegistry).getFor(any(Appendix.class));

        applier.apply(td.TRANSACTION_13);

        verify(prunableService, times(4)).loadPrunable(any(Transaction.class), any(Appendix.class), anyBoolean());
        verify(accountService).addToBalanceATM(sender, LedgerEvent.ORDINARY_PAYMENT, td.TRANSACTION_13.getId(), 0, -td.TRANSACTION_13.getFeeATM());
        verify(accountService).getAccount(td.TRANSACTION_13.getRecipientId());
        verify(accountService).getAccount(td.TRANSACTION_13.getSenderId());
        verify(appendixApplier, times(4)).apply(any(Transaction.class), any(Appendix.class), any(Account.class), any(Account.class));
        verifyNoMoreInteractions(accountService);
    }

    @Test
    void applyPhasingFailed() {
        Account sender = new Account(1L, 200000000000000000L,10_000_000L, 0, 0, 0);
        doReturn(10L).when(tx).getFeeATM();
        List<AbstractAppendix> appendages = new ArrayList<>();
        appendages.add(attachment);
        doReturn(appendages).when(tx).getAppendages();
        doReturn(sender).when(accountService).getAccount(1L);
        doReturn(true).when(tx).canFailDuringExecution();
        doReturn(1L).when(tx).getSenderId();
        doThrow(new AplTransactionExecutionException("Test phasing tx execution error", tx)).when(attachment).apply(tx, sender, null);
        doReturn(true).when(blockchainConfig).isFailedTransactionsAcceptanceActiveAtHeight(0);
        doReturn(true).when(attachment).isPhasable();

        applier.applyPhasing(tx);

        verify(tx).fail("Test phasing tx execution error");
        verify(blockchain).updateTransaction(tx);
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.FAILED_EXECUTION_TRANSACTION_FEE, 0, 0, -10);
    }

    @Test
    void applyPhasing() {
    }

    @Test
    void undoUnconfirmed() {
    }
}