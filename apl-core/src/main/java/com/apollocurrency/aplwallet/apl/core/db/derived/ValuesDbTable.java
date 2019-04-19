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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<V> extends DerivedDbTable<V> {

    private final boolean multiversion;
    protected final KeyFactory<V> dbKeyFactory;

    protected ValuesDbTable(String table, KeyFactory<V> dbKeyFactory) {
        this(table, dbKeyFactory, false);
    }

    ValuesDbTable(String table, KeyFactory<V> dbKeyFactory, boolean multiversion) {
        super(table);
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
    }

    public ValuesDbTable(String table, boolean init,  KeyFactory<V> dbKeyFactory) {
        super(table, init);
        this.multiversion = false;
        this.dbKeyFactory = dbKeyFactory;
    }

//    protected abstract T load(Connection con, ResultSet rs) throws SQLException;

//    protected abstract void save(Connection con, T t, V v) throws SQLException;

    protected void clearCache() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        dataSource.clearCache(table);
    }

    public final List<V> get(DbKey dbKey) {
        List<V> values;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (dataSource.isInTransaction()) {
            values = (List<V>) dataSource.getCache(table).get(dbKey);
            if (values != null) {
                return values;
            }
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
                     + (multiversion ? " AND latest = TRUE" : "") + " ORDER BY db_id")) {
            dbKey.setPK(pstmt);
            values = get(con, pstmt);
            if (dataSource.isInTransaction()) {
                dataSource.getCache(table).put(dbKey, values);
            }
            return values;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private List<V> get(Connection con, PreparedStatement pstmt) {
        try {
            List<V> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(load(con, rs, dbKeyFactory.newKey(rs)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(List<V> values) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(values.get(0)); // TODO: YL review and fix
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
        checkKeys(dbKey, values);
        dataSource.getCache(table).put(dbKey, values);
        try (Connection con = dataSource.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            for (V v : values) {
//                save(con, t, v);
                save(con, v); // TODO: YL review and fix
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public abstract void save(Connection con, V entity) throws SQLException;

    private void checkKeys(DbKey key, List<V> values) {

        boolean match = values
                .stream()
                .map(dbKeyFactory::newKey)
                .allMatch(key::equals);
        if (!match) {
            throw new IllegalArgumentException("DbKeys not match");
        }
    }

    @Override
    public final void rollback(int height) {
        if (multiversion) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            VersionedEntityDbTable.rollback(dataSource, table, height, dbKeyFactory);
        } else {
            super.rollback(height);
        }
    }

    @Override
    public final void trim(int height, TransactionalDataSource dataSource) {
        if (multiversion) {
            if (dataSource == null) {
                dataSource = databaseManager.getDataSource();
            }
            VersionedEntityDbTable.trim(dataSource, table, height, dbKeyFactory);
        }
        // nothing to do here
/*
        else {
            super.trim(height, dataSource);
        }
*/
    }

}
