/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class ComplexKeyFactory<T> extends KeyFactory<T> {

    private final String idColumnA;
    private final String idColumnB;

    protected ComplexKeyFactory(String idColumnA, String idColumnB) {
        super(" WHERE " + idColumnA + " = ? AND " + idColumnB + " = ? ", idColumnA + ", " + idColumnB, " a." + idColumnA + " = b." + idColumnA + " AND a." + idColumnB + " = b." + idColumnB + " ");
        this.idColumnA = idColumnA;
        this.idColumnB = idColumnB;
    }

    @Override
    public DbKey newKey(ResultSet rs) throws SQLException {
        return new ComplexKey(rs.getLong(idColumnA), rs.getBytes(idColumnB));
    }

    public DbKey newKey(long idA, byte[] idB) {
        return new ComplexKey(idA, idB);
    }
}
