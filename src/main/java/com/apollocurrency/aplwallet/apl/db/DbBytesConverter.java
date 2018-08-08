/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.db;

import com.apollocurrency.aplwallet.apl.Db;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Functions for inner db conversions
 */
public class DbBytesConverter {
    public static final String BYTE_TO_LONG_DB_FUNCTION_NAME = "bytes_to_long";
    public static final String BYTE_TO_LONG_METHOD_NAME = "getLong";


    public static long getLong(byte[] bytes, int startPosition) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(startPosition);
        return buffer.getLong();
    }

    public static void init() {
        Connection connection = null;
        try {
            connection = Db.db.getConnection();
            PreparedStatement statement = connection.prepareStatement("DROP ALIAS IF EXISTS " + BYTE_TO_LONG_DB_FUNCTION_NAME);
            statement.executeUpdate();
            String className = DbBytesConverter.class.getName();
            String functionPath = className + "." + BYTE_TO_LONG_METHOD_NAME;
            PreparedStatement stmt = connection.prepareStatement("CREATE ALIAS ? FOR \"?\"");
            int i = 0;
            stmt.setString(++i, BYTE_TO_LONG_DB_FUNCTION_NAME);
            stmt.setString(++i, functionPath);

        }
        catch (SQLException e) {
            DbUtils.close(connection);
            throw new RuntimeException(e.toString(), e);
        }
    }
}
