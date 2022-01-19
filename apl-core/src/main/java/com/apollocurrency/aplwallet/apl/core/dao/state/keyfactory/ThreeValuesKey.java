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
public final class ThreeValuesKey implements DbKey {

    private final long idA;
    private final String idB;
    private final byte[] idC;

    public ThreeValuesKey(long idA, String idB, byte[] idC) {
        this.idA = idA;
        this.idB = idB;
        this.idC = idC;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index, idA);
        pstmt.setString(index + 1, idB);
        pstmt.setBytes(index + 2, idC);
        return index + 3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreeValuesKey that = (ThreeValuesKey) o;
        return idA == that.idA && idB.equals(that.idB) && Arrays.equals(idC, that.idC);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(idA);
        result = 31 * result + idB.hashCode();
        result = 31 * result + Arrays.hashCode(idC);
        return result;
    }

    @Override
    public String toString() {
        return "ThreeValuesKey{" + "idA=" + idA + ", idB=" + idB + ", idC=" + Convert.toHexString(idC) + '}';

    }
}
