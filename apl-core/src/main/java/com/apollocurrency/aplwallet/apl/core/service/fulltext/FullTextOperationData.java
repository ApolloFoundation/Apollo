/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to keep information about data changes in DB to be propagated into Lucene
 */
@Setter
@Getter
public class FullTextOperationData {

    private OperationType operationType; // insert/update or delete data from Lucene
    private List<Object> columnsWithData = new LinkedList<>(); // stores ordered column values
    private String schema; // schema name
    private String tableName; // table name
    private String thread;
    private BigInteger dbIdValue;
    StringBuffer buffer = new StringBuffer();

    public FullTextOperationData(String schema, String tableName, String threadName) {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(threadName);
        this.schema = schema;
        this.tableName = tableName;
        this.thread = threadName;
    }

    public FullTextOperationData addColumnData(Object data) {
        if (data == null) {
            columnsWithData.add("NULL");
        } else {
            columnsWithData.add(data);
        }
        return this;
    }

    public String getTableKey() {
        if (this.dbIdValue == null) {
            throw new RuntimeException("db_id value was not set. Set the value first !!");
        }
        buffer.setLength(0);
        buffer.append(this.schema).append(".").append(this.tableName).append(";DB_ID;")
            .append(this.dbIdValue);
        return buffer.toString();
    }

    public enum OperationType {
        INSERT_UPDATE,
        DELETE
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FullTextOperationData{");
        sb.append("operationType=").append(operationType);
        sb.append(", tableKey='").append(this.dbIdValue != null ? getTableKey() : "db_id is EMPTY!!").append('\'');
        sb.append(", columnsWithData=[").append(columnsWithData.size()).append("]");
        sb.append(", tableName='").append(schema).append(".").append(tableName).append('\'');
        sb.append(", thread='").append(thread).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
