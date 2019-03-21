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

package com.apollocurrency.aplwallet.apl.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<T,V> extends DerivedDbTable {

    private final boolean multiversion;
    protected final KeyFactory<T> dbKeyFactory;

    protected ValuesDbTable(String table, KeyFactory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false);
    }

    ValuesDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion) {
        super(table);
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
    }

    protected abstract V load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t, V v) throws SQLException;

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
                    result.add(load(con, rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(T t, List<V> values) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
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
                save(con, t, v);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
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
        } else {
            super.trim(height, dataSource);
        }
    }

    @Override
    public void createSearchIndex(Connection con) throws SQLException {
        super.createSearchIndex(con);
    }
}
