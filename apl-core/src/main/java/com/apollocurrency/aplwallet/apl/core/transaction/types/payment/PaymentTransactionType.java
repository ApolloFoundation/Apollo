/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.payment;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author al
 */
@Slf4j
public abstract class PaymentTransactionType extends TransactionType {

    public PaymentTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (recipientAccount == null) {
            getAccountService().addToBalanceAndUnconfirmedBalanceATM(getAccountService().getAccount(GenesisImporter.CREATOR_ID), getLedgerEvent(), transaction.getId(), transaction.getAmountATM());
            log.info("{} burnt {} ATM", senderAccount.balanceString(), transaction.getAmountATM());
        }
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final boolean canHaveRecipient() {
        return true;
    }

    @Override
    public final boolean isPhasingSafe() {
        return true;
    }

    @Override
    protected void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        long maxBalanceATM = getBlockchainConfig().getCurrentConfig().getMaxBalanceATM();
        if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= maxBalanceATM) {
            throw new AplException.NotValidException("Invalid payment, amount of the transaction should be in range [1.." + maxBalanceATM + "]");
        }
    }
}
