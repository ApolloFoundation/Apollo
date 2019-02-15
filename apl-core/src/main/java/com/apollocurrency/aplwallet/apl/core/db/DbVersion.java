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

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

/**
 * Abstract base classs for running database migration process.
 */
public abstract class DbVersion {
    private static final Logger log = getLogger(DbVersion.class);

    public DbVersion() {
    }

    protected DataSource basicDataSource;

    void init(DataSource dataSource) {
        this.basicDataSource = dataSource;
        Connection con = null;
        Statement stmt = null;
        try {
            con = dataSource.getConnection();
            stmt = con.createStatement();
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid version table");
                }
                rs.close();
                log.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                log.info("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
                con.commit();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            DbUtils.rollback(con);
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(stmt, con);
        }

    }

    protected DbVersion(DataSourceWrapper db) {
        init(db);
    }

    protected void apply(String sql) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = basicDataSource.getConnection();
            stmt = con.createStatement();
            try {
                if (sql != null) {
                    log.debug("Will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                con.commit();
            } catch (Exception e) {
                DbUtils.rollback(con);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        } finally {
            DbUtils.close(stmt, con);
        }
    }

    protected abstract void update(int nextUpdate);

}
