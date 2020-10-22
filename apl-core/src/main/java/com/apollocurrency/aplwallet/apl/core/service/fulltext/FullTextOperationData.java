/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class to keep information about data changes in DB to be propagated into Lucene
 */
@Data
@AllArgsConstructor
public class FullTextOperationData {

    private OperationType operationType; // insert/update or delete data from Lucene
    private String tableKey; // table name + column name + DB_ID value joined with ';'
    private List<Object> columnsWithData = new LinkedList<>(); // stores ordered column values
    private String tableName; // table name

    public FullTextOperationData(OperationType operationType, String tableKey, String tableName) {
        Objects.requireNonNull(operationType);
        Objects.requireNonNull(tableKey);
        Objects.requireNonNull(tableName);
        this.operationType = operationType;
        this.tableKey = tableKey;
        this.tableName = tableName;
    }

    public FullTextOperationData(String tableKey, String tableName) {
        Objects.requireNonNull(tableKey);
        Objects.requireNonNull(tableName);
        this.tableKey = tableKey;
        this.tableName = tableName;
    }

    public FullTextOperationData(OperationType operationType, String tableName) {
        Objects.requireNonNull(operationType);
        Objects.requireNonNull(tableName);
        this.operationType = operationType;
        this.tableName = tableName;
    }

    public FullTextOperationData addColumnData(Object data) {
        Objects.requireNonNull(data);
        columnsWithData.add(data);
        return this;
    }

    public enum OperationType {
        INSERT_UPDATE,
        DELETE
    }

}
