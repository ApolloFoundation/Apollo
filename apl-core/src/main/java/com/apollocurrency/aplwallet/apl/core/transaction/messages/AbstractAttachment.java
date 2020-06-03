/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class AbstractAttachment extends AbstractAppendix implements Attachment {

    public AbstractAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public AbstractAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public AbstractAttachment(int version) {
        super(version);
    }

    public AbstractAttachment() {
    }

    @Override
    public String getAppendixName() {
        return getTransactionType().getName();
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        getTransactionType().validateAttachment(transaction);
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        getTransactionType().apply((TransactionImpl) transaction, senderAccount, recipientAccount);
    }

    @Override
    public final Fee getBaselineFee(Transaction transaction) {
        return getTransactionType().getBaselineFee(transaction);
    }

    @Override
    public final Fee getNextFee(Transaction transaction) {
        return getTransactionType().getNextFee(transaction);
    }

    @Override
    public final int getBaselineFeeHeight() {
        return getTransactionType().getBaselineFeeHeight();
    }

    @Override
    public final int getNextFeeHeight() {
        return getTransactionType().getNextFeeHeight();
    }

    @Override
    public boolean isPhasable() {
        return !(this instanceof Prunable) && getTransactionType().isPhasable();
    }

    public int getFinishValidationHeight(Transaction transaction) {
        return isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : lookupBlockchain().getHeight();
    }

}
