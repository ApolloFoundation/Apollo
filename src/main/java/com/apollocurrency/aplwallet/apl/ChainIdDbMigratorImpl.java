/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.chainid.ChainIdDbMigrator;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;

public class ChainIdDbMigratorImpl implements ChainIdDbMigrator  {
    private static final String DEFAULT_DB_TYPE = "h2";
    private static final String DEFAULT_DB_NAME = "apl";
    private static final String DEFAULT_DB_USER = "sa";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DB_SUFFIX = "";

    private final String dbName;
    private final String chainIdDbDir;
    private final String legacyDbDir;
    private final String dbType;
    private final String dbUser;
    private final String dbPassword;
    private final String dbSuffix;


    private static final Logger LOG = getLogger(ChainIdDbMigratorImpl.class);

    private static class DbInfo {
        private Path dbPath;
        private Path dbDir;
        private int height;

        public DbInfo(Path dbPath, Path dbDir, int height) {
            this.dbPath = dbPath;
            this.dbDir = dbDir;
            this.height = height;
        }
    }
    public static class Builder {
        private String dbName = DEFAULT_DB_NAME;
        private String dbType = DEFAULT_DB_TYPE;
        private String chainIdDbDir;
        private String legacyDbDir;

        private String dbUser = DEFAULT_DB_USER;
        private String dbPassword = DEFAULT_PASSWORD;
        private String dbSuffix = DEFAULT_DB_SUFFIX;

        public Builder(String chainIdDbDir, String legacyDbDir) {
            this.chainIdDbDir = chainIdDbDir;
            this.legacyDbDir = legacyDbDir;
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

        public ChainIdDbMigratorImpl build() {
            return new ChainIdDbMigratorImpl(this);
        }
    }

    public ChainIdDbMigratorImpl(Builder builder) {
        this.dbName = builder.dbName;
        this.chainIdDbDir = builder.chainIdDbDir;
        this.legacyDbDir = builder.legacyDbDir;
        this.dbType = builder.dbType;
        this.dbPassword = builder.dbPassword;
        this.dbUser = builder.dbUser;
        this.dbSuffix = builder.dbSuffix == null ? "" : builder.dbSuffix;
    }


    public DbInfo getOldDbInfo() {

        int chainIdDbHeight = getDbHeight(chainIdDbDir);
        if (chainIdDbHeight != 0) {
           return new DbInfo(createDbPath(chainIdDbDir), Paths.get(chainIdDbDir).getParent(), chainIdDbHeight);
        }

        int legacyDbHeight = getDbHeight(legacyDbDir);
        if (legacyDbHeight != 0 ) {

            return new DbInfo(createDbPath(legacyDbDir), Paths.get(legacyDbDir), legacyDbHeight);
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
        int height = 0;
        try(Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM block")) {
                if (rs.next()) {
                    height = rs.getInt(1);
                }
            }
        }
        catch (SQLException ignored) {}
        return height;
    }


    @Override
    public void migrate(String targetDbDir, boolean deleteOldDb) throws IOException {
        DbInfo oldDbInfo = getOldDbInfo();
        if (oldDbInfo != null) {
            LOG.info("Found old db for migration at path {}", oldDbInfo.dbPath);
            int height = oldDbInfo.height;
            if (height > 0) {
                LOG.info("Db {} has blocks - {}. Do migration to {}", oldDbInfo.dbPath, height, targetDbDir);
                Path targetDbDirPath = createDbPath(targetDbDir);
                Files.copy(oldDbInfo.dbPath, targetDbDirPath, StandardCopyOption.REPLACE_EXISTING);
                if (deleteOldDb) {
                    deleteAllWithExclusion(oldDbInfo.dbDir, Paths.get(targetDbDir));
                }
            }
            int actualDbHeight = getDbHeight(targetDbDir);
            if (actualDbHeight != height) {
                throw new RuntimeException(String.format("Db was migrated with errors. Expected height - %d, actual - %d. Application restart is " +
                        "needed.", height, actualDbHeight));
            }
        } else {
            LOG.info("Nothing to migrate");
        }
    }

    private void deleteAllWithExclusion(Path pathToDelete, Path pathToExclude) {

        try {
            List<Path> excludedPaths = Files.walk(pathToExclude.normalize()).collect(Collectors.toList());
            if (pathToExclude.startsWith(pathToDelete)) {
                Path relativePath = pathToDelete.relativize(pathToExclude);
                for (Path aRelativePath : relativePath) {
                    excludedPaths.add(aRelativePath);
                }
                excludedPaths.add(pathToDelete.normalize());
            }
            Files.walkFileTree(pathToDelete.normalize(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!excludedPaths.contains(file)) {
                        Files.delete(file);
                    }
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (!excludedPaths.contains(dir) ) {
                        Files.delete(dir);
                    }
                    return super.postVisitDirectory(dir, exc);
                }
            });
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private static String createDbUrl(String dbDir, String dbName, String type) {
        return String.format("jdbc:%s:%s;MV_STORE=FALSE", type, dbDir + "/" + dbName);
    }

    private Path createDbPath(String dbDir) {
        return Paths.get(dbDir, dbName + dbSuffix);
    }
}
