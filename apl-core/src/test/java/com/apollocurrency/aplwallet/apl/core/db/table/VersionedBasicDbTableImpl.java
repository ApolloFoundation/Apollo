/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.BasicDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VersionedBasicDbTableImpl extends BasicDbTable<VersionedDerivedIdEntity> {

    public VersionedBasicDbTableImpl() {
        super("versioned_derived_entity", new VersionedEntityKeyFactory(), true, true);
    }

    @Override
    protected VersionedDerivedIdEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long dbId = rs.getLong("db_id");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        boolean latest = rs.getBoolean("latest");
        return new VersionedDerivedIdEntity(dbId, height, id, latest);
    }
}
