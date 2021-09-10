/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.derived;


import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ToString
public abstract class DerivedEntity implements Comparable<DerivedEntity>, Cloneable {
    protected static final int DEFAULT_HEIGHT = -1;
    private static final long DEFAULT_DB_ID = 0L;
    private volatile DbKey dbKey;
    private long dbId;
    private int height;

    public DerivedEntity(Long dbId, Integer height) {
        this.dbId = dbId == null ? DEFAULT_DB_ID : dbId;
        this.height = height == null ? DEFAULT_HEIGHT : height;
    }

    public DerivedEntity(ResultSet rs) throws SQLException {
        this.dbId = rs.getLong("db_id");
        this.height = rs.getInt("height");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DerivedEntity that = (DerivedEntity) o;
        return dbId == that.dbId && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbId, height);
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public long getDbId() {
        return dbId;
    }

    public boolean isNew() {
        return dbId == DEFAULT_DB_ID;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int compareTo(DerivedEntity o) {
        int res = Integer.compare(o.getHeight(), height);
        if (res == 0) {
            res = Long.compare(o.getDbId(), dbId);
        }
        return res;
    }
    public DerivedEntity clone() throws CloneNotSupportedException {
        return (DerivedEntity) super.clone();
    }

    public DerivedEntity deepCopy() {
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(getClass().getSimpleName() + " does not support Object.clone() operation");
        }
    }

    public boolean isSearchable() {
        return false;
    }

}
