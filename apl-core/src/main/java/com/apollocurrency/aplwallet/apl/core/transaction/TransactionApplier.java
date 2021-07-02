package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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


    @Inject
    public TransactionApplier(BlockchainConfig blockchainConfig, ReferencedTransactionDao referencedTransactionDao, AccountService accountService, AccountPublicKeyService accountPublicKeyService, PrunableLoadingService prunableService, AppendixApplierRegistry applierRegistry) {
        this.blockchainConfig = blockchainConfig;
        this.referencedTransactionDao = referencedTransactionDao;
        this.accountService = accountService;
        this.accountPublicKeyService = accountPublicKeyService;
        this.prunableService = prunableService;
        this.applierRegistry = applierRegistry;
    }

    // returns false if double spending
    public boolean applyUnconfirmed(Transaction transaction) {
        if (transaction.isFailed()) {
            return applyUnconfirmedFailed(transaction);
        }
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
    }

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
                if (transaction.canFailDuringExecution()) {
                    log.info("Transaction {} failed during execution: {}", transaction.getStringId(), e.getMessage());
                    transaction.fail(e.getMessage());
                } else {
                    throw new AplTransactionExecutionException("Transaction Type: " + transaction.getType().getSpec() + " does not support failures during execution", e, transaction);
                }
            }
        } else {
            applyAppendage(transaction, senderAccount, recipientAccount, appendage);
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

    private void applyFailed(Transaction transaction) {
        long feeATM = transaction.getFeeATM();
        Account sender = accountService.getAccount(transaction.getId());
        accountService.addToBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, transaction.getId(), 0, -feeATM);
    }

    private boolean applyUnconfirmedFailed(Transaction tx) {
        Account sender = accountService.getAccount(tx.getSenderId());
        if (sender.getUnconfirmedBalanceATM() >= tx.getFeeATM()) {
            accountService.addToUnconfirmedBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, tx.getId(), 0, -tx.getFeeATM());
            return true;
        } else {
            return false;
        }
    }

    private void undoUnconfirmedFailed(Transaction tx) {
        Account sender = accountService.getAccount(tx.getSenderId());
        accountService.addToUnconfirmedBalanceATM(sender, LedgerEvent.TRANSACTION_FEE, tx.getId(), 0, tx.getFeeATM());
    }
}
