/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class IndexService {
    /**
     * Reindex the table
     *
     * @param conn SQL connection
     * @throws SQLException Unable to reindex table
     */
    private void reindexTable(Connection conn, String tableName) throws SQLException {
        if (indexColumns.isEmpty()) {
            return;
        }
        //
        // Build the SELECT statement for just the indexed columns
        //
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT DB_ID");
        for (int index : indexColumns) {
            sb.append(", ").append(columnNames.get(index));
        }
        sb.append(" FROM ").append(tableName);
        Object[] row = new Object[columnNames.size()];
        //
        // Index each row in the table
        //
        try (Statement qstmt = conn.createStatement();
             ResultSet rs = qstmt.executeQuery(sb.toString())) {
            while (rs.next()) {
                row[dbColumn] = rs.getObject(1);
                int i = 2;
                for (int index : indexColumns) {
                    row[index] = rs.getObject(i++);
                }
                indexRow(row);
            }
        }
        //
        // Commit the index updates
        //
        commitIndex();
    }

    public Map<String, String> getColumnNamesWithTypes(Connection connection, String tableName, String schemaName) throws SQLException {
        Map<String, String> res = new HashMap<>();
        try (ResultSet rs = connection.createStatement().executeQuery("SHOW COLUMNS FROM " + tableName + " FROM " + schemaName)) {
            while (rs.next()) {
                String columnName = rs.getString("FIELD");
                String columnType = rs.getString("TYPE");
                columnType = columnType.substring(0, columnType.indexOf('('));
                res.put(columnName, columnType);
            }
        }
        return res;
    }
}
