/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author al
 */
public final class LinkKey implements DbKey {
    
    private final long idA;
    private final long idB;

    public LinkKey(long idA, long idB) {
        this.idA = idA;
        this.idB = idB;
    }

    public long[] getId() {
        return new long[]{idA, idB};
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index, idA);
        pstmt.setLong(index + 1, idB);
        return index + 2;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LinkKey && ((LinkKey) o).idA == idA && ((LinkKey) o).idB == idB;
    }

    @Override
    public int hashCode() {
        return (int) (idA ^ (idA >>> 32)) ^ (int) (idB ^ (idB >>> 32));
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LinkKey{");
        sb.append("idA=").append(idA);
        sb.append(", idB=").append(idB);
        sb.append('}');
        return sb.toString();
    }
}
