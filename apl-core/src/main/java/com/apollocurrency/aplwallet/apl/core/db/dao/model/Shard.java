package com.apollocurrency.aplwallet.apl.core.db.dao.model;

/**
 * Shard db dto entity
 */
public class Shard {
    private Long shardId;
    private String shardHash;

    public Shard() {
    }

    public Shard(String shardHash) {
        this.shardHash = shardHash;
    }

    public Shard(Long shardId, String shardHash) {
        this.shardId = shardId;
        this.shardHash = shardHash;
    }

    public Long getShardId() {
        return shardId;
    }

    public void setShardId(Long shardId) {
        this.shardId = shardId;
    }

    public String getShardHash() {
        return shardHash;
    }

    public void setShardHash(String shardHash) {
        this.shardHash = shardHash;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private String shardHash;

        private ShardBuilder() {
        }

        public ShardBuilder id(Long shardId) {
            this.shardId = shardId;
            return this;
        }

        public ShardBuilder shardHash(String shardHash) {
            this.shardHash = shardHash;
            return this;
        }

        public Shard build() {
            return new Shard(/*shardId, */shardHash);
        }
    }
}
