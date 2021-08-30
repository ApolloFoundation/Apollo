/*
 *  Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.types.child;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;

/**
 * Create child account transaction.
 * Sender is a parent account. There is an list of child public keys in the attachment.
 *
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
public abstract class ChildAccountTransactionType extends TransactionType {

    public ChildAccountTransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        super(blockchainConfig, accountService);
    }

    boolean isAccountExists(byte[] childPublicKey) {
        return getAccountService().getAccount(childPublicKey) != null;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.NotValidException {
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        if (transaction.getAmountATM() != 0 ) {
            throw new AplException.NotValidException("Wrong value of the transaction amount "+transaction.getAmountATM());
        }
        if (attachment.getChildCount() <= 0 || attachment.getChildCount() != attachment.getChildPublicKey().size()){
            throw new AplException.NotValidException("Wrong value of the child count, count=" + attachment.getChildCount());
        }
        for (byte[] publicKey : attachment.getChildPublicKey()) {
            if(Arrays.equals(publicKey, transaction.getSenderPublicKey())) {
                throw new AplException.NotValidException("Wrong value of the child public keys, a child can't simultaneously be a parent.");
            }
            if (!Crypto.isCanonicalPublicKey(publicKey)) {
                throw new AplException.NotValidException("Invalid child public key: " + Convert.toHexString(publicKey));
            }
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        for (byte[] publicKey : attachment.getChildPublicKey()) {
            if (TransactionType.isDuplicate(TransactionTypes.TransactionTypeSpec.CHILD_ACCOUNT_CREATE, Convert.toHexString(publicKey), duplicates, true)) {
                return true;
            }
        }
        return super.isDuplicate(transaction, duplicates);
    }

    @Override
    public final boolean canHaveRecipient() {
        return true;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

}
