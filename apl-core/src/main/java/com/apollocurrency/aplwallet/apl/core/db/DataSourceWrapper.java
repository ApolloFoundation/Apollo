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

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

/**
 * Represent basic implementation of DataSource
 */
public class DataSourceWrapper implements DataSource {
    private static final Logger log = getLogger(DataSourceWrapper.class);
    private static final String DB_INITIALIZATION_ERROR_TEXT = "DatabaseManager was not initialized!";

    @Override
    public Connection getConnection(String username, String password) {
        throw new UnsupportedOperationException("Cannot get connection using different username and password instead of default");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        requireInitialization();
        return dataSource.unwrap(iface);
    }

    private void requireInitialization() {
        if (!initialized) {
            throw new DbException(DB_INITIALIZATION_ERROR_TEXT);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        requireInitialization();
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        requireInitialization();
        return this.dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        requireInitialization();
        this.dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        requireInitialization();
        this.dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        requireInitialization();
        return this.dataSource.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        requireInitialization();
        return this.dataSource.getParentLogger();
    }

    private HikariDataSource dataSource;
    private HikariPoolMXBean jmxBean;
//    private JdbcConnectionPool dataSource;
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

    public DataSourceWrapper(DbProperties dbProperties) {
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
    }

    /**
     * Constructor creates internal DataSource.
     * @param dbVersion database version related information
     */
    public void init(DbVersion dbVersion) {
        log.debug("Database jdbc url set to {} username {}", dbUrl, dbUsername);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(maxConnections);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(loginTimeout));
        dataSource = new HikariDataSource(config);
        jmxBean = dataSource.getHikariPoolMXBean();
/*
        dataSource = JdbcConnectionPool.create(dbUrl, dbUsername, dbPassword);
        dataSource.setMaxConnections(maxConnections);
        dataSource.setLoginTimeout(loginTimeout);
*/
        log.debug("Attempting to create DataSource by path = {}...", dbUrl);
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
            stmt.executeUpdate("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
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
            Connection con = dataSource.getConnection();
            Statement stmt = con.createStatement();
            stmt.execute("SHUTDOWN COMPACT");
            shutdown = true;
            initialized = false;
            dataSource.close();
//            dataSource.dispose();
            log.info("Database shutdown completed");

        } catch (SQLException e) {
            log.info(e.toString(), e);
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void analyzeTables() {
        try (Connection con = dataSource.getConnection();
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
        Connection con = dataSource.getConnection();
        int activeConnections = jmxBean.getActiveConnections();
//        int activeConnections = dataSource.getActiveConnections();
        if (activeConnections > maxActiveConnections) {
            maxActiveConnections = activeConnections;
            log.debug("Used/Maximum connections from Pool '{}'/'{}'",
//                    dataSource.getActiveConnections(), dataSource.getMaxConnections());
                    jmxBean.getActiveConnections(), jmxBean.getTotalConnections());
        }
        return con;
    }

    public String getUrl() {
        return dbUrl;
    }

}
