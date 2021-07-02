/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.dgs;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.DGSService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

public abstract class DigitalGoodsTransactionType extends TransactionType {

    protected final DGSService dgsService;

    public DigitalGoodsTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, DGSService service) {
        super(blockchainConfig, accountService);
        this.dgsService = service;
    }


    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException {
        if (transaction.getAmountATM() != 0) {
            throw new AplException.NotValidException("Invalid digital goods transaction");
        }
        doValidateAttachment(transaction);
    }

    public abstract void doValidateAttachment(Transaction transaction) throws AplException.ValidationException;

}
