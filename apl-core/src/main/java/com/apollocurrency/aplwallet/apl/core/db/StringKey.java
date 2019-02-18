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
public final class StringKey implements DbKey {
    
    private final String id;

    StringKey(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
        return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
        pstmt.setString(index, id);
        return index + 1;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StringKey && (id != null ? id.equals(((StringKey) o).id) : ((StringKey) o).id == null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("StringKey{");
        sb.append("id='").append(id).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
