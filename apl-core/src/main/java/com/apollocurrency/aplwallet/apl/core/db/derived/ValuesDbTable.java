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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<T> extends BasicDbTable<T> {
        private static final Logger log = LoggerFactory.getLogger(ValuesDbTable.class);


    public ValuesDbTable(String table, KeyFactory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false);
    }

    public ValuesDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion) {
        super(table, dbKeyFactory, multiversion, true);
    }

    public ValuesDbTable(String table, boolean init,  KeyFactory<T> dbKeyFactory, boolean multiversion) {
        super(table, dbKeyFactory, multiversion, init);
    }

    public final List<T> get(DbKey dbKey) {
        List<T> values;
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (dataSource.isInTransaction()) {
            values = (List<T>) dataSource.getCache(table).get(dbKey);
            if (values != null) {
                return values;
            }
        }
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + keyFactory.getPKClause()
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

    private List<T> get(Connection con, PreparedStatement pstmt) {
        try {
            List<T> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    T loadedValue = load(con, rs, keyFactory.newKey(rs));
                    if (loadedValue != null) {
                        result.add(loadedValue);
                    } else {
                        log.debug("Loaded null value from {}. Skipping it", getTableName());
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(List<T> values) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (!dataSource.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = keyFactory.newKey(values.get(0));
        if (dbKey == null) {
            throw new RuntimeException("DbKey not set");
        }
        checkKeys(dbKey, values);
        dataSource.getCache(table).put(dbKey, values);
        try (Connection con = dataSource.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + keyFactory.getPKClause() + " AND latest = TRUE")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            for (T v : values) {
                save(con, v);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected abstract void save(Connection con, T entity) throws SQLException;

    private void checkKeys(DbKey key, List<T> values) {

        boolean match = values
                .stream()
                .map(keyFactory::newKey)
                .allMatch(key::equals);
        if (!match) {
            throw new IllegalArgumentException("DbKeys not match");
        }
    }
}
