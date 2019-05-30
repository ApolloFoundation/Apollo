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
    private Long shardState;
    private Integer shardHeight;
    private byte[] zipHashCrc;

    public Shard() {
    }

    public Shard copy() {
        byte[] shardHashCopy = Arrays.copyOf(shardHash, shardHash.length);
        byte[] shardZipHashCrcCopy = zipHashCrc != null && zipHashCrc.length > 0 ?
                Arrays.copyOf(zipHashCrc, zipHashCrc.length) : null;
        return new Shard(shardId, shardHashCopy, shardState, shardHeight, shardZipHashCrcCopy);
    }

    public Shard(Integer shardHeight) {
        this.shardHeight = shardHeight;
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

    public Shard(Long shardId, byte[] shardHash, Long shardState, Integer shardHeight, byte[] zipHashCrc) {
        this.shardId = shardId;
        this.shardHash = shardHash;
        this.shardState = shardState;
        this.shardHeight = shardHeight;
        this.zipHashCrc = zipHashCrc;
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
                Arrays.equals(zipHashCrc, shard.zipHashCrc);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(shardId, shardState, shardHeight);
        result = 31 * result + Arrays.hashCode(shardHash);
        result = 31 * result + Arrays.hashCode(zipHashCrc);
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

    public Long getShardState() {
        return shardState;
    }

    public void setShardState(Long shardState) {
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

    public static ShardBuilder builder() {
        return new ShardBuilder();
    }

    public static final class ShardBuilder {
        private Long shardId;
        private byte[] shardHash;
        private Long shardState;
        private Integer shardHeight;
        private byte[] zipHashCrc;

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

        public ShardBuilder shardHeight(Integer shardHeight) {
            this.shardHeight = shardHeight;
            return this;
        }

        public ShardBuilder zipHashCrc(byte[] zipHashCrc) {
            this.zipHashCrc= zipHashCrc;
            return this;
        }

        public Shard build() {
            return new Shard(shardId, shardHash, shardState, shardHeight, zipHashCrc);
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
        if (zipHashCrc == null) sb.append("null");
        else {
            sb.append('[').append(Convert.toHexString(zipHashCrc)).append(']');
        }
        sb.append('}');
        return sb.toString();
    }
}
