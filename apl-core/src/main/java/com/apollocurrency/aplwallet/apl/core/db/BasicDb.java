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

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Represent basic implementation of DataSource
 * can be initialized with optional fulltext support by using {@link FullTextSearchService} in constructor
 */
public class BasicDb implements DataSource {
    private static final Logger log = getLogger(BasicDb.class);
    private static final String DB_INITIALIZATION_ERROR_TEXT = "Db was not initialized!";

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
            throw new DbException(DB_INITIALIZATION_ERROR_TEXT);
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
    private volatile boolean shutdown = false;
    private FullTextSearchService fullTextSearchProvider;
    public BasicDb(DbProperties dbProperties, FullTextSearchService fullTextSearchProvider) {
        long maxCacheSize = dbProperties.getMaxCacheSize();
        if (maxCacheSize == 0) {
            maxCacheSize = Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128)/2)) * 1024;
        }
        String dbUrl = dbProperties.getDbUrl();
        if (StringUtils.isBlank(dbUrl)) {
            String dbFileName = dbProperties.getDbFileName();
            dbUrl = String.format("jdbc:%s:%s;%s", dbProperties.getDbType(), dbProperties.getDbDir() + "/" + dbFileName, dbProperties.getDbParams());
        }
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        if (!dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        this.dbUrl = dbUrl;
        this.dbUsername = dbProperties.getDbUsername();
        this.dbPassword = dbProperties.getDbPassword();
        this.maxConnections = dbProperties.getMaxConnections();
        this.loginTimeout = dbProperties.getLoginTimeout();
        this.defaultLockTimeout = dbProperties.getDefaultLockTimeout();
        this.maxMemoryRows = dbProperties.getMaxMemoryRows();
        this.fullTextSearchProvider = fullTextSearchProvider;
    }

    public BasicDb(DbProperties dbProperties) {
        this(dbProperties, null);
    }

    /**
     * Constructor creates internal DataSource with optional 'Full Text Search' indexes.
     * @param dbVersion database version related information
     */
    public void init(DbVersion dbVersion) {
        log.debug("Database jdbc url set to {} username {}", dbUrl, dbUsername);
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
        if (fullTextSearchProvider != null) {
            fullTextSearchProvider.init();
        }
        dbVersion.init(this);
        initialized = true;
        shutdown = false;
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }
        try {
            if (fullTextSearchProvider != null) {
                fullTextSearchProvider.shutdown();
            }
            Connection con = cp.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN COMPACT");
            log.info("Database shutdown completed");
            cp.dispose();
            shutdown = true;
            initialized = false;
        } catch (SQLException e) {
            log.info(e.toString(), e);
        }
    }

    public boolean isShutdown() {
        return shutdown;
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
            log.debug("Database connection pool current size: " + activeConnections);
        }
        return con;
    }

    public String getUrl() {
        return dbUrl;
    }

}
