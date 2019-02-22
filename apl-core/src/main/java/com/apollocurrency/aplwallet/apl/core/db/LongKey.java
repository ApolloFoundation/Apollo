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
public final class LongKey implements DbKey {
    
    private final long id;

    public LongKey(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setLong(index, id);
        return index + 1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LongKey && ((LongKey) o).id == id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LongKey{");
        sb.append("id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
