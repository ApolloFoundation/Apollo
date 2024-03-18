/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class ThreeValuesKeyFactory<T> extends KeyFactory<T> {

    private final String idColumnA;
    private final String idColumnB;
    private final String idColumnC;

    protected ThreeValuesKeyFactory(String idColumnA, String idColumnB, String idColumnC) {
        super(" WHERE" +
                " " + idColumnA + " = ? " +
                "AND " + idColumnB + " = ? " +
                "AND " + idColumnC + " = ? ",
            idColumnA + ", " + idColumnB + ", " + idColumnC,
            " a." + idColumnA + " = b." + idColumnA + " " +
                "AND a." + idColumnB + " = b." + idColumnB + " " +
                "AND a." + idColumnC + " = b." + idColumnC + " "
        );
        this.idColumnA = idColumnA;
        this.idColumnB = idColumnB;
        this.idColumnC = idColumnC;
    }

    @Override
    public DbKey newKey(ResultSet rs) throws SQLException {
        return new ThreeValuesKey(rs.getLong(idColumnA), rs.getString(idColumnB), rs.getBytes(idColumnC));
    }

    public DbKey newKey(long idA, String idB, byte[] idC) {
        return new ThreeValuesKey(idA, idB, idC);
    }
}
