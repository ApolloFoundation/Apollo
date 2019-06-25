package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountEntity;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublickKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ReferencedTransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ReferencedTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionApplier {

    private BlockchainConfig blockchainConfig;
    private ReferencedTransactionDaoImpl referencedTransactionDao;
    private AccountService accountService;
    private AccountPublickKeyService accountPublickKeyService;


    @Inject
    public TransactionApplier(BlockchainConfig blockchainConfig, ReferencedTransactionDaoImpl referencedTransactionDao, AccountService accountService, AccountPublickKeyService accountPublickKeyService) {
        this.blockchainConfig = blockchainConfig;
        this.referencedTransactionDao = referencedTransactionDao;
        this.accountService = accountService;
        this.accountPublickKeyService = accountPublickKeyService;
    }

    // returns false iff double spending
    public boolean applyUnconfirmed(Transaction transaction) {
        AccountEntity senderAccount = accountService.getAccountEntity(transaction.getSenderId());
        return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
    }

    public void apply(Transaction transaction) {
        AccountEntity senderAccount = accountService.getAccountEntity(transaction.getSenderId());
        accountPublickKeyService.apply(senderAccount, transaction.getSenderPublicKey());
        AccountEntity recipientAccount = null;
        if (transaction.getRecipientId() != 0) {
            recipientAccount = accountService.getAccountEntity(transaction.getRecipientId());
            if (recipientAccount == null) {
                recipientAccount = accountService.addOrGetAccount(transaction.getRecipientId());
            }
        }
        if (transaction.getReferencedTransactionFullHash() != null) {
            accountService.addToUnconfirmedBalanceATM(senderAccount, transaction.getType().getLedgerEvent(), transaction.getId(),
                    0, blockchainConfig.getUnconfirmedPoolDepositAtm());

            referencedTransactionDao.insert(new ReferencedTransaction((long) 0, transaction.getId(), Convert.fullHashToId(transaction.referencedTransactionFullHash()), transaction.getHeight()));
        }
        if (transaction.attachmentIsPhased()) {
            accountService.addToBalanceATM(senderAccount, transaction.getType().getLedgerEvent(), transaction.getId(), 0, -transaction.getFeeATM());
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            if (!appendage.isPhased(transaction)) {
                appendage.loadPrunable(transaction);
                appendage.apply(transaction, senderAccount, recipientAccount);
            }
        }
    }

    public void undoUnconfirmed(Transaction transaction) {
        AccountEntity senderAccount = accountService.getAccountEntity(transaction.getSenderId());
        transaction.getType().undoUnconfirmed(transaction, senderAccount);
    }
}
