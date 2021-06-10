/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;

public class EntityDbTableImpl extends EntityDbTable<DerivedIdEntity> {
    public EntityDbTableImpl(DatabaseManager databaseManager) {
        super("derived_entity", new DerivedEntityKeyFactory(), false, null, databaseManager, mock(Event.class));
    }

    @Override
    public void save(Connection con, DerivedIdEntity entity) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO derived_entity (id, height) VALUES (?, ?)")) {
            pstmt.setLong(1, entity.getId());
            pstmt.setInt(2, entity.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    protected DerivedIdEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long dbId = rs.getLong("db_id");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        return new DerivedIdEntity(dbId, height, id);
    }
}
