package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Transaction global secondary index entity.
 */
public class TransactionIndex {
    private Long transactionId;
    private byte[] partialTransactionHash;
    private Integer height;
    private Short transactionIndex;

    public TransactionIndex(Long transactionId, byte[] partialTransactionHash, Integer height, Short transactionIndex) {
        this.transactionId = transactionId;
        this.partialTransactionHash = partialTransactionHash;
        this.height = height;
        this.transactionIndex = transactionIndex;
    }

    public TransactionIndex() {
    }

    public static TransactionIndexBuilder builder() {
        return new TransactionIndexBuilder();
    }

    public TransactionIndex copy() {
        byte[] hashCopy = Arrays.copyOf(partialTransactionHash, partialTransactionHash.length);
        return new TransactionIndex(transactionId, hashCopy, height, transactionIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionIndex)) return false;
        TransactionIndex that = (TransactionIndex) o;
        return Objects.equals(transactionId, that.transactionId) &&
            Arrays.equals(partialTransactionHash, that.partialTransactionHash) &&
            Objects.equals(height, that.height) &&
            Objects.equals(transactionIndex, that.transactionIndex);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(transactionId, height, transactionIndex);
        result = 31 * result + Arrays.hashCode(partialTransactionHash);
        return result;
    }

    public byte[] getPartialTransactionHash() {
        return partialTransactionHash;
    }

    public void setPartialTransactionHash(byte[] partialTransactionHash) {
        this.partialTransactionHash = partialTransactionHash;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Short getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(Short transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    public static final class TransactionIndexBuilder {
        private Long transactionId;
        private Integer height;
        private Short transactionIndex;
        private byte[] partialTransactionHash;

        private TransactionIndexBuilder() {
        }

        public TransactionIndexBuilder partialTransactionHash(byte[] partialTransactionHash) {
            this.partialTransactionHash = partialTransactionHash;
            return this;
        }

        public TransactionIndexBuilder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public TransactionIndexBuilder height(Integer height) {
            this.height = height;
            return this;
        }

        public TransactionIndexBuilder transactinIndex(Short transactionIndex) {
            this.transactionIndex = transactionIndex;
            return this;
        }

        public TransactionIndex build() {
            return new TransactionIndex(transactionId, partialTransactionHash, height, transactionIndex);
        }
    }
}
