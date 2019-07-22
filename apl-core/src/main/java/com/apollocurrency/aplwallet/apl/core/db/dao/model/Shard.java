package com.apollocurrency.aplwallet.apl.core.db.dao.model;

import java.util.Arrays;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.crypto.Convert;

/**
 * Shard db entity
 */
public class Shard {
    private Long shardId;
    private byte[] shardHash;
    private ShardState shardState;
    private Integer shardHeight;
    private byte[] zipHashCrc;
    private long[] generatorIds; // tree latest generator Ids
    private int[] blockTimeouts;  // two previous block timeout values
    private int[] blockTimestamps; // two previous block timestamp values

    public Shard() {
    }

    public Shard copy() {
        byte[] shardHashCopy = Arrays.copyOf(shardHash, shardHash.length);
        byte[] shardZipHashCrcCopy = zipHashCrc != null && zipHashCrc.length > 0 ?
                Arrays.copyOf(zipHashCrc, zipHashCrc.length) : null;
        long[] generatorIds = this.generatorIds != null && this.generatorIds.length > 0 ?
                Arrays.copyOf(this.generatorIds, this.generatorIds.length) : Convert.EMPTY_LONG;
        int[] blockTimeouts = this.blockTimeouts != null && this.blockTimeouts.length > 0 ?
                Arrays.copyOf(this.blockTimeouts, this.blockTimeouts.length) : Convert.EMPTY_INT;
        int[] blockTimestamps = this.blockTimestamps != null && this.blockTimestamps.length > 0 ?
                Arrays.copyOf(this.blockTimestamps, this.blockTimestamps.length) : Convert.EMPTY_INT;
        return new Shard(shardId, shardHashCopy, shardState, shardHeight,
                shardZipHashCrcCopy, generatorIds, blockTimeouts, blockTimestamps);
    }

    public Shard(long id, Integer shardHeight) {
        this.shardHeight = shardHeight;
        this.shardId = id;
    }

    public Shard(byte[] shardHash, Integer shardHeight) {
        this.shardHash = shardHash;
        this.shardHeight = shardHeight;
    }

    public Shard(Long shardId, byte[] shardHash, Integer shardHeight, byte[] zipHashCrc) {
        this.shardId = shardId;
        this.shardHash = shardHash;
        this.shardHeight = shardHeight;
        this.zipHashCrc = zipHashCrc;
    }

    public Shard(Long shardId, String shardHash, Integer shardHeight) {
        this.shardId = shardId;
        this.shardHash = Convert.parseHexString(shardHash);
        this.shardHeight = shardHeight;
    }

    public Shard(Long shardId, byte[] shardHash, ShardState shardState, Integer shardHeight,
                 byte[] zipHashCrc, long[] generatorIds, int[] blockTimeouts, int[] blockTimestamps) {
        this.shardId = shardId;
        this.shardHash = shardHash;
        this.shardState = shardState;
        this.shardHeight = shardHeight;
        this.zipHashCrc = zipHashCrc;
        this.generatorIds = generatorIds;
        this.blockTimeouts = blockTimeouts;
        this.blockTimestamps = blockTimestamps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shard shard = (Shard) o;
        return Objects.equals(shardId, shard.shardId) &&
                Arrays.equals(shardHash, shard.shardHash) &&
                Objects.equals(shardState, shard.shardState) &&
                Objects.equals(shardHeight, shard.shardHeight) &&
                Arrays.equals(zipHashCrc, shard.zipHashCrc) &&
                Arrays.equals(generatorIds, shard.generatorIds) &&
                Arrays.equals(blockTimeouts, shard.blockTimeouts) &&
                Arrays.equals(blockTimestamps, shard.blockTimestamps);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(shardId, shardState, shardHeight);
        result = 31 * result + Arrays.hashCode(shardHash);
        result = 31 * result + Arrays.hashCode(zipHashCrc);
        result = 31 * result + Arrays.hashCode(generatorIds);
        result = 31 * result + Arrays.hashCode(blockTimeouts);
        result = 31 * result + Arrays.hashCode(blockTimestamps);
        return result;
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

    public ShardState getShardState() {
        return shardState;
    }

    public void setShardState(ShardState shardState) {
        this.shardState = shardState;
    }

    public Integer getShardHeight() {
        return shardHeight;
    }

    public void setShardHeight(Integer shardHeight) {
        this.shardHeight = shardHeight;
    }

    public byte[] getZipHashCrc() {
        return zipHashCrc;
    }

    public void setZipHashCrc(byte[] zipHashCrc) {
        this.zipHashCrc = zipHashCrc;
    }

    public long[] getGeneratorIds() {
        return generatorIds;
    }

    public void setGeneratorIds(long[] generatorIds) {
        this.generatorIds = generatorIds;
    }

    public int[] getBlockTimeouts() {
        return blockTimeouts;
    }

    public void setBlockTimeouts(int[] blockTimeouts) {
        this.blockTimeouts = blockTimeouts;
    }

    public int[] getBlockTimestamps() {
        return blockTimestamps;
    }

    public void setBlockTimestamps(int[] blockTimestamps) {
        this.blockTimestamps = blockTimestamps;
    }

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private byte[] shardHash;
        private ShardState shardState;
        private Integer shardHeight;
        private byte[] zipHashCrc;
        private long[] generatorIds;
        private int[] blockTimeouts;  // two previous block timeout values
        private int[] blockTimestamps; // two previous block timestamp values

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

        public ShardBuilder shardState(ShardState shardState) {
            this.shardState = shardState;
            return this;
        }

        public ShardBuilder shardHeight(Integer shardHeight) {
            this.shardHeight = shardHeight;
            return this;
        }

        public ShardBuilder zipHashCrc(byte[] zipHashCrc) {
            this.zipHashCrc = zipHashCrc;
            return this;
        }

        public ShardBuilder generatorIds(long[] generatorIds) {
            this.generatorIds = generatorIds;
            return this;
        }

        public ShardBuilder blockTimeouts(int[] blockTimeouts) {
            this.blockTimeouts = blockTimeouts;
            return this;
        }

        public ShardBuilder blockTimestamps(int[] blockTimestamps) {
            this.blockTimestamps = blockTimestamps;
            return this;
        }

        public Shard build() {
            return new Shard(shardId, shardHash, shardState, shardHeight,
                    zipHashCrc, generatorIds, blockTimeouts, blockTimestamps);
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Shard{");
        sb.append("shardId=").append(shardId);
        sb.append(", shardHash=");
        if (shardHash == null) sb.append("null");
        else {
            sb.append('[').append(Convert.toHexString(shardHash)).append(']');
        }
        sb.append(", shardState=").append(shardState);
        sb.append(", shardHeight=").append(shardHeight);
        sb.append(", zipHashCrc=");
        if (zipHashCrc == null) {
            sb.append("null");
        } else {
            sb.append('[').append(Convert.toHexString(zipHashCrc)).append(']');
        }
        sb.append(", generatorIds=");
        if (generatorIds == null) {
            sb.append("null");
        } else {
            sb.append('[').append(generatorIds).append(']');
        }
        sb.append(", blockTimeouts=");
        if (blockTimeouts == null) {
            sb.append("null");
        } else {
            sb.append('[').append(blockTimeouts).append(']');
        }
        sb.append(", blockTimestamps=");
        if (blockTimestamps == null) {
            sb.append("null");
        } else {
            sb.append('[').append(blockTimestamps).append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
