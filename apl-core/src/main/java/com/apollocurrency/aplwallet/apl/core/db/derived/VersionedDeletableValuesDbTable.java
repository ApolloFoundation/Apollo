/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class VersionedDeletableValuesDbTable<T extends VersionedDerivedEntity> extends ValuesDbTable<T> {
    protected VersionedDeletableValuesDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    public VersionedDeletableValuesDbTable(String table, boolean init, KeyFactory<T> dbKeyFactory) {
        super(table, init, dbKeyFactory, true);
    }

    public boolean delete(T t, int height) {

        if (t == null) {
            return false;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = getDbKeyFactory().newKey(t);
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT 1 FROM " + table + getDbKeyFactory().getPKClause()
                     + " AND height < ? LIMIT 1")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, height);
            try (ResultSet rs = pstmtCount.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + getDbKeyFactory().getPKClause() + " AND height = ? AND latest = TRUE")) {
                        int j = dbKey.setPK(pstmt);
                        pstmt.setInt(j, height);
                        if (pstmt.executeUpdate() > 0) {
                            return true;
                        }
                    }
                    List<T> values = get(dbKey);
                    if (values.isEmpty()) {
                        return false;
                    }
                    for (T v : values) {
                        v.setHeight(height);
                        save(con, v);
                    }
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + getDbKeyFactory().getPKClause() + " AND latest = TRUE")) {
                        dbKey.setPK(pstmt);
                        if (pstmt.executeUpdate() == 0) {
                            throw new RuntimeException(); // should not happen
                        }
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + getDbKeyFactory().getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            dataSource.getCache(table).remove(dbKey);
        }
    }
    
}
