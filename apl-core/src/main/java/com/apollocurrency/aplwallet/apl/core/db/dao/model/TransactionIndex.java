package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Objects;

/**
 * Transaction global secondary index entity.
 */
public class TransactionIndex {
    private Long transactionId;
    private Long blockId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionIndex)) return false;
        TransactionIndex that = (TransactionIndex) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(blockId, that.blockId);
    }

    @Override
    public String toString() {
        return "TransactionIndex{" +
                "transactionId=" + transactionId +
                ", blockId=" + blockId +
                '}';
    }

    public TransactionIndex copy() {
        return new TransactionIndex(transactionId, blockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, blockId);
    }

    public TransactionIndex() {
    }

    public TransactionIndex(Long transactionId, Long blockId) {
        this.transactionId = transactionId;
        this.blockId = blockId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long transactionId;
        private Long blockId;

        private ShardBuilder() {
        }

        public ShardBuilder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public ShardBuilder blockId(Long blockId) {
            this.blockId = blockId;
            return this;
        }

        public TransactionIndex build() {
            return new TransactionIndex(transactionId, blockId);
        }
    }
}
