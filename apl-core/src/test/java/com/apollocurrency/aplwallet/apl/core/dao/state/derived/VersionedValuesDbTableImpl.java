/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.model.VersionedChildDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;

import jakarta.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;

public class VersionedValuesDbTableImpl extends VersionedDeletableValuesDbTable<VersionedChildDerivedEntity> {

    public VersionedValuesDbTableImpl(DatabaseManager databaseManager) {
        super("versioned_child_derived_entity",
            new VersionedChildDerivedEntityKeyFactory(),
            databaseManager, mock(Event.class)) ;
    }

    @Override
    protected void save(Connection con, VersionedChildDerivedEntity entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO versioned_child_derived_entity (parent_id, id, height, latest) VALUES (?, ?, ?, TRUE)")) {
            pstmt.setLong(1, entity.getParentId());
            pstmt.setLong(2, entity.getId());
            pstmt.setInt(3, entity.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected VersionedChildDerivedEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long parentId = rs.getLong("parent_id");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        long dbId = rs.getLong("db_id");
        boolean latest = rs.getBoolean("latest");
        return new VersionedChildDerivedEntity(dbId, parentId, id, height, latest);
    }
}
