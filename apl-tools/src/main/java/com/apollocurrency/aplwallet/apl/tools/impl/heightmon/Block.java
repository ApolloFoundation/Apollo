/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl.heightmon;

import java.util.Objects;

public class Block {
    private long id;
    private int height;
    private int timestamp;
    private int timeout;
    private int version;
    private long generatorId;

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", height=" + height +
                ", timestamp=" + timestamp +
                ", timeout=" + timeout +
                ", version=" + version +
                ", generatorId=" + generatorId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block = (Block) o;
        return id == block.id &&
                height == block.height &&
                timestamp == block.timestamp &&
                timeout == block.timeout &&
                version == block.version &&
                generatorId == block.generatorId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, height, timestamp, timeout, version, generatorId);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getGeneratorId() {
        return generatorId;
    }

    public void setGeneratorId(long generatorId) {
        this.generatorId = generatorId;
    }

    public Block() {
    }

    public Block(long id, int height, int timestamp, int timeout, int version, long generatorId) {
        this.id = id;
        this.height = height;
        this.timestamp = timestamp;
        this.timeout = timeout;
        this.version = version;
        this.generatorId = generatorId;
    }
}
