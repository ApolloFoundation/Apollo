/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.Update;

import java.util.Objects;

public class UpdateTransaction {
    private Transaction transaction;
    private boolean updated;

    public UpdateTransaction(Transaction transaction, boolean updated) {
        this.transaction = transaction;
        this.updated = updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateTransaction)) return false;
        UpdateTransaction that = (UpdateTransaction) o;
        return updated == that.updated &&
                Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, updated);
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
        return transaction.getType() == Update.MINOR;
    }

}
