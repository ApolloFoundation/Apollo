package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionApplier {
    // returns false iff double spending
    private BlockchainConfig blockchainConfig;

    @Inject
    public TransactionApplier(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = blockchainConfig;
    }

    public boolean applyUnconfirmed(Transaction transaction) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
    }

    public void apply(Transaction transaction) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        senderAccount.apply(transaction.getSenderPublicKey());
        Account recipientAccount = null;
        if (transaction.getRecipientId() != 0) {
            recipientAccount = Account.getAccount(transaction.getRecipientId());
            if (recipientAccount == null) {
                recipientAccount = Account.addOrGetAccount(transaction.getRecipientId());
            }
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceATM(transaction.getType().getLedgerEvent(), transaction.getId(),
                    0, blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
        if (transaction.attachmentIsPhased()) {
            senderAccount.addToBalanceATM(transaction.getType().getLedgerEvent(), transaction.getId(), 0, -transaction.getFeeATM());
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            if (!appendage.isPhased(transaction)) {
                appendage.loadPrunable(transaction);
                appendage.apply(transaction, senderAccount, recipientAccount);
            }
        }
    }

    public void undoUnconfirmed(Transaction transaction) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        transaction.getType().undoUnconfirmed(transaction, senderAccount);
    }
}
