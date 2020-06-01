/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.operation.tagged;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class TaggedDataTimestamp extends VersionedDerivedEntity {

    private long id;
    private int timestamp;

    public TaggedDataTimestamp(long id, int timestamp) {
        super(null, null);
        this.id = id;
        this.timestamp = timestamp;
    }

    public TaggedDataTimestamp(long id, int timestamp, int height) {
        super(null, height);
        this.id = id;
        this.timestamp = timestamp;
    }

    public TaggedDataTimestamp(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        setDbKey(dbKey);
        this.id = rs.getLong("id");
        this.timestamp = rs.getInt("timestamp");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaggedDataTimestamp that = (TaggedDataTimestamp) o;
        return id == that.id &&
            timestamp == that.timestamp &&
            this.getHeight() == that.getHeight() &&
            Objects.equals(this.getDbKey(), that.getDbKey());
    }

    @Override
    public String toString() {
        return "TaggedDataTimestamp{" +
            "id=" + id +
            ", dbKey=" + this.getDbKey() +
            ", timestamp=" + timestamp +
            ", height=" + getHeight() +
            ", latest=" + isLatest() +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, this.getDbKey(), timestamp, getHeight());
    }
}
