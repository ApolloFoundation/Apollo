package com.apollocurrency.aplwallet.apl.core.db.dao.model;

/**
 * Shard db entity
 */
public class Shard {
    private Long shardId;
    private byte[] shardHash;
    private Long shardState;

    public Shard() {
    }

    public Shard(String shardHash) {
        this.shardHash = shardHash.getBytes();
    }

    public Shard(Long shardId, String shardHash) {
        this.shardId = shardId;
        this.shardHash = shardHash.getBytes();
    }

    public Shard(Long shardId, byte[] shardHash, Long shardState) {
        this.shardId = shardId;
        this.shardHash = shardHash;
        this.shardState = shardState;
    }

    public Long getShardId() {
        return shardId;
    }

    public void setShardId(Long shardId) {
        this.shardId = shardId;
    }

    public byte[] getShardHash() {
        return shardHash;
    }

    public void setShardHash(byte[] shardHash) {
        this.shardHash = shardHash;
    }

    public Long getShardState() {
        return shardState;
    }

    public void setShardState(Long shardState) {
        this.shardState = shardState;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private byte[] shardHash;
        private Long shardState;

        private ShardBuilder() {
        }

        public ShardBuilder id(Long shardId) {
            this.shardId = shardId;
            return this;
        }

        public ShardBuilder shardHash(byte[] shardHash) {
            this.shardHash = shardHash;
            return this;
        }

        public ShardBuilder shardState(Long shardState) {
            this.shardState = shardState;
            return this;
        }

        public Shard build() {
            return new Shard(shardId, shardHash, shardState);
        }
    }
}
