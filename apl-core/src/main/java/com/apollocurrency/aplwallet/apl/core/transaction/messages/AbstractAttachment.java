/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import org.json.simple.JSONObject;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class AbstractAttachment extends AbstractAppendix implements Attachment {
    private TransactionType transactionType;

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
    public void bindTransactionType(@NotNull TransactionType transactionType) {
        if (transactionType.getSpec() != getTransactionTypeSpec()) {
            throw new IllegalArgumentException("Required tx type " + getTransactionTypeSpec() + " but got " + transactionType.getSpec());
        }
        if (this.transactionType != null) {
            throw new IllegalStateException("Transaction type is already set");
        }
        this.transactionType = transactionType;
    }

    private TransactionType transactionType() {
        if (transactionType == null) {
            throw new IllegalStateException("Transaction type was not set");
        }
        return transactionType;
    }

    // TODO Resolve names to be compatible with old implementation
    @Override
    public String getAppendixName() {
        return getTransactionTypeSpec().toString();
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        transactionType().validateAttachment(transaction);
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        transactionType().apply((TransactionImpl) transaction, senderAccount, recipientAccount);
    }

    @Override
    public final Fee getBaselineFee(Transaction transaction) {
        return transactionType().getBaselineFee(transaction);
    }

    @Override
    public boolean isPhasable() {
        return !(this instanceof Prunable) && transactionType().isPhasable();
    }
}
