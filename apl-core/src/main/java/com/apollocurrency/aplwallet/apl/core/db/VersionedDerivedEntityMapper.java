/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.converter.db.DerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class VersionedDerivedEntityMapper<T extends VersionedDerivedEntity> extends DerivedEntityMapper<T> {
    public VersionedDerivedEntityMapper(KeyFactory<T> keyFactory) {
        super(keyFactory);
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        T obj = super.map(rs, ctx);
        obj.setLatest(rs.getBoolean("latest"));
        return obj;
    }
}
