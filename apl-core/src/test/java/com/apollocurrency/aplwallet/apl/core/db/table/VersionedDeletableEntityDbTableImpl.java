/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;

public class VersionedDeletableEntityDbTableImpl extends VersionedDeletableEntityDbTable<VersionedDerivedIdEntity> {

    public VersionedDeletableEntityDbTableImpl(DatabaseManager databaseManager) {
        super("versioned_derived_entity", new VersionedEntityKeyFactory(), null,databaseManager, mock(Event.class));
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
