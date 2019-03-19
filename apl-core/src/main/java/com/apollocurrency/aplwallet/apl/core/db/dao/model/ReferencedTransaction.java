/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Objects;

public class ReferencedTransaction {
    private Long dbId;
    private Long transactionId;
    private Long referencedTransactionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferencedTransaction)) return false;
        ReferencedTransaction that = (ReferencedTransaction) o;
        return Objects.equals(dbId, that.dbId) &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(referencedTransactionId, that.referencedTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbId, transactionId, referencedTransactionId);
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
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

    public ReferencedTransaction(Long dbId, Long transactionId, Long referencedTransactionId) {
        this.dbId = dbId;
        this.transactionId = transactionId;
        this.referencedTransactionId = referencedTransactionId;
    }
}
