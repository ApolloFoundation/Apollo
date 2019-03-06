package com.apollocurrency.aplwallet.apl.core.db.dao.model;

/**
 * Transaction global secondary index entity.
 */
public class TransactionIndex {
    private Long transactionId;
    private Long blockId;

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
