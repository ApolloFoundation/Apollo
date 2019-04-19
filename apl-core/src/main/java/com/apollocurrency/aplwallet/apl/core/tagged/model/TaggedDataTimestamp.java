/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public class TaggedDataTimestamp {

    private long id;
    private DbKey dbKey;
    private int timestamp;
    private int height;

    public TaggedDataTimestamp(long id, int timestamp) {
        this.id = id;
        this.timestamp = timestamp;
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
}
