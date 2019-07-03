/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon.model;

import java.util.Objects;

public class ShardDTO {
    private long shardId;
    private String shardHash;
    private String zipHashCrc;
    private int shardHeight;
    private int shardState;

    public ShardDTO() {
    }

    public long getShardId() {
        return shardId;
    }

    public void setShardId(long shardId) {
        this.shardId = shardId;
    }

    public String getShardHash() {
        return shardHash;
    }

    public void setShardHash(String shardHash) {
        this.shardHash = shardHash;
    }

    public String getZipHashCrc() {
        return zipHashCrc;
    }

    public void setZipHashCrc(String zipHashCrc) {
        this.zipHashCrc = zipHashCrc;
    }

    public int getShardHeight() {
        return shardHeight;
    }

    public void setShardHeight(int shardHeight) {
        this.shardHeight = shardHeight;
    }

    public int getShardState() {
        return shardState;
    }

    public void setShardState(int shardState) {
        this.shardState = shardState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShardDTO)) return false;
        ShardDTO shardDTO = (ShardDTO) o;
        return shardId == shardDTO.shardId &&
                shardHeight == shardDTO.shardHeight &&
                shardState == shardDTO.shardState &&
                Objects.equals(shardHash, shardDTO.shardHash) &&
                Objects.equals(zipHashCrc, shardDTO.zipHashCrc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shardId, shardHash, zipHashCrc, shardHeight, shardState);
    }
}
