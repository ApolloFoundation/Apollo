package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Transaction global secondary index entity.
 */
public class TransactionIndex {
    private Long transactionId;
    private byte[] partialTransactionHash;
    private Long blockId;

    public TransactionIndex(Long transactionId, byte[] partialTransactionHash, Long blockId) {
        this.transactionId = transactionId;
        this.partialTransactionHash = partialTransactionHash;
        this.blockId = blockId;
    }

    public TransactionIndex copy() {
        byte[] hashCopy = Arrays.copyOf(partialTransactionHash, partialTransactionHash.length);
        return new TransactionIndex(transactionId, hashCopy, blockId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionIndex)) return false;
        TransactionIndex that = (TransactionIndex) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(blockId, that.blockId) &&
                Arrays.equals(partialTransactionHash, that.partialTransactionHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(transactionId, blockId);
        result = 31 * result + Arrays.hashCode(partialTransactionHash);
        return result;
    }

    public TransactionIndex() {
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

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public static TransactionIndexBuilder builder() {
        return new TransactionIndexBuilder();
    }

    public static final class TransactionIndexBuilder {
        private Long transactionId;
        private Long blockId;
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

        public TransactionIndexBuilder blockId(Long blockId) {
            this.blockId = blockId;
            return this;
        }

        public TransactionIndex build() {
            return new TransactionIndex(transactionId, partialTransactionHash, blockId);
        }
    }
}
