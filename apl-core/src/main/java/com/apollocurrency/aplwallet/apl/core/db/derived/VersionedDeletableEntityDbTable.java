/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;


import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class VersionedDeletableEntityDbTable<T> extends EntityDbTable<T> {
    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    public VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns, boolean init) {
        super(table, dbKeyFactory, true, fullTextSearchColumns, init);
    }

    public VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean init) {
        super(table, dbKeyFactory, true, null, init);

    }

    @Override
    public boolean supportDelete() {
        return true;
    }

    @Override
    public boolean deleteAtHeight(T t, int height) {
        return delete(t, height);
    }

    public boolean delete(T t, int height) {
        if (t == null) {
            return false;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }

        KeyFactory<T> keyFactory = getDbKeyFactory();
        DbKey dbKey = keyFactory.newKey(t);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT db_id FROM " + table
                 + keyFactory.getPKClause() + " AND height < ? ORDER BY db_id DESC LIMIT 1");
        ) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, height);
            try (ResultSet rs = pstmtCount.executeQuery()) {
                if (rs.next()) {
                    long dbId = rs.getLong(1);
                    try (
                        @DatabaseSpecificDml(DmlMarker.UPDATE_WITH_LIMIT)
                        PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE, deleted = TRUE " + keyFactory.getPKClause() + " AND latest = TRUE LIMIT 1");
                        PreparedStatement updatePrevPstmt = con.prepareStatement("UPDATE " + table + " SET latest = FALSE, deleted = TRUE WHERE db_id = ?")
                    ) {
                        updatePrevPstmt.setLong(1, dbId);
                        updatePrevPstmt.executeUpdate();
                        dbKey.setPK(pstmt);
                        save(con, t);
                        pstmt.executeUpdate();// delete after the save
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + keyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
