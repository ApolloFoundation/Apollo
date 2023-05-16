/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.appdata;

import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;

import java.util.Objects;

public class ReferencedTransaction extends DerivedEntity {
    private Long transactionId;
    private Long referencedTransactionId;

    public ReferencedTransaction(Long dbId, Long transactionId, Long referencedTransactionId, Integer height) {
        super(dbId, height);
        this.transactionId = transactionId;
        this.referencedTransactionId = referencedTransactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferencedTransaction that = (ReferencedTransaction) o;
        return Objects.equals(getDbId(), that.getDbId()) &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(referencedTransactionId, that.referencedTransactionId) &&
                Objects.equals(getHeight(), that.getHeight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDbId(), transactionId, referencedTransactionId, getHeight());
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getReferencedTransactionId() {
        return referencedTransactionId;
    }

    public void setReferencedTransactionId(Long referencedTransactionId) {
        this.referencedTransactionId = referencedTransactionId;
    }
}
