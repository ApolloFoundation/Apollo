/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.table;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.model.DerivedIdEntity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DerivedDbTableImpl extends DerivedDbTable<DerivedIdEntity> {
    public DerivedDbTableImpl() {
        super("derived_entity", true);
    }

    @Override
    protected DerivedIdEntity load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        long dbId = rs.getLong("db_id");
        long id = rs.getLong("id");
        int height = rs.getInt("height");
        return new DerivedIdEntity(dbId, height, id);

    }
}
