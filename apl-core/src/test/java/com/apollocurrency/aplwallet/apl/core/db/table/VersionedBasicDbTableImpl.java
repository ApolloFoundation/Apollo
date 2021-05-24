/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.BasicDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;

public class VersionedBasicDbTableImpl extends BasicDbTable<VersionedDerivedIdEntity> {

    public VersionedBasicDbTableImpl(DatabaseManager databaseManager) {
        super("versioned_derived_entity", new VersionedEntityKeyFactory(), true, databaseManager, mock(Event.class), null);
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
