/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.smc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class SmcTransaction extends TransactionType {

    public SmcTransaction(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public String getName() {
        return getSpec().getCompatibleName();
    }

    @Override
    public boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return false;
    }

    @Override
    public @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE) void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {

    }

    @Override
    public boolean isPhasingSafe() {
        return true;
    }
}
