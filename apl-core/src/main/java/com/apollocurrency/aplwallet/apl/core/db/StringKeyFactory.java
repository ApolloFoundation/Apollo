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
public abstract class StringKeyFactory<T> extends KeyFactory<T> {
    
    private final String idColumn;

    public StringKeyFactory(String idColumn) {
        super(" WHERE " + idColumn + " = ? ", idColumn, " a." + idColumn + " = b." + idColumn + " ");
        this.idColumn = idColumn;
    }

    @Override
    public DbKey newKey(ResultSet rs) throws SQLException {
        return new StringKey(rs.getString(idColumn));
    }

    public DbKey newKey(String id) {
        return new StringKey(id);
    }
    
}
