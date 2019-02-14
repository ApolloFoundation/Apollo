/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.enterprise.inject.spi.CDI;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Functions for inner db conversions
 */
public class DbBytesConverter {
    public static final String BYTE_TO_LONG_DB_FUNCTION_NAME = "bytes_to_long";
    public static final String BYTE_TO_LONG_METHOD_NAME = "getLong";
    private static DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();

    public static long getLong(byte[] bytes, int startPosition) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(startPosition);
        return buffer.getLong();
    }

    public static void init() {
        try (Connection connection = databaseManager.getDataSource().getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP ALIAS IF EXISTS " + BYTE_TO_LONG_DB_FUNCTION_NAME);
            String className = DbBytesConverter.class.getName();
            String functionPath = className + "." + BYTE_TO_LONG_METHOD_NAME;
            stmt.executeUpdate(String.format("CREATE ALIAS %s FOR \"%s\"", BYTE_TO_LONG_DB_FUNCTION_NAME, functionPath));
        }
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
