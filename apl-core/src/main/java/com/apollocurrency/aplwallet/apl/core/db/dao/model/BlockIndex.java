package com.apollocurrency.aplwallet.apl.core.db.dao.model;

/**
 * Shard db dto entity
 */
public class BlockIndex {
    private Long shardId;
    private Long blockId;
    private Integer blockHeight;

    public BlockIndex() {
    }

    public BlockIndex(Long shardId, Long blockId, Integer blockHeight) {
        this.shardId = shardId;
        this.blockId = blockId;
        this.blockHeight = blockHeight;
    }

    public Long getShardId() {
        return shardId;
    }

    public void setShardId(Long shardId) {
        this.shardId = shardId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public Integer getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Integer blockHeight) {
        this.blockHeight = blockHeight;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private Long blockId;
        private Integer blockHeight;

        private ShardBuilder() {
        }

        public ShardBuilder shardId(Long shardId) {
            this.shardId = shardId;
            return this;
        }

        public ShardBuilder blockId(Long blockId) {
            this.blockId = blockId;
            return this;
        }

        public ShardBuilder blockHeight(Integer blockHeight) {
            this.blockHeight = blockHeight;
            return this;
        }

        public BlockIndex build() {
            return new BlockIndex(shardId, blockId, blockHeight);
        }
    }
}
