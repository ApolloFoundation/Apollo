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
public abstract class LinkKeyFactory<T> extends KeyFactory<T> {
    
    private final String idColumnA;
    private final String idColumnB;

    public LinkKeyFactory(String idColumnA, String idColumnB) {
        super(" WHERE " + idColumnA + " = ? AND " + idColumnB + " = ? ", idColumnA + ", " + idColumnB, " a." + idColumnA + " = b." + idColumnA + " AND a." + idColumnB + " = b." + idColumnB + " ");
        this.idColumnA = idColumnA;
        this.idColumnB = idColumnB;
    }

    @Override
    public DbKey newKey(ResultSet rs) throws SQLException {
        return new LinkKey(rs.getLong(idColumnA), rs.getLong(idColumnB));
    }

    public DbKey newKey(long idA, long idB) {
        return new LinkKey(idA, idB);
    }
    
}
