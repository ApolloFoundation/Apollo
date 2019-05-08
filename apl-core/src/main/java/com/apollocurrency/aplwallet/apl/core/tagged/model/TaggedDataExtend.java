/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class TaggedDataExtend {

    private long taggedDataId;
    private DbKey dbKey;
    private int height;
    private long extendId;

    public TaggedDataExtend(long taggedDataId, int height, long extendId) {
        this.taggedDataId = taggedDataId;
        this.height = height;
        this.extendId = extendId;
    }

    public TaggedDataExtend(ResultSet rs, DbKey dbKey) throws SQLException {
        this.taggedDataId = rs.getLong("id");
        this.dbKey = dbKey;
        this.height = rs.getInt("height");
        this.extendId = rs.getLong("extend_id");
    }

    public TaggedDataExtend(ResultSet rs) throws SQLException {
        this.taggedDataId = rs.getLong("id");
        this.height = rs.getInt("height");
        this.extendId = rs.getLong("extend_id");
    }

    public long getTaggedDataId() {
        return taggedDataId;
    }

    public void setTaggedDataId(long taggedDataId) {
        this.taggedDataId = taggedDataId;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getExtendId() {
        return extendId;
    }

    public void setExtendId(int extendId) {
        this.extendId = extendId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaggedDataExtend that = (TaggedDataExtend) o;
        return taggedDataId == that.taggedDataId &&
                height == that.height &&
                extendId == that.extendId &&
                Objects.equals(dbKey, that.dbKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taggedDataId, dbKey, height, extendId);
    }
}
