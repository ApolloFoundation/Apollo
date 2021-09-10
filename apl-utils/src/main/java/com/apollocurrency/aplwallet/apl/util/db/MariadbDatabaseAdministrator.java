/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.MigrationParams;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class MariadbDatabaseAdministrator implements DatabaseAdministrator {
//    public static final String APOLLO_MARIADB_INSTALL_DIR="apollo-mariadb";
    public static final String APOLLO_MARIADB_INSTALL_DIR="/home/ylarin/temp_soft/ApolloWallet-1.48.3/apollo-mariadb";

    private final DirProvider dirProvider;
    private final DbProperties dbProperties;
    private MariaDbProcess mariaDbProcess;

    public MariadbDatabaseAdministrator(DirProvider dirProvider, DbProperties dbProperties) {
        this.dirProvider = dirProvider;
        this.dbProperties = dbProperties;
    }

    @Override
    public synchronized void startDatabase() {
        if (!checkOrRunDatabaseServer(dbProperties)) {
            dumpDbLogs();
            throw new RuntimeException("Unable to start from '" + dbInstallationPath() + "' and connect to the MariaDB by url: " + dbProperties.formatJdbcUrlString(true));
        }
    }

    @Override
    public synchronized void deleteDatabase() {

    }

    @Override
    public synchronized String createDatabase() {
        if (StringUtils.isNotBlank(dbProperties.getDbUrl())) {
            return dbProperties.getDbUrl(); // assuming database created already
        }
        HikariConfig sysDBConf = new HikariConfig();
        String systemDbUrl = dbProperties.formatJdbcUrlString(true);
        sysDBConf.setJdbcUrl(systemDbUrl);
        sysDBConf.setUsername(dbProperties.getDbUsername());
        if (StringUtils.isNotBlank(dbProperties.getDbPassword())) {

            sysDBConf.setPassword(dbProperties.getDbPassword());
        }
        sysDBConf.setMaximumPoolSize(5);
        sysDBConf.setPoolName("systemDB");

        try (HikariDataSource systemDataSource = new HikariDataSource(sysDBConf);
             Connection con = systemDataSource.getConnection();
             Statement stmt = con.createStatement()) {

            stmt.execute(
                String.format(
                    "CREATE DATABASE IF NOT EXISTS %1$s;",
                    dbProperties.getDbName())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        return dbProperties.formatJdbcUrlString(false);
    }

    @Override
    public synchronized void migrateDatabase(DBUpdater dbUpdater) {
        dbUpdater.update(new MigrationParams(dbProperties.formatJdbcUrlString(false), dbProperties.getDbType(), dbProperties.getDbUsername(), dbProperties.getDbPassword()));
    }

    @Override
    public synchronized void stopDatabase() {
        if (mariaDbProcess != null) {
            mariaDbProcess.stop();
        }
    }

    private synchronized boolean checkOrRunDatabaseServer(DbProperties conf) {
        boolean res = checkDbWithJDBC(conf);
        //if we have connected to database URL from config, wha have nothing to do
        if(!res){
            // if we can not connect to database, we'll try start it
            // from Apollo package. If it is first start, data base data dir
            // will be initialized
            Path dbDataDir = dirProvider.getDbDir();
            Path dbInstallPath = dbInstallationPath();
            Path dbOutPath = dbLogPath();
            log.info("Setting mariadb process out path: {}", dbOutPath);
            mariaDbProcess = new MariaDbProcess(conf,dbInstallPath,dbDataDir, dbOutPath);
            res = mariaDbProcess.startAndWaitWhenReady();
        }
        return res;
    }

    private boolean checkDbWithJDBC(DbProperties conf) {
        boolean res = true;
        String dbURL = conf.formatJdbcUrlString(true);
        Connection conn;
        try {
            conn = DriverManager.getConnection(dbURL);
            if(!conn.isValid(1)){
                res = false;
            }
        } catch (SQLException ex) {
            res = false;
        }
        return res;
    }

    private void dumpDbLogs() {
        try {
            Files.readAllLines(dbLogPath()).forEach(e-> log.error("MariaDB log file dump: {}", e));
        } catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private Path dbLogPath() {
        return dirProvider.getLogsDir().resolve("maria_out.log");
    }

    private Path dbInstallationPath() {
        return DirProvider.getBinDir().getParent().resolve(APOLLO_MARIADB_INSTALL_DIR);
    }
}
