/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.MigrationParams;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.NonNull;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represent basic implementation of DataSource
 */
public class DataSourceWrapper implements DataSource {
    private static final Logger log = getLogger(DataSourceWrapper.class);
    private static final String DB_INITIALIZATION_ERROR_TEXT = "DatabaseManager was not initialized!";

    private HikariDataSource dataSource;
    private HikariPoolMXBean jmxBean;
    protected final DbProperties dbProperties;

    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;


    public DataSourceWrapper(@NonNull DbProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

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
    public int getLoginTimeout() throws SQLException {
        requireInitialization();
        return this.dataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        requireInitialization();
        this.dataSource.setLoginTimeout(seconds);
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        requireInitialization();
        return this.dataSource.getParentLogger();
    }

    public HikariPoolMXBean getJmxBean() {
        return jmxBean;
    }


    private void setInitialzed() {
        initialized = true;
        shutdown = false;
    }

    private void initDatasource() {
        HikariConfig config = new HikariConfig();
        String dbUrl = dbProperties.getDbUrl();
        if (StringUtils.isBlank(dbUrl)) {
            throw new IllegalStateException("Db url was not assigned in db properties, possibly datasource creation flow is violated : " + dbProperties);
        }
        log.debug("Database jdbc url set to {} username {}", dbUrl, dbProperties.getDbUsername());
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbProperties.getDbUsername());
        String dbPassword = dbProperties.getDbPassword();
        if (StringUtils.isNotBlank(dbPassword)) {
            config.setPassword(dbPassword);
        }
        config.setMaximumPoolSize(dbProperties.getMaxConnections());
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(dbProperties.getLoginTimeout()));
        config.setLeakDetectionThreshold(60_000 * 5); // 5 minutes
        config.setIdleTimeout(60_000 * 20); // 20 minutes in milliseconds
        config.setPoolName(dbProperties.getDbName());
        log.debug("Creating DataSource pool '{}', path = {}", dbProperties.getDbName(), dbProperties.getDbUrl());
        dataSource = new HikariDataSource(config);
        jmxBean = dataSource.getHikariPoolMXBean();
    }

    public void init() {
        initDatasource();
        setInitialzed();
    }

    public void update(DBUpdater dbUpdater) {
        dbUpdater.update(new MigrationParams(dbProperties.getDbUrl(), dbProperties.getDbType(), dbProperties.getDbUsername(), dbProperties.getDbPassword()));
    }

    public void shutdown() {
        long start = System.currentTimeMillis();
        if (!initialized) {
            return;
        }
        shutdown = true;
        initialized = false;
        dataSource.close();
        log.debug("Db shutdown completed in {} ms for '{}'", System.currentTimeMillis() - start, dbProperties.getDbUrl());
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = getPooledConnection();
        con.setAutoCommit(true);
        return con;
    }

    protected Connection getPooledConnection() throws SQLException {
        Connection con = dataSource.getConnection();
        if (jmxBean != null) {
            if (log.isDebugEnabled()) {
                int totalConnections = jmxBean.getTotalConnections();
                int idleConnections = jmxBean.getIdleConnections();

                if (idleConnections <= totalConnections * 0.1) {
                    int activeConnections = jmxBean.getActiveConnections();
                    int threadAwaitingConnections = jmxBean.getThreadsAwaitingConnection();
                    log.debug("Total/Active/Idle connections in Pool '{}'/'{}'/'{}', threadsAwaitPool=[{}], {} Thread: {}",
                        totalConnections,
                        activeConnections,
                        idleConnections,
                        threadAwaitingConnections,
                        dataSource.getPoolName(), // show main or shard db
                        Thread.currentThread().getName());
                }
            }
        }
        return con;
    }

    public String getUrl() {
        return dbProperties.getDbUrl();
    }

    // Use the original datasource directly only in special cases
    public DataSource original() {
        return dataSource;
    }

    @Override
    public String toString() {
        return "DataSourceWrapper{" +
            "dbUrl='" + dbProperties.getDbUrl() + '\'' +
            ", initialized=" + initialized +
            ", shutdown=" + shutdown +
            '}';
    }
}
