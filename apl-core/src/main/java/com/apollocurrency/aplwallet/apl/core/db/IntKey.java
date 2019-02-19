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
public final class IntKey implements DbKey {
    
    private final Integer id;

    private IntKey(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setInt(index, id);
        return index + 1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IntKey && (id != null ? id.equals(((IntKey) o).id) : ((IntKey) o).id == null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("IntKey{");
        sb.append("id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
