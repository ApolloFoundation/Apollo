package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ReferencedTransactionDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplier;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixApplierRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
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

    // returns false iff double spending
    public boolean applyUnconfirmed(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
    }

    public void apply(Transaction transaction) {
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
                AppendixApplier<AbstractAppendix> applier = applierRegistry.getFor(appendage);
                if (applier == null) {
                    appendage.apply(transaction, senderAccount, recipientAccount);
                } else {
                    applier.apply(transaction, appendage, senderAccount, recipientAccount);
                }
            }
        }
    }

    public void undoUnconfirmed(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        transaction.getType().undoUnconfirmed(transaction, senderAccount);
    }
}
