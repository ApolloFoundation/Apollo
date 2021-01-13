/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.transaction.Fee;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import lombok.NonNull;
import org.json.simple.JSONObject;

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
    public void bindTransactionType(@NonNull TransactionType transactionType) {
        if (transactionType.getSpec() != getTransactionTypeSpec()) {
            throw new IllegalArgumentException("Required tx type " + getTransactionTypeSpec() + " but got " + transactionType.getSpec());
        }
        this.transactionType = transactionType;
    }

    private TransactionType transactionType() {
        if (this.transactionType == null) {
            throw new IllegalStateException("Transaction type was not set");
        }
        return this.transactionType;
    }

    @Override
    public String getAppendixName() {
        return getTransactionTypeSpec().getCompatibleName();
    }

    @Override
    public void performFullValidation(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        transactionType().doStateIndependentValidation(transaction);
        transactionType().doStateDependentValidation(transaction);
    }

    @Override
    public void performLightweightValidation(Transaction transaction, int blockcHeight) throws AplException.ValidationException {
        transactionType().doStateIndependentValidation(transaction);
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        transactionType().apply(transaction, senderAccount, recipientAccount);
    }

    @Override
    public final Fee getBaselineFee(Transaction transaction, long oneAPL) {
        return transactionType().getBaselineFee(transaction);
    }

    @Override
    public boolean isPhasable() {
        return !(this instanceof Prunable) && transactionType().isPhasable();
    }

    @Override
    public String toString() {
        return "Attachment[" + getClass().getSimpleName() + ", type = " + getTransactionTypeSpec()  + "]";
    }

}
