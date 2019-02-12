/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.util.List;
import java.util.Objects;

/**
 * Holds table specific data
 */
public class TableData {
    private final int dbIdColumnPosition;
    private final String table;
    private final String schema;
    private final List<String> columnNames;
    private final List<Integer> columnTypes;
    private final List<Integer> indexColumns;

    public TableData(int dbIdColumnPosition, String table, String schema, List<String> columnNames, List<Integer> columnTypes,
                     List<Integer> indexColumns) {
        this.dbIdColumnPosition = dbIdColumnPosition;
        this.table = table.toUpperCase();
        this.schema = schema.toUpperCase();
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.indexColumns = indexColumns;
    }

    public List<Integer> getIndexColumns() {
        return indexColumns;
    }

    public int getDbIdColumnPosition() {
        return dbIdColumnPosition;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Integer> getColumnTypes() {
        return columnTypes;
    }

    public String getTable() {
        return table;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableData)) return false;
        TableData tableData = (TableData) o;
        return dbIdColumnPosition == tableData.dbIdColumnPosition &&
                Objects.equals(table, tableData.table) &&
                Objects.equals(schema, tableData.schema) &&
                Objects.equals(columnNames, tableData.columnNames) &&
                Objects.equals(columnTypes, tableData.columnTypes) &&
                Objects.equals(indexColumns, tableData.indexColumns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbIdColumnPosition, table, schema, columnNames, columnTypes, indexColumns);
    }

    @Override
    public String toString() {
        return "TableData{" +
                "dbIdColumnPosition=" + dbIdColumnPosition +
                ", table='" + table + '\'' +
                ", schema='" + schema + '\'' +
                ", columnNames=" + columnNames +
                ", columnTypes=" + columnTypes +
                ", indexColumns=" + indexColumns +
                '}';
    }
}
