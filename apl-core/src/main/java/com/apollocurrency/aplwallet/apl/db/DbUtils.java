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

package com.apollocurrency.aplwallet.apl.db;

import org.slf4j.Logger;

import java.sql.*;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

public final class DbUtils {
    private static final Logger LOG = getLogger(DbUtils.class);

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {}
            }
        }
    }

    public static void rollback(Connection con) {
        try {
            if (con != null) {
                con.rollback();
            }
        } catch (SQLException e) {
            LOG.error(e.toString(), e);
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
        Array array = rs.getArray(columnName);
        if (array != null) {
            Object[] objects = (Object[]) array.getArray();
            return Arrays.copyOf(objects, objects.length, cls);
        } else {
            return ifNull;
        }
    }

    public static <T> void setArray(PreparedStatement pstmt, int index, T[] array) throws SQLException {
        if (array != null) {
            pstmt.setObject(index, array);
        } else {
            pstmt.setNull(index, Types.ARRAY);
        }
    }

    public static <T> void setArrayEmptyToNull(PreparedStatement pstmt, int index, T[] array) throws SQLException {
        if (array != null && array.length > 0) {
            pstmt.setObject(index, array);
        } else {
            pstmt.setNull(index, Types.ARRAY);
        }
    }

    public static String limitsClause(int from, int to) {
        int limit = to >=0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
        if (limit > 0 && from > 0) {
            return " LIMIT ? OFFSET ? ";
        } else if (limit > 0) {
            return " LIMIT ? ";
        } else if (from > 0) {
            return " LIMIT NULL OFFSET ? ";
        } else {
            return "";
        }
    }

    public static int setLimits(int index, PreparedStatement pstmt, int from, int to) throws SQLException {
        int limit = to >=0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
        if (limit > 0) {
            pstmt.setInt(index++, limit);
        }
        if (from > 0) {
            pstmt.setInt(index++, from);
        }
        return index;
    }

    private DbUtils() {} // never

}
