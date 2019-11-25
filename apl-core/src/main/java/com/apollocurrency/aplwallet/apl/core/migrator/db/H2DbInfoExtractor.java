/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.SneakyThrows;
import org.postgresql.ds.PGConnectionPoolDataSource;

import javax.inject.Inject;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Extract height and path from PostreSQL db
 */
public class H2DbInfoExtractor implements DbInfoExtractor {
    private final String user;
    private final String password;
    private final String dbHost;
    private final int dbPort;
    private final String databaseName;
    private final String dbType;

    @Inject
    public H2DbInfoExtractor(
            final @Property("apl.dbUsername") String user,
            final @Property("apl.dbPassword") String password,
            final @Property("apl.dbHost") String dbHost,
            final @Property("apl.dbPort") int dbPort,
            final @Property("apl.databaseName") String databaseName,
            final @Property("apl.dbType") String dbType
    ) {
        this.user = user;
        this.password = password;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.databaseName = databaseName;
        this.dbType = dbType;
    }

    private String createDbUrl() {
        return String.format(
                "jdbc:%s://%s:%d/%s",
                dbType,
                dbHost,
                dbPort,
                databaseName
        );
    }

    @Override
    public int getHeight(String dbPath) {
        PGConnectionPoolDataSource dataSource = createDataSource(dbPath);
        if (dataSource != null) {
            int height = getHeight(dataSource);
            shutdownDb(dataSource);
            return height;
        } else return 0;
    }

    @Override
    public Path getPath(String dbPath) {
        throw new RuntimeException("Should be refactored not to use any files");
    }

    protected void shutdownDb(PGConnectionPoolDataSource dataSource) {
    }

    @SneakyThrows
    protected PGConnectionPoolDataSource createDataSource(String dbPath) {
        String dbUrl = createDbUrl();
        final PGConnectionPoolDataSource pgConnectionPoolDataSource = new PGConnectionPoolDataSource();
        pgConnectionPoolDataSource.setUrl(dbUrl);
        pgConnectionPoolDataSource.setUser(user);
        pgConnectionPoolDataSource.setPassword(password);
        return pgConnectionPoolDataSource;
    }

    private int getHeight(PGConnectionPoolDataSource dataSource) {
        int height = 0;
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            try (
                    @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
                    final ResultSet rs = stmt.executeQuery("SELECT height FROM block order by \"timestamp\" desc"
                    )
            ) {
                if (rs.next()) {
                    height = rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return height;
    }

}
