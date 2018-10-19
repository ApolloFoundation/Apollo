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

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import static org.slf4j.LoggerFactory.getLogger;

public class BasicDb implements DataSource {
    private static final Logger LOG = getLogger(BasicDb.class);
    private static final String DB_INITIALIZATION_ERROR_TEXT = "Db was not initialized!";
    private static final DbException DB_NOT_INITIALIZED_EXCEPTION = new DbException(DB_INITIALIZATION_ERROR_TEXT);

    @Override
    public Connection getConnection(String username, String password) {
        throw new UnsupportedOperationException("Cannot get connection using different username and password instead of default");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        requireInitialization();
        return cp.unwrap(iface);
    }

    private void requireInitialization() {
        if (!initialized) {
            throw DB_NOT_INITIALIZED_EXCEPTION;
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        requireInitialization();
        return cp.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() {
        requireInitialization();
        return this.cp.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        requireInitialization();
        this.cp.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) {
        requireInitialization();
        this.cp.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        requireInitialization();
        return this.cp.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        requireInitialization();
        return this.cp.getParentLogger();
    }

    public static final class DbProperties {

        private long maxCacheSize;
        private String dbUrl;
        private String dbType;
        private String dbDir;
        private String dbParams;
        private String dbUsername;
        private String dbPassword;
        private int maxConnections;
        private int loginTimeout;
        private int defaultLockTimeout;
        private int maxMemoryRows;

        public DbProperties maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        public DbProperties dbUrl(String dbUrl) {
            this.dbUrl = dbUrl;
            return this;
        }

        public DbProperties dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public DbProperties dbDir(String dbDir) {
            this.dbDir = dbDir;
            return this;
        }

        public DbProperties dbParams(String dbParams) {
            this.dbParams = dbParams;
            return this;
        }

        public DbProperties dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }

        public DbProperties dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public DbProperties maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public DbProperties loginTimeout(int loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        public DbProperties defaultLockTimeout(int defaultLockTimeout) {
            this.defaultLockTimeout = defaultLockTimeout;
            return this;
        }

        public DbProperties maxMemoryRows(int maxMemoryRows) {
            this.maxMemoryRows = maxMemoryRows;
            return this;
        }

        @Override
        public String toString() {
            return "DbProperties{" +
                    "maxCacheSize=" + maxCacheSize +
                    ", dbUrl='" + dbUrl + '\'' +
                    ", dbType='" + dbType + '\'' +
                    ", dbDir='" + dbDir + '\'' +
                    ", dbParams='" + dbParams + '\'' +
                    ", dbUsername='" + dbUsername + '\'' +
                    ", dbPassword='" + dbPassword + '\'' +
                    ", maxConnections=" + maxConnections +
                    ", loginTimeout=" + loginTimeout +
                    ", defaultLockTimeout=" + defaultLockTimeout +
                    ", maxMemoryRows=" + maxMemoryRows +
                    '}';
        }
    }

    private JdbcConnectionPool cp;
    private volatile int maxActiveConnections;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final int maxConnections;
    private final int loginTimeout;
    private final int defaultLockTimeout;
    private final int maxMemoryRows;
    private volatile boolean initialized = false;

    public BasicDb(DbProperties dbProperties) {
        long maxCacheSize = dbProperties.maxCacheSize;
        if (maxCacheSize == 0) {
            maxCacheSize = Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128)/2)) * 1024;
        }
        String dbUrl = dbProperties.dbUrl;
        if (dbUrl == null) {
            String dbDir = Apl.getDbDir(dbProperties.dbDir);
            dbUrl = String.format("jdbc:%s:%s;%s", dbProperties.dbType, dbDir, dbProperties.dbParams);
        }
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        if (!dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        this.dbUrl = dbUrl;
        this.dbUsername = dbProperties.dbUsername;
        this.dbPassword = dbProperties.dbPassword;
        this.maxConnections = dbProperties.maxConnections;
        this.loginTimeout = dbProperties.loginTimeout;
        this.defaultLockTimeout = dbProperties.defaultLockTimeout;
        this.maxMemoryRows = dbProperties.maxMemoryRows;
    }

    public void init(DbVersion dbVersion) {
        LOG.debug("Database jdbc url set to {} username {}", dbUrl, dbUsername);
        FullTextTrigger.setActive(true);
        cp = JdbcConnectionPool.create(dbUrl, dbUsername, dbPassword);
        cp.setMaxConnections(maxConnections);
        cp.setLoginTimeout(loginTimeout);
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
            stmt.executeUpdate("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        dbVersion.init(this);
        initialized = true;
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }
        try {
            FullTextTrigger.setActive(false);
            Connection con = cp.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN COMPACT");
            LOG.info("Database shutdown completed");
        } catch (SQLException e) {
            LOG.info(e.toString(), e);
        }
    }

    public void analyzeTables() {
        try (Connection con = cp.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    @Override
    public Connection getConnection() throws SQLException {
        Connection con = getPooledConnection();
        con.setAutoCommit(true);
        return con;
    }

    protected Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            LOG.debug("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

    public String getUrl() {
        return dbUrl;
    }

}
