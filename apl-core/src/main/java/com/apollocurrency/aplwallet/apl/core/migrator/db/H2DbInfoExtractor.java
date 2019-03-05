/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * Extract height and path from h2 db
 */
public class H2DbInfoExtractor implements DbInfoExtractor {
    private static final String DB_TYPE = "h2";
    private static final String DB_SUFFIX = ".h2.db";
    private String user;
    private String password;

    @Inject
    public H2DbInfoExtractor(@Property(name = "apl.dbUsername") String user,
                             @Property(name = "apl.dbPassword") String password) {
        this.user = user;
        this.password = password;
    }

    private static String createDbUrl(String dbPath, String type) {
        return String.format("jdbc:%s:%s;MV_STORE=FALSE", type, dbPath);
    }

    private Path createDbPath(String dbPath) {
        return Paths.get(dbPath + DB_SUFFIX);
    }
    @Override
    public int getHeight(String dbPath) {
        JdbcConnectionPool dataSource = createDataSource(dbPath);
        if (dataSource != null) {
            int height = getHeight(dataSource);
            shutdownDb(dataSource);
            return height;
        } else return 0;
    }

    @Override
    public Path getPath(String dbPath) {
        return createDbPath(dbPath);
    }

    protected void shutdownDb(JdbcConnectionPool dataSource) {
        try {
            Connection connection = dataSource.getConnection();
            connection.createStatement().execute("SHUTDOWN");
            dataSource.dispose();
        }
        catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected JdbcConnectionPool createDataSource(String dbPath) {
        if (!checkPath(dbPath)) {
            return null;
        }
        String dbUrl = createDbUrl(dbPath, DB_TYPE);
        return JdbcConnectionPool.create(dbUrl, user, password);
    }

    private boolean checkPath(String dbDir) {
        Path dbPath = createDbPath(dbDir);
        return Files.exists(dbPath);
    }

    private int getHeight(DataSource dataSource) {
        int height = 0;
        try(Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT height FROM block order by timestamp desc")) {
                if (rs.next()) {
                    height = rs.getInt(1);
                }
            }
        }
        catch (SQLException ignored) {}
        return height;
    }

}
