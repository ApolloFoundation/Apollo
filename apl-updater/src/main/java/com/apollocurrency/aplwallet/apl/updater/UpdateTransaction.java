/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;

public class UpdateTransaction {
    private Transaction transaction;
    private Long transactionId;
    private boolean updated;

    public UpdateTransaction(Transaction transaction, Long transactionId, boolean updated) {
        this.transaction = transaction;
        this.updated = updated;
        this.transactionId = transactionId;
    }

    public UpdateTransaction(Long transactionId, boolean updated) {
        this.transactionId = transactionId;
        this.updated = updated;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean requireManualUpdate() {
        return transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.MINOR_UPDATE;
    }

}
