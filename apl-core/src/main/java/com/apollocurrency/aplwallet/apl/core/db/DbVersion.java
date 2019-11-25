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

import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract base class for running database migration process.
 */
public abstract class DbVersion {
    private static final Logger log = getLogger(DbVersion.class);

    public DbVersion() {
    }

    protected DataSource basicDataSource;

    void init(DataSource dataSource) {
        this.basicDataSource = dataSource;
        int nextUpdate = 1;
        try (
                final Connection con = dataSource.getConnection();
                final Statement stmt = con.createStatement()
        ) {
            @DatabaseSpecificDml(DmlMarker.CHECK_IF_TABLE_EXISTS)
            final ResultSet rsVersionExists = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.tables WHERE lower(table_schema) = 'public' AND lower(table_name) = 'version'"
            );

            if (!rsVersionExists.next() || !rsVersionExists.getBoolean(1)) {
                log.debug("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
            } else {
                final ResultSet rsCurrentVersion = stmt.executeQuery("SELECT next_update FROM version");
                rsCurrentVersion.next();
                nextUpdate = rsCurrentVersion.getInt("next_update");
                if (rsCurrentVersion.next()) {
                    throw new RuntimeException("Invalid version table");
                }
            }

            log.debug("Database update may take a while if needed, current db version {} ...", nextUpdate - 1);
            update(nextUpdate);
        } catch (SQLException e) {
            log.error("Db init/update error", e);
            throw new IllegalStateException(e.toString(), e);
        }
    }

    protected DbVersion(DataSourceWrapper db) {
        init(db);
    }

    protected void apply(String sql) {
        try (
                final Connection con = basicDataSource.getConnection();
                final Statement stmt = con.createStatement();
        ){
                if (sql != null) {
                    log.trace("Will apply sql:\n{}", sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
        } catch (SQLException e) {
            log.error("Apply error for SQL = '" + sql + "'", e);
            throw new RuntimeException("Database error executing: " + sql, e);
        }
    }

    protected abstract int update(int nextUpdate);

}
