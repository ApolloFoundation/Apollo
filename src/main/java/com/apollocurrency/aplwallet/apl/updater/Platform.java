/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.SystemUtils;
import org.h2.jdbcx.JdbcDataSource;

public enum Platform {

    LINUX, WINDOWS, MAC_OS, ALL, OSX;


    public static Platform current() {
        return
               SystemUtils.IS_OS_WINDOWS ? Platform.WINDOWS : // Windows
               SystemUtils.IS_OS_LINUX  ? Platform.LINUX   : // Linux
               SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.MAC_OS : // Mac
                                                                    null;              // Other
    }

    public boolean isAppropriate(Platform platform) {
        return this == platform || platform == ALL;
    }

    public static void main(String[] args) throws SQLException {
        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setPassword("sa");
        jdbcDataSource.setUser("sa");
        jdbcDataSource.setURL("jdbc:h2:/home/andrew/.apollo/apl_db/b5d7b697-f359-4ce5-a619-fa34b6fb01a5/apl");
        try (Connection connection = jdbcDataSource.getConnection();
            Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT count(*) from block")) {
                resultSet.next();
                System.out.println(resultSet.getInt(1));
            }
        };

    }
}
