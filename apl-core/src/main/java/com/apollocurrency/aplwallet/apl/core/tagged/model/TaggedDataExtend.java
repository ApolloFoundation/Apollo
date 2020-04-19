/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class TaggedDataExtend extends VersionedDerivedEntity {

    private long taggedDataId;
    private long extendId;

    public TaggedDataExtend(Long dbId, Integer height, long taggedDataId, long extendId) {
        super(dbId, height);
        this.taggedDataId = taggedDataId;
        this.extendId = extendId;
    }

    public TaggedDataExtend(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.taggedDataId = rs.getLong("id");
        setDbKey(dbKey);
        this.extendId = rs.getLong("extend_id");
    }

    public long getTaggedDataId() {
        return taggedDataId;
    }

    public void setTaggedDataId(long taggedDataId) {
        this.taggedDataId = taggedDataId;
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
        if (!(o instanceof TaggedDataExtend)) return false;
        if (!super.equals(o)) return false;
        TaggedDataExtend that = (TaggedDataExtend) o;
        return taggedDataId == that.taggedDataId &&
            extendId == that.extendId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), taggedDataId, extendId);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TaggedDataExtend{");
        sb.append("taggedDataId=").append(taggedDataId);
        sb.append(", extendId=").append(extendId);
        sb.append(", height=").append(getHeight());
        sb.append(", latest=").append(isLatest());
        sb.append('}');
        return sb.toString();
    }
}
