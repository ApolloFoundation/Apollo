package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Objects;

/**
 * Block global secondary index entity.
 */
public class BlockIndex {
    private Long shardId;
    private Long blockId;
    private Integer blockHeight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockIndex that = (BlockIndex) o;
        return Objects.equals(shardId, that.shardId) &&
                Objects.equals(blockId, that.blockId) &&
                Objects.equals(blockHeight, that.blockHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shardId, blockId, blockHeight);
    }

    public BlockIndex copy() {
        return new BlockIndex(shardId, blockId, blockHeight);
    }

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
