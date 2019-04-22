/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public class TaggedDataExtend {

    private long id;
    private DbKey dbKey;
    private int height;
    private long extendId;

    public TaggedDataExtend(long id, int height, int extendId) {
        this.id = id;
        this.height = height;
        this.extendId = extendId;
    }

    public TaggedDataExtend(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.height = rs.getInt("height");
        this.extendId = rs.getLong("extend_id");
    }

    public TaggedDataExtend(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.height = rs.getInt("height");
        this.extendId = rs.getLong("extend_id");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
}
