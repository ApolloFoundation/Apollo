/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VersionedEntityDbTableImpl extends EntityDbTable<VersionedDerivedIdEntity> {

    public VersionedEntityDbTableImpl() {
        super("versioned_derived_entity", new VersionedEntityKeyFactory(), true, null, true);
    }

    @Override
    public void save(Connection con, VersionedDerivedIdEntity entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO versioned_derived_entity (id, height, latest) VALUES (?, ?, TRUE)")) {
            pstmt.setLong(1, entity.getId());
            pstmt.setInt(2, entity.getHeight());
            pstmt.executeUpdate();
        }
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
