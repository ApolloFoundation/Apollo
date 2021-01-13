/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.derived;

import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@ToString(callSuper = true)
public abstract class VersionedDerivedEntity extends DerivedEntity {
    private boolean latest = true;

    public VersionedDerivedEntity(Long dbId, Integer height) {
        super(dbId, height);
    }

    public VersionedDerivedEntity(ResultSet rs) throws SQLException {
        super(rs);
        latest = rs.getBoolean("latest");
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionedDerivedEntity)) return false;
        if (!super.equals(o)) return false;
        VersionedDerivedEntity that = (VersionedDerivedEntity) o;
        return latest == that.latest;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), latest);
    }
}
