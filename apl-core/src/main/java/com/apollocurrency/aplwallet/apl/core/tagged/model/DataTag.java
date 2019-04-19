/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;

public class DataTag {

    private String tag;
    private DbKey dbKey;
    private int height;
    private int count;

    public DataTag(String tag, int height, int count) {
        this.tag = tag;
        this.height = height;
        this.count = count;
    }

    public DataTag(String tag, int height) {
        this.tag = tag;
        this.height = height;
    }

    public DataTag(ResultSet rs, DbKey dbKey) throws SQLException {
        this.tag = rs.getString("tag");
        this.dbKey = dbKey;
        this.count = rs.getInt("tag_count");
        this.height = rs.getInt("height");
    }

    public DataTag(ResultSet rs) throws SQLException {
        this.tag = rs.getString("tag");
        this.count = rs.getInt("tag_count");
        this.height = rs.getInt("height");
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
