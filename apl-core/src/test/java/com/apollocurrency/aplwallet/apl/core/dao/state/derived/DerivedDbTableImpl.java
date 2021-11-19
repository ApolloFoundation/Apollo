/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import javax.enterprise.event.Event;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.model.DerivedIdEntity;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DerivedDbTableImpl extends DerivedDbTable<DerivedIdEntity> {
    public DerivedDbTableImpl(DatabaseManager databaseManager, Event<FullTextOperationData> fullTextOperationDataEvent) {
        super("derived_entity",databaseManager, fullTextOperationDataEvent, null);
    }

    @Override
    protected DerivedIdEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long dbId = rs.getLong("db_id");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        return new DerivedIdEntity(dbId, height, id);

    }
}
