/*
 *  Copyright © 2018-2019 Apollo Foundation
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
import javax.enterprise.inject.spi.CDI;

public abstract class VersionedDeletableEntityDbTable<T> extends EntityDbTable<T> {
    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    public VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, String fullTextSearchColumns, boolean init) {
        super(table, dbKeyFactory,true, fullTextSearchColumns, init);
    }

    public VersionedDeletableEntityDbTable(String table, KeyFactory<T> dbKeyFactory, boolean init) {
        super(table, dbKeyFactory, true, null, init);

    }

    @Override
    public boolean delete(T t) { //TODO remove blockchain
        Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
        return delete(t, false, blockchain.getHeight());
    }

    public final boolean delete(T t, boolean keepInCache, int height) {
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
             PreparedStatement pstmtCount = con.prepareStatement("SELECT 1 FROM " + table
                     + keyFactory.getPKClause() + " AND height < ? LIMIT 1")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, height);
            try (ResultSet rs = pstmtCount.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + keyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                        dbKey.setPK(pstmt);
                        pstmt.executeUpdate();
                        save(con, t);
                        pstmt.executeUpdate(); // delete after the save
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + keyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
