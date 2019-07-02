/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class TaggedDataTimestamp {

    private long id;
    private DbKey dbKey;
    private int timestamp;
    private int height;

    public TaggedDataTimestamp(long id, int timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public TaggedDataTimestamp(long id, int timestamp, int height) {
        this.id = id;
        this.timestamp = timestamp;
        this.height = height;
    }

    public TaggedDataTimestamp(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.timestamp = rs.getInt("timestamp");
        this.height = rs.getInt("height");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaggedDataTimestamp that = (TaggedDataTimestamp) o;
        return id == that.id &&
                timestamp == that.timestamp &&
                height == that.height &&
                Objects.equals(dbKey, that.dbKey);
    }

    @Override
    public String toString() {
        return "TaggedDataTimestamp{" +
                "id=" + id +
                ", dbKey=" + dbKey +
                ", timestamp=" + timestamp +
                ", height=" + height +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dbKey, timestamp, height);
    }
}
