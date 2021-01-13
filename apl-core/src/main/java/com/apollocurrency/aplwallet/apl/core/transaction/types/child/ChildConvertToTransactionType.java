/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.child;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class ChildConvertToTransactionType extends ChildAccountTransactionType {
    @Inject
    public ChildConvertToTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        super.doStateIndependentValidation(transaction);
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        if (attachment.getChildCount() != 1) {
            throw new AplException.NotValidException("Wrong value of the child count value, only one account can be converted at once.");
        }
        throw new AplException.NotValidException("Not implemented yet.");
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        super.applyAttachment(transaction, senderAccount, recipientAccount);
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        //TODO: not implemented yet
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CHILD_ACCOUNT_CONVERT_TO;
    }

    @Override
    public LedgerEvent getLedgerEvent() {
        return LedgerEvent.CHILD_CONVERT_TO;
    }

    @Override
    public String getName() {
        return "ConvertToChildAccount";
    }

    @Override
    public ChildAccountAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
        return new ChildAccountAttachment(buffer);
    }

    @Override
    public ChildAccountAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
        return new ChildAccountAttachment(attachmentData);
    }
}
