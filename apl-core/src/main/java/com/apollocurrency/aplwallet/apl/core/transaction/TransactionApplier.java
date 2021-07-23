package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionFailureNotSupportedException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class TransactionApplier {

    private final BlockchainConfig blockchainConfig;
    private final ReferencedTransactionDao referencedTransactionDao;
    private final AccountService accountService;
    private final AccountPublicKeyService accountPublicKeyService;
    private final PrunableLoadingService prunableService;
    private final AppendixApplierRegistry applierRegistry;
    private final Blockchain blockchain;


    @Inject
    public TransactionApplier(BlockchainConfig blockchainConfig, ReferencedTransactionDao referencedTransactionDao, AccountService accountService, AccountPublicKeyService accountPublicKeyService, PrunableLoadingService prunableService, AppendixApplierRegistry applierRegistry, Blockchain blockchain) {
        this.blockchainConfig = blockchainConfig;
        this.referencedTransactionDao = referencedTransactionDao;
        this.accountService = accountService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.prunableService = prunableService;
        this.applierRegistry = applierRegistry;
        this.blockchain = blockchain;
    }

    @TransactionFee(FeeMarker.UNCONFIRMED_BALANCE)
    // returns false if double spending
    public boolean applyUnconfirmed(Transaction transaction) {
        if (transaction.isFailed()) {
            return applyUnconfirmedFailed(transaction);
        }
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        boolean applied = senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
        if (!applied) { // try to keep transaction as failed
            if (!blockchainConfig.isFailedTransactionsAcceptanceActiveAtHeight(blockchain.getHeight())) {
                return false;
            }
            log.warn("Transaction {} didn't pass applyUnconfirmed validation, will treat transaction as failed at height {}", transaction.getStringId(), blockchain.getHeight());
            applied = applyUnconfirmedFailed(transaction);
            if (!applied) {
                return false;
            }
            transaction.fail("Double spending");
        }
        return true;
    }

    @TransactionFee({FeeMarker.BALANCE, FeeMarker.UNCONFIRMED_BALANCE})
    public void apply(Transaction transaction) {
        if (transaction.isFailed()) {
            applyFailed(transaction);
            return;
        }
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        accountPublicKeyService.apply(senderAccount, transaction.getSenderPublicKey());
        Account recipientAccount = null;
        if (transaction.getRecipientId() != 0) {
            recipientAccount = accountService.getAccount(transaction.getRecipientId());
            if (recipientAccount == null) {
                recipientAccount = accountService.addAccount(transaction.getRecipientId(), false);
            }
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            accountService.addToUnconfirmedBalanceATM(senderAccount, transaction.getType().getLedgerEvent(), transaction.getId(),
                0, blockchainConfig.getUnconfirmedPoolDepositAtm());

            referencedTransactionDao.insert(new ReferencedTransaction((long) 0, transaction.getId(), Convert.transactionFullHashToId(transaction.referencedTransactionFullHash()), transaction.getHeight()));
        }
        if (transaction.attachmentIsPhased()) {
            accountService.addToBalanceATM(senderAccount, transaction.getType().getLedgerEvent(), transaction.getId(), 0, -transaction.getFeeATM());
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            if (!appendage.isPhased(transaction)) {
                prunableService.loadPrunable(transaction, appendage, false);
                executeAppendage(transaction, senderAccount, recipientAccount, appendage);
            }
        }
    }

    public void applyPhasing(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        Account recipientAccount = transaction.getRecipientId() == 0 ? null : accountService.getAccount(transaction.getRecipientId());
        transaction.getAppendages().forEach(appendage -> {
            if (appendage.isPhasable()) {
                executeAppendage(transaction, senderAccount, recipientAccount, appendage);
            }
        });
    }

    public void undoUnconfirmed(Transaction transaction) {
        if (transaction.isFailed()) {
            undoUnconfirmedFailed(transaction);
            return;
        }
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        transaction.getType().undoUnconfirmed(transaction, senderAccount);
    }

    private void executeAppendage(Transaction transaction, Account senderAccount, Account recipientAccount, AbstractAppendix appendage) {
        if (appendage instanceof Attachment) {
            try {
                applyAppendage(transaction, senderAccount, recipientAccount, appendage);
            } catch (AplTransactionExecutionException e) {
                checkFailedTxsAcceptance(transaction, e);
                if (transaction.canFailDuringExecution()) {
                    appendage.undo(transaction, senderAccount, recipientAccount);
                    log.info("Transaction {} failed during execution: {}", transaction.getStringId(), e.getMessage());
                    transaction.fail(e.getMessage());
                } else {
                    throw new AplTransactionExecutionFailureNotSupportedException(e, transaction);
                }
            }
        } else {
            applyAppendage(transaction, senderAccount, recipientAccount, appendage);
        }
    }

    private void checkFailedTxsAcceptance(Transaction transaction, AplTransactionExecutionException e) {
        if (!blockchainConfig.isFailedTransactionsAcceptanceActiveAtHeight(transaction.getHeight())) {
            throw new AplTransactionFeatureNotEnabledException("Acceptance of the failed transactions", e, transaction);
        }
    }

    private void applyAppendage(Transaction transaction, Account senderAccount, Account recipientAccount, AbstractAppendix appendage) {
        AppendixApplier<AbstractAppendix> applier = applierRegistry.getFor(appendage);
        if (applier == null) {
            appendage.apply(transaction, senderAccount, recipientAccount);
        } else {
            applier.apply(transaction, appendage, senderAccount, recipientAccount);
        }
    }

    @TransactionFee({FeeMarker.BALANCE, FeeMarker.FEE})
    private void applyFailed(Transaction transaction) {
        long feeATM = transaction.getFeeATM();
        Account sender = accountService.getAccount(transaction.getSenderId());
        accountService.addToBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, transaction.getId(), 0, -feeATM);
    }

    @TransactionFee({FeeMarker.UNCONFIRMED_BALANCE, FeeMarker.FEE})
    private boolean applyUnconfirmedFailed(Transaction tx) {
        Account sender = accountService.getAccount(tx.getSenderId());
        if (sender.getUnconfirmedBalanceATM() >= tx.getFeeATM()) {
            accountService.addToUnconfirmedBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, tx.getId(), 0, -tx.getFeeATM());
            return true;
        } else {
            return false;
        }
    }

    @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE)
    private void undoUnconfirmedFailed(Transaction tx) {
        Account sender = accountService.getAccount(tx.getSenderId());
        accountService.addToUnconfirmedBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, tx.getId(), 0, tx.getFeeATM());
    }
}
