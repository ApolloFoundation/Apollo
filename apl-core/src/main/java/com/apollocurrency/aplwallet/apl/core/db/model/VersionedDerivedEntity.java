/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import java.sql.ResultSet;
import java.sql.SQLException;

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
}
