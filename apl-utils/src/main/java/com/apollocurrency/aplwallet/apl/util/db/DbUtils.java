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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public final class DbUtils {
    private static final Logger log = getLogger(DbUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String EMPTY_JS_ARRAY = "[]";

    private DbUtils() {
    } // never

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Close a result set without throwing an exception.
     *
     * @param rs the result set or null
     */
    public static void closeSilently(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
        }
    }

    public static void rollback(Connection con) {
        try {
            if (con != null) {
                con.rollback();
            }
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }

    }

    public static void setBytes(PreparedStatement pstmt, int index, byte[] bytes) throws SQLException {
        if (bytes != null) {
            pstmt.setBytes(index, bytes);
        } else {
            pstmt.setNull(index, Types.BINARY);
        }
    }

    public static void setString(PreparedStatement pstmt, int index, String s) throws SQLException {
        if (s != null) {
            pstmt.setString(index, s);
        } else {
            pstmt.setNull(index, Types.VARCHAR);
        }
    }

    public static void setLong(PreparedStatement pstmt, int index, Long l) throws SQLException {
        if (l != null) {
            pstmt.setLong(index, l);
        } else {
            pstmt.setNull(index, Types.BIGINT);
        }
    }

    public static void setShortZeroToNull(PreparedStatement pstmt, int index, short s) throws SQLException {
        if (s != 0) {
            pstmt.setShort(index, s);
        } else {
            pstmt.setNull(index, Types.SMALLINT);
        }
    }

    public static void setIntZeroToNull(PreparedStatement pstmt, int index, int n) throws SQLException {
        if (n != 0) {
            pstmt.setInt(index, n);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    public static void setByteZeroToNull(PreparedStatement pstmt, int index, byte n) throws SQLException {
        if (n != 0) {
            pstmt.setByte(index, n);
        } else {
            pstmt.setNull(index, Types.TINYINT);
        }
    }

    public static void setLongZeroToNull(PreparedStatement pstmt, int index, long l) throws SQLException {
        if (l != 0) {
            pstmt.setLong(index, l);
        } else {
            pstmt.setNull(index, Types.BIGINT);
        }
    }

    public static <T> T[] getArray(ResultSet rs, String columnName, Class<? extends T[]> cls) throws SQLException {
        return getArray(rs, columnName, cls, null);
    }

    public static <T> T[] getArray(ResultSet rs, String columnName, Class<? extends T[]> cls, T[] ifNull) throws SQLException {
        final String string = rs.getString(columnName);
        if (string == null || EMPTY_JS_ARRAY.equals(string)) {
            return ifNull;
        } else {
            try {
                return OBJECT_MAPPER.readValue(string, cls);
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        }
    }

    public static <T> void setArray(PreparedStatement pstmt, int index, T[] array) throws SQLException {
        if (array != null) {
            try {
                pstmt.setString(index, OBJECT_MAPPER.writeValueAsString(array));
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        } else {
            pstmt.setString(index, EMPTY_JS_ARRAY);
        }
    }

    public static <T> void setArrayEmptyToNull(PreparedStatement pstmt, int index, T[] array) throws SQLException {
        if (array != null && array.length > 0) {
            try {
                pstmt.setString(index, OBJECT_MAPPER.writeValueAsString(array));
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        } else {
            pstmt.setString(index, "[]");
        }
    }

    public static byte[][] get2dByteArray(
        final ResultSet resultSet,
        final String columnName,
        final byte[][] defaultValue
    ) throws SQLException {
        final byte[] resultSetBytes = resultSet.getBytes(columnName);
        if (resultSetBytes == null) {
            return defaultValue;
        }
        return Optional.ofNullable(convertFromBytes(resultSetBytes))
            .map(byte[][].class::cast)
            .orElse(defaultValue);
    }

    public static void set2dByteArray(PreparedStatement pstmt, int index, byte[][] bytes) throws SQLException {
        if (bytes != null) {
            pstmt.setBytes(index, convertToBytes(bytes));
        } else {
            pstmt.setNull(index, Types.BINARY);
        }
    }

    private static byte[] convertToBytes(final Object object) {
        try (
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutput out = new ObjectOutputStream(bos)
        ) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            log.debug("Failed to convertToBytes: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static Object convertFromBytes(final byte[] bytes) {
        try (
            final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            final ObjectInput in = new ObjectInputStream(bis)
        ) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.debug("Failed to convertFromBytes: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static int calculateLimit(int from, int to) {
        return to >= 0
            && to >= from
            && to < Integer.MAX_VALUE ?
            to - from + 1 : 0;
    }

    public static String limitsClause(int from, int to) {
        int limit = calculateLimit(from, to);
        if (limit > 0 && from > 0) {
            return " LIMIT ? OFFSET ? ";
        } else if (limit > 0) {
            return " LIMIT ? ";
        } else if (from > 0) {
            if (to > 0) {
                return String.format(" LIMIT %d OFFSET ? ", to);
            } else {
                return " LIMIT 0 OFFSET ? ";
            }
        } else {
            return "";
        }
    }

    public static int setLimits(int index, PreparedStatement pstmt, int from, int to) throws SQLException {
        int limit = calculateLimit(from, to);
        if (limit > 0) {
            pstmt.setInt(index++, limit);
        }
        if (from > 0) {
            pstmt.setInt(index++, from);
        }
        return index;
    }

}
