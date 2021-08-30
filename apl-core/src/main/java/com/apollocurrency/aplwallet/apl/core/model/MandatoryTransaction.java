/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import java.util.Arrays;

public class MandatoryTransaction extends WrappedTransaction {
    private final byte[] requiredTxHash;

    public MandatoryTransaction(Transaction transaction, byte[] requiredTxHash) {
        super(transaction);
        this.requiredTxHash = requiredTxHash;
    }

    public byte[] getRequiredTxHash() {
        return requiredTxHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MandatoryTransaction)) return false;
        if (!super.equals(o)) return false;
        MandatoryTransaction that = (MandatoryTransaction) o;
        return Arrays.equals(requiredTxHash, that.requiredTxHash);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(requiredTxHash);
        return result;
    }

    @Override
    public String toString() {
        return "MandatoryTransaction{" +
            "transaction=" + transaction +
            ", requiredTxHash=" + Arrays.toString(requiredTxHash) +
            '}';
    }
}
