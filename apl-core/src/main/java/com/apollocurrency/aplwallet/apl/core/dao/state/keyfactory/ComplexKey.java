/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory;

import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public final class ComplexKey implements DbKey {

    private final long idA;
    private final byte[] idB;

    public ComplexKey(long idA, byte[] idB) {
        this.idA = idA;
        this.idB = idB;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index, idA);
        pstmt.setBytes(index + 1, idB);
        return index + 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexKey that = (ComplexKey) o;
        return idA == that.idA && Arrays.equals(idB, that.idB);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(idA);
        result = 31 * result + Arrays.hashCode(idB);
        return result;
    }

    @Override
    public String toString() {
        return "ComplexKey{" + "idA=" + idA + ", idB=" + Convert.toHexString(idB) + '}';
    }
}
