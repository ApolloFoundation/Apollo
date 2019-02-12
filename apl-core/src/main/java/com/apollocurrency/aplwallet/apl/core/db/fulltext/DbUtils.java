/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.StringValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DbUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DbUtils.class);

    public static TableData getTableData(Connection connection, String tableName, String schemaName) throws SQLException {
        Objects.requireNonNull(connection, "Connection cannot be null");
        StringValidator.requireNonBlank(tableName, "Table name");
        StringValidator.requireNonBlank(schemaName, "Schema");

        List<String> columnNames = new ArrayList<>();
        List<Integer> columnTypes = new ArrayList<>();
        int dbColumn = -1;
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet rs = metaData.getColumns(null, schemaName.toUpperCase(), tableName.toUpperCase(), null);
            int index = 0;
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int columnType = rs.getInt("DATA_TYPE");
                columnNames.add(columnName);
                columnTypes.add(columnType);
                if (columnName.equalsIgnoreCase("DB_ID")) {
                    dbColumn = index;
                }
                index++;
        }
        List<Integer> indexColumns = getIndexColumns(connection, columnNames, columnTypes, schemaName, tableName);
        return new TableData(dbColumn, tableName, schemaName, columnNames, columnTypes, indexColumns);
    }

    private static List<Integer> getIndexColumns(Connection con, List<String> columnNames, List<Integer> columnTypes, String schema, String table) {
        List<Integer> indexColumns = new ArrayList<>();
        try (ResultSet rs = con.createStatement().executeQuery(String.format(
                "SELECT COLUMNS FROM FTL.INDEXES WHERE SCHEMA = '%s' AND TABLE = '%s'", schema.toUpperCase(), table.toUpperCase()))) {
            if (rs.next()) {
                String[] columns = rs.getString(1).split(",");
                for (String column : columns) {
                    int pos = columnNames.indexOf(column.toUpperCase());
                    if (pos >= 0) {
                        if (Types.VARCHAR == columnTypes.get(pos)) {
                            indexColumns.add(pos);
                        } else {
                            LOG.error("Indexed column " + column + " in table " + table + " is not a string");
                        }
                    } else {
                        LOG.error("Indexed column " + column + " not found in table " + table);
                    }
                }
            }
        }
        catch (SQLException e) {
            LOG.error("Cannot process index table", e);
        }
        return indexColumns;
    }
}
