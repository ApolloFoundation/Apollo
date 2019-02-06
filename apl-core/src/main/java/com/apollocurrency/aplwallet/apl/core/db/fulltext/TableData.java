/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.util.List;

/**
 * Holds table specific data
 */
public class TableData {
    private final int dbIdColumnPosition;
    private final String table;
    private final String schema;
    private final List<String> columnNames;
    private final List<String> columnTypes;
    private final List<Integer> indexColumns;

    public TableData(int dbIdColumnPosition, String table, String schema, List<String> columnNames, List<String> columnTypes, List<Integer> indexColumns) {
        this.dbIdColumnPosition = dbIdColumnPosition;
        this.table = table;
        this.schema = schema;
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

    public List<String> getColumnTypes() {
        return columnTypes;
    }

    public String getTable() {
        return table;
    }

    public String getSchema() {
        return schema;
    }
}
