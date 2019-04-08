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

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.enterprise.inject.spi.CDI;

public abstract class VersionedValuesDbTable<T> extends ValuesDbTable<T> {
    private Blockchain blockchain = CDI.current().select(Blockchain.class).get();
    protected VersionedValuesDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true);
    }

    public boolean delete(T t) {
        if (t == null) {
            return false;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT 1 FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < ? LIMIT 1")) {
            int i = dbKey.setPK(pstmtCount);
            int height = blockchain.getHeight();
            pstmtCount.setInt(i, blockchain.getHeight()); // TODO: YL review
            try (ResultSet rs = pstmtCount.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND height = ? AND latest = TRUE")) {
                        int j = dbKey.setPK(pstmt);
                        pstmt.setInt(j, blockchain.getHeight()); // TODO: YL review
                        if (pstmt.executeUpdate() > 0) {
                            return true;
                        }
                    }
                    List<T> values = get(dbKey); // TODO: YL review and fix
                    if (values.isEmpty()) {
                        return false;
                    }
                    for (T v : values) { // TODO: YL review and fix
                        save(con, v);
                    }
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
                        dbKey.setPK(pstmt);
                        if (pstmt.executeUpdate() == 0) {
                            throw new RuntimeException(); // should not happen
                        }
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause())) {
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
