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

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.DerivedEntity;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class ValuesDbTable<T extends DerivedEntity> extends BasicDbTable<T> {

    public ValuesDbTable(String table, KeyFactory<T> dbKeyFactory, boolean multiversion,
                         DatabaseManager databaseManager,
                         Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(table, dbKeyFactory, multiversion, databaseManager,
                deleteOnTrimDataEvent, null);
    }

    public final List<T> get(DbKey dbKey) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + keyFactory.getPKClause()
                 + (multiversion ? " AND latest = TRUE" : "") + " ORDER BY db_id")) {
            dbKey.setPK(pstmt);
            return get(con, pstmt);
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
