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

import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.BigIntegerArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.DexCurrenciesFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.LongArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.OrderStatusFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.OrderTypeFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.ShardStateFactory;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.exception.DbException;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.jdbi.v3.core.ConnectionException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represent basic implementation of DataSource
 */
public class DataSourceWrapper implements DataSource {
    private static final Logger log = getLogger(DataSourceWrapper.class);
    private static final String DB_INITIALIZATION_ERROR_TEXT = "DatabaseManager was not initialized!";
    private static Pattern patternExtractShardNumber = Pattern.compile("shard-\\d+");
    //    private JdbcConnectionPool dataSource;
//    private volatile int maxActiveConnections;
    private final String dbUrl;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final String systemDbUrl;
    private final int maxConnections;
    private final int loginTimeout;
    private String shardId = "main-db";
    private HikariDataSource dataSource;
    private HikariPoolMXBean jmxBean;
    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;
    private DataSource systemDateSource;


    public DataSourceWrapper(DbProperties dbProperties) {
        //Even though dbUrl is no longer coming from apl-blockchain.properties,
        //DbMigrationExecutor in afterMigration triggers the further creation of DataSourceWrapper
        String dbUrlTemp = dbProperties.getDbUrl();
        this.dbName = dbProperties.getDbName();

        if (StringUtils.isBlank(dbUrlTemp)) {
            Matcher m = patternExtractShardNumber.matcher(dbName); // try to match shard name
            if (m.find()) {
                shardId = m.group(); // store shard id
            }
            dbUrlTemp = String.format(
                "jdbc:%s://%s:%d/%s?user=%s&password=%s",
                dbProperties.getDbType(),
                dbProperties.getDatabaseHost(),
                dbProperties.getDatabasePort(),
                dbProperties.getDbName(),
                dbProperties.getDbUsername(),
                dbProperties.getDbPassword()
            );
            dbProperties.setDbUrl(dbUrlTemp);
        }

        if (StringUtils.isBlank(dbProperties.getSystemDbUrl())) {
            String sysDbUrl = String.format(
                "jdbc:%s://%s:%d/%s?user=%s&password=%s",
                dbProperties.getDbType(),
                dbProperties.getDatabaseHost(),
                dbProperties.getDatabasePort(),
                DbProperties.DB_SYSTEM_NAME,
                dbProperties.getDbUsername(),
                dbProperties.getDbPassword()
            );
            dbProperties.setSystemDbUrl(sysDbUrl);
        }

        this.dbUrl = dbUrlTemp;
        this.dbUsername = dbProperties.getDbUsername();
        this.dbPassword = dbProperties.getDbPassword();
        this.maxConnections = dbProperties.getMaxConnections();
        this.loginTimeout = dbProperties.getLoginTimeout();
        this.systemDbUrl = dbProperties.getSystemDbUrl();
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

    /**
     * Constructor creates internal DataSource.
     *
     * @param dbVersion database version related information
     */
    public Jdbi initWithJdbi(DbVersion dbVersion) {
        initDatasource(dbVersion);
        Jdbi jdbi = initJdbi();
        setInitialzed();
        return jdbi;
    }

    private void setInitialzed() {
        initialized = true;
        shutdown = false;
    }

    private void initDatasource(DbVersion dbVersion) {
        log.debug("Database jdbc url set to {} username {}", dbUrl, dbUsername);
        if (this.systemDateSource == null) {
            HikariConfig sysDBConf = new HikariConfig();
            sysDBConf.setJdbcUrl(systemDbUrl);
            sysDBConf.setUsername(dbUsername);
            sysDBConf.setPassword(dbPassword);
            sysDBConf.setMaximumPoolSize(20);
            sysDBConf.setPoolName("systemDB");
            this.systemDateSource = new HikariDataSource(sysDBConf);
        }

        try (Connection con = this.systemDateSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(
                String.format(
                    "CREATE DATABASE IF NOT EXISTS %1$s CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
                    dbName)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(maxConnections);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(loginTimeout));
        config.setLeakDetectionThreshold(60_000 * 5); // 5 minutes
        config.setIdleTimeout(60_000 * 20); // 20 minutes in milliseconds
        config.setPoolName(shardId);
        log.debug("Creating DataSource pool '{}', path = {}", shardId, dbUrl);
        dataSource = new HikariDataSource(config);
        jmxBean = dataSource.getHikariPoolMXBean();

        log.debug("Attempting to create DataSource by path = {}...", dbUrl);
        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement()) {

//            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
//            stmt.executeUpdate("SET MAX_MEMORY_ROWS " + maxMemoryRows);

            stmt.executeUpdate("set global rocksdb_max_row_locks=1073741824");
            stmt.executeUpdate("set session rocksdb_max_row_locks=1073741824");

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        log.debug("Before starting Db schema init {}...", dbVersion);
        dbVersion.init(this);
    }

    private Jdbi initJdbi() {
        log.debug("Attempting to create Jdbi instance...");
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerArgument(new BigIntegerArgumentFactory());
        jdbi.registerArgument(new DexCurrenciesFactory());
        jdbi.registerArgument(new OrderTypeFactory());
        jdbi.registerArgument(new OrderStatusFactory());
        jdbi.registerArgument(new LongArrayArgumentFactory());
        jdbi.registerArrayType(long.class, "generatorIds");
        jdbi.registerArgument(new ShardStateFactory());

        log.debug("Attempting to open Jdbi handler to database..");
        try (Handle handle = jdbi.open()) {
            @DatabaseSpecificDml(DmlMarker.DUAL_TABLE_USE)
            Optional<Integer> result = handle.createQuery("select 1 from dual;")
                .mapTo(Integer.class).findOne();
            log.debug("check SQL result ? = {}", result);
        } catch (ConnectionException e) {
            log.error("Error on opening database connection", e);
            throw e;
        }
        return jdbi;
    }

    public void init(DbVersion dbVersion) {
        initDatasource(dbVersion);
        setInitialzed();
    }

    public void update(DbVersion dbVersion) {
        dbVersion.init(this);
    }

    public void shutdown() {
        long start = System.currentTimeMillis();
        if (!initialized) {
            return;
        }
        try {
            Connection con = dataSource.getConnection();
            Statement stmt = con.createStatement();
//            stmt.execute("SHUTDOWN");
            shutdown = true;
            initialized = false;
            dataSource.close();
//            dataSource.dispose();
            log.debug("Db shutdown completed in {} ms for '{}'", System.currentTimeMillis() - start, this.dbUrl);
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
            log.debug("Start DB 'ANALYZE' on {}", con.getMetaData());
            stmt.execute("ANALYZE");
            log.debug("FINISHED DB 'ANALYZE' on {}", con.getMetaData());
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
        if (jmxBean != null) {
            if (log.isDebugEnabled()) {
                int totalConnections = jmxBean.getTotalConnections();
                int idleConnections = jmxBean.getIdleConnections();

                if (idleConnections <= totalConnections * 0.1) {
                    int activeConnections = jmxBean.getActiveConnections();
                    int threadAwaitingConnections = jmxBean.getThreadsAwaitingConnection();
                    log.debug("Total/Active/Idle connections in Pool '{}'/'{}'/'{}', threadsAwaitPool=[{}], {} Tread: {}",
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

    public DataSource getSystemDateSource() {
        return systemDateSource;
    }

    public void setSystemDateSource(DataSource systemDateSource) {
        this.systemDateSource = systemDateSource;
    }

    public String getUrl() {
        return dbUrl;
    }

    @Override
    public String toString() {
        return "DataSourceWrapper{" +
            "dbUrl='" + dbUrl + '\'' +
            ", initialized=" + initialized +
            ", shutdown=" + shutdown +
            '}';
    }
}
