/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.dbmodel.Option;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;

public class ChainIdDbMigrator {
    private static final String DEFAULT_DB_TYPE = "h2";
    private static final String DEFAULT_DB_NAME = "apl";
    private static final String DEFAULT_DB_USER = "sa";
    private static final String DEFAULT_PASSWORD = "sa";
    private static final String DEFAULT_DB_SUFFIX = "";

    private String dbName;
    private String chainIdDbDir;
    private String legacyDbDir;
    private String targetDbDir;
    private String dbType;
    private String dbUser;
    private String dbPassword;
    private String dbSuffix;


    private static final Logger LOG = getLogger(ChainIdDbMigrator.class);

    private static class DbInfo {
        private Path path;
        private int height;

        public DbInfo(Path path, int height) {
            this.path = path;
            this.height = height;
        }
    }
    public static class Builder {
        private String dbName = DEFAULT_DB_NAME;
        private String dbType = DEFAULT_DB_TYPE;
        private String chainIdDbDir;
        private String legacyDbDir;
        private String targetDbDir;

        private String dbUser;
        private String dbPassword;
        private String dbSuffix = DEFAULT_DB_SUFFIX;

        public Builder(String chainIdDbDir, String legacyDbDir, String targetDbDir) {
            this.chainIdDbDir = chainIdDbDir;
            this.legacyDbDir = legacyDbDir;
            this.targetDbDir = targetDbDir;
        }

        public Builder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }
        public Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }
        public Builder dbUser(String dbUser) {
            this.dbUser = dbUser;
            return this;
        }
        public Builder dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }
        public Builder dbSuffix(String suffix) {
            this.dbSuffix = suffix;
            return this;
        }

        public ChainIdDbMigrator build() {
            return new ChainIdDbMigrator(this);
        }
    }

    public ChainIdDbMigrator(Builder builder) {
        this.dbName = builder.dbName;
        this.chainIdDbDir = builder.chainIdDbDir;
        this.legacyDbDir = builder.legacyDbDir;
        this.dbType = builder.dbType;
        this.dbPassword = builder.dbPassword;
        this.dbUser = builder.dbUser;
    }


    public DbInfo getOldDbInfo() throws IOException {

        int chainIdDbHeight = getDbHeight(chainIdDbDir);
        if (chainIdDbHeight != 0) {
           return new DbInfo(createDbPath(chainIdDbDir), chainIdDbHeight);
        }

        int legacyDbHeight = getDbHeight(legacyDbDir);
        if (legacyDbHeight != 0 ) {

            return new DbInfo(createDbPath(legacyDbDir), legacyDbHeight);
        } else return null;
    }

    private int getDbHeight(String dbDir) {
        DataSource dataSource = createDataSource(dbDir);
        if (dataSource != null) {
            int height = getHeight(dataSource);
            shutdownDb(dataSource);
            return height;
        } else return 0;
    }

    protected void shutdownDb(DataSource dataSource) {
        try {
            Connection connection = dataSource.getConnection();
            connection.createStatement().execute("SHUTDOWN");
        }
        catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected DataSource createDataSource(String dbDir) {
        if (!checkPath(dbDir)) {
            return null;
        }
        String dbUrl = createDbUrl(dbDir, dbName, dbType);
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setPassword("sa");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setURL(dbUrl);
        return jdbcDataSource;
    }

    private boolean checkPath(String dbDir) {
        Path dbPath = Paths.get(dbDir, dbName + DEFAULT_DB_SUFFIX);
        return Files.exists(dbPath);
    }

    private int getHeight(DataSource dataSource) {
        BlockDb blockDb = new BlockDb(new ConnectionProviderImpl() {
            @Override
            public Connection getConnection() throws SQLException {
                return dataSource.getConnection();
            }
        });
        BlockImpl lastBlock = blockDb.findLastBlock();
        return lastBlock == null ? 0 : lastBlock.getHeight();
    }


    public void migrate() throws IOException {
            DbInfo oldDbInfo = getOldDbInfo();
            if (oldDbInfo != null) {
                int height = oldDbInfo.height;
                if (height > 0) {
                    LOG.info("Required db migration for applying chainId");
                    Path targetDbDirPath = createDbPath(targetDbDir);
                    LOG.info("Copying db from {} to {}", oldDbInfo.path, targetDbDir);
                    Files.copy(oldDbInfo.path, targetDbDirPath, StandardCopyOption.REPLACE_EXISTING);

                }
                Db.init();
                if (height > 0) {
                    BlockImpl lb = BlockDb.findLastBlock();
                    int actualHeight = lb == null ? 0 : lb.getHeight();
                    if (actualHeight != height) {
                        LOG.error("Db was copied with errors. Restart your wallet.");
                        System.exit(-1);
                    }
                }
            } else {
                Db.init();
            }
            Option.set("secondDbMigrationRequired", "false");
        }
    }

    private static String createDbUrl(String dbDir, String dbName, String type) {
        return String.format("jdbc:%s:%s", type, dbDir + "/" + dbName);
    }

    private Path createDbPath(String dbDir) {
        return Paths.get(dbDir, dbName + dbSu)
    }
}
