/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public abstract class VersionedDerivedEntity extends DerivedEntity {
    private boolean latest = true;

    public VersionedDerivedEntity(Long dbId, Integer height) {
        super(dbId, height);
    }

    public VersionedDerivedEntity(ResultSet rs) throws SQLException {
        super(rs);
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

    @Override
    public VersionedDerivedEntity clone() throws CloneNotSupportedException {
        VersionedDerivedEntity clone = (VersionedDerivedEntity) super.clone();
        clone.setLatest(latest);
        return clone;
    }
}
