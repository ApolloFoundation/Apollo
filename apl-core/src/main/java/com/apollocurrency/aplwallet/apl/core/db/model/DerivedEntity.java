/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;


import com.apollocurrency.aplwallet.apl.core.db.DbKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public abstract class DerivedEntity implements Comparable {
    private DbKey dbKey;
    private static final long DEFAULT_DB_ID = 0L;
    private static final int DEFAULT_HEIGHT = -1;

    private long dbId;
    private int height;

    public DerivedEntity(Long dbId, Integer height) {
        this.dbId = dbId == null ? DEFAULT_DB_ID : dbId;
        this.height = height == null ? DEFAULT_HEIGHT : height;
    }

    public void setDbId(long dbId) {
        this.dbId = dbId;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public DerivedEntity(ResultSet rs) throws SQLException {
        this.dbId = rs.getLong("db_id");
        this.height = rs.getInt("height");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DerivedEntity)) return false;
        DerivedEntity that = (DerivedEntity) o;
        return dbId == that.dbId &&
                height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbId, height);
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public long getDbId() {
        return dbId;
    }

    public int getHeight() {
        return height;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    @Override
    public int compareTo(Object o) {
        DerivedEntity entity = (DerivedEntity) o;
        int res = Integer.compare(entity.getHeight(), height);
        if (res == 0) {
            res = Long.compare(entity.getDbId(), dbId);
        }
        return res;
    }
}
