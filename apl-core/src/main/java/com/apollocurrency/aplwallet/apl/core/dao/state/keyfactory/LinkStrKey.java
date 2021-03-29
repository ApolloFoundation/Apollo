/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/**
 * @author al
 */
public final class LinkStrKey implements DbKey {

    private final String idA;
    private final String idB;

    LinkStrKey(String idA, String idB) {
        this.idA = idA;
        this.idB = idB;
    }

    public String[] getId() {
        return new String[]{idA, idB};
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setString(index, idA);
        pstmt.setString(index + 1, idB);
        return index + 2;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkStrKey && ((LinkStrKey) o).idA == idA && ((LinkStrKey) o).idB == idB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idA, idB);
    }


    @Override
    public String toString() {
        return "LinkStrKey{" + "idA=" + idA +
                ", idB=" + idB +
            '}';
    }

}
