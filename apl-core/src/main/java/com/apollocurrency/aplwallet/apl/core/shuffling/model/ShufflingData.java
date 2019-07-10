/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.model;

import com.apollocurrency.aplwallet.apl.core.db.model.DerivedEntity;

import java.util.Arrays;
import java.util.Objects;

public class ShufflingData extends DerivedEntity {

    private final long shufflingId;
    private final long accountId;
    private final byte[][] data;
    private final int transactionTimestamp;

    public ShufflingData(Long dbId, Integer height, long shufflingId, long accountId, byte[][] data, int transactionTimestamp) {
        super(dbId, height);
        this.shufflingId = shufflingId;
        this.accountId = accountId;
        this.data = data;
        this.transactionTimestamp = transactionTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShufflingData)) return false;
        if (!super.equals(o)) return false;
        ShufflingData that = (ShufflingData) o;
        return shufflingId == that.shufflingId &&
                accountId == that.accountId &&
                transactionTimestamp == that.transactionTimestamp &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), shufflingId, accountId, transactionTimestamp);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public long getShufflingId() {
        return shufflingId;
    }

    public long getAccountId() {
        return accountId;
    }

    public byte[][] getData() {
        return data;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }
}

