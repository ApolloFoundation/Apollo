/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DerivedEntityMapper<T extends DerivedEntity> implements RowMapper<T> {
    private KeyFactory<T> keyFactory;

    public DerivedEntityMapper(KeyFactory<T> keyFactory) {
        this.keyFactory = keyFactory;
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        T derivedEntity = doMap(rs, ctx);
        derivedEntity.setDbId(rs.getLong("db_id"));
        derivedEntity.setHeight(rs.getInt("height"));
        derivedEntity.setDbKey(keyFactory.newKey(rs));
        return derivedEntity;
    }

    public abstract T doMap(ResultSet rs, StatementContext ctx) throws SQLException;

}
