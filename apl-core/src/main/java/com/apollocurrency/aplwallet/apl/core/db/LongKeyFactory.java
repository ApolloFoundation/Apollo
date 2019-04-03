/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public abstract class LongKeyFactory<T> extends KeyFactory<T> {
    
    private final String idColumn;

    public LongKeyFactory(String idColumn) {
        super(" WHERE " + idColumn + " = ? ", idColumn, " a." + idColumn + " = b." + idColumn + " ");
        this.idColumn = idColumn;
    }

    @Override
    public DbKey newKey(ResultSet rs) throws SQLException {
        return new LongKey(rs.getLong(idColumn));
    }

    public DbKey newKey(long id) {
        return new LongKey(id);
    }
    
}
