/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import com.apollocurrency.aplwallet.apl.util.config.Property;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Extract height and path from h2 db
 */
public class H2DbInfoExtractor implements DbInfoExtractor {
    private static final String DB_TYPE = "h2";
    private String user;
    private String password;

    @Inject
    public H2DbInfoExtractor(@Property("apl.dbUsername") String user,
                             @Property("apl.dbPassword") String password) {
        this.user = user;
        this.password = password;
    }


    protected void shutdownDb() {
        try (Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306/apollo_new", "root", "12");
             Statement statement = connection.createStatement();
        ) {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public int getHeight() {
        int height = 0;
        try (Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306/apollo_new", "root", "12");
             Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT height FROM block order by `timestamp` desc")) {
                if (rs.next()) {
                    height = rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {
        }
        shutdownDb();
        return height;
    }
}
