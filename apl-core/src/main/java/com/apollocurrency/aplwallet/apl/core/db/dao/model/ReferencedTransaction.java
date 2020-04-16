/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Objects;

public class ReferencedTransaction {
    private Long dbId;
    private Long transactionId;
    private Long referencedTransactionId;
    private Integer height;

    public ReferencedTransaction(Long dbId, Long transactionId, Long referencedTransactionId, Integer height) {
        this.dbId = dbId;
        this.transactionId = transactionId;
        this.referencedTransactionId = referencedTransactionId;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferencedTransaction that = (ReferencedTransaction) o;
        return Objects.equals(dbId, that.dbId) &&
            Objects.equals(transactionId, that.transactionId) &&
            Objects.equals(referencedTransactionId, that.referencedTransactionId) &&
            Objects.equals(height, that.height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbId, transactionId, referencedTransactionId, height);
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

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
