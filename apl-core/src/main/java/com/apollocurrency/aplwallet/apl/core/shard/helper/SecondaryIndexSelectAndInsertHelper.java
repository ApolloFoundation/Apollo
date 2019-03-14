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
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Helper class is used for changing/updating linked table's data from old Block records to snapshot Block record.
 *
 * @author yuriy.larin
 */
public class SecondaryIndexSelectAndInsertHelper implements BatchedSelectInsert {
    private static final Logger log = getLogger(SecondaryIndexSelectAndInsertHelper.class);

    private String currentTableName;
    private ResultSetMetaData rsmd;
    private int numColumns;
    private int[] columnTypes;
//    int targetDbIdColumnIndex = -1;

    private StringBuilder sqlInsertString = new StringBuilder(500);
    private StringBuilder columnNames = new StringBuilder();
    private StringBuilder columnQuestionMarks = new StringBuilder();
//    private StringBuilder columnValues = new StringBuilder();

    private Long totalRowCount = 0L;
    private Long insertedCount = 0L;
    private String BASE_COLUMN_NAME = "DB_ID";
    private PreparedStatement preparedInsertStatement = null;


    public SecondaryIndexSelectAndInsertHelper() {
    }

    @Override
    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
        this.columnTypes = null;
//        this.targetDbIdColumnIndex = -1;
        this.sqlInsertString = new StringBuilder(500);
        this.columnNames = new StringBuilder();
        this.columnQuestionMarks = new StringBuilder();
//        this.columnValues = new StringBuilder();
        this.totalRowCount = 0L;
        this.insertedCount = 0L;
        this.preparedInsertStatement = null;
    }

    @Override
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        if (!operationParams.shardId.isPresent()) {
            String error = "Error, Optional shardId is not present";
            log.error(error);
            throw new IllegalArgumentException(error);
        }
        currentTableName = operationParams.tableName;

        long startSelect = System.currentTimeMillis();
        String sqlToExecuteWithPaging = null;
        String sqlSelectUpperBound = null;
        String sqlSelectBottomBound = null;
        Long upperBoundIdValue = null;
        Long lowerBoundIdValue = null;

        if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
//                    "SELECT * FROM BLOCK WHERE DB_ID > ? AND DB_ID < ? limit ?";
                    "select ? as shard_id, ID, HEIGHT, DB_ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(operationParams.tableName)) {
            sqlToExecuteWithPaging = //"SELECT ID, BLOCK_ID from TRANSACTION where BLOCK_ID in (SELECT ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?)";
//                    "insert into TRANSACTION_SHARD_INDEX (transaction_id, block_id) SELECT ID, BLOCK_ID from TRANSACTION where BLOCK_ID in " +
//                            "(SELECT BLOCK.ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?)";
                    "select ID, BLOCK_ID, DB_ID from transaction where DB_ID > ? AND DB_ID <= ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                    "select DB_ID from transaction where block_timestamp <= (SELECT TIMESTAMP from BLOCK where HEIGHT = ?) order by block_timestamp desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from TRANSACTION";
            log.trace(sqlSelectBottomBound);
        }
        // select MAX DB_ID
        upperBoundIdValue = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight, sqlSelectUpperBound);
        if (upperBoundIdValue == null) {
            String error = String.format("Not Found MAX height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // select MIN DB_ID
        lowerBoundIdValue = selectLowerDbId(sourceConnect, sqlSelectBottomBound);
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);

        // turn OFF HEIGHT constraint for specified table
        if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect, "drop index IF EXISTS block_index_block_id_shard_id_idx");
            issueConstraintUpdateQuery(sourceConnect, "drop index IF EXISTS block_index_block_height_shard_id_idx");
        } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect, "alter table transaction_shard_index drop constraint transaction_shard_index_block_fk");
            issueConstraintUpdateQuery(sourceConnect, "drop index IF EXISTS transaction_index_shard_1_idx");
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
                    ps.setLong(1, operationParams.shardId.get());
                    ps.setLong(2, paginateResultWrapper.limitValue);
                    ps.setLong(3, upperBoundIdValue);
                    ps.setLong(4, operationParams.batchCommitSize);
                } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(operationParams.tableName)) {
//                    ps.setLong(1, operationParams.shardId.get());
                    ps.setLong(1, paginateResultWrapper.limitValue);
                    ps.setLong(2, upperBoundIdValue);
                    ps.setLong(3, operationParams.batchCommitSize);
                }
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams.batchCommitSize));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null && !this.preparedInsertStatement.isClosed()) {
                this.preparedInsertStatement.close();
            }
        }
        log.debug("Inserted '{}' = [{}] within {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);

        // turn ON HEIGHT constraint for specified table
        if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect,
                    "CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_id_shard_id_idx ON block_index (block_id, shard_id DESC)");
            issueConstraintUpdateQuery(sourceConnect,
                    "CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_height_shard_id_idx ON block_index (block_height, shard_id DESC)");
        } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect,
                    "ALTER TABLE transaction_shard_index ADD CONSTRAINT IF NOT EXISTS transaction_shard_index_block_fk FOREIGN KEY (block_id) REFERENCES block_index(block_id)");
            issueConstraintUpdateQuery(sourceConnect,
                    "CREATE UNIQUE INDEX IF NOT EXISTS transaction_index_shard_1_idx ON transaction_shard_index (transaction_id, block_id)");
        }

        log.debug("Total (with CONSTRAINTS) '{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                /*
                 * handle rows here
                 */
                if (rsmd == null) {
                    rsmd = rs.getMetaData();
                    numColumns = rsmd.getColumnCount();
                    columnTypes = new int[numColumns];
/*
                    for (int i = 0; i < numColumns; i++) {
                        columnTypes[i] = rsmd.getColumnType(i + 1);
                        if (i != 0) {
                            columnNames.append(",");
                            columnQuestionMarks.append("?,");
                        }
                        String columnName = rsmd.getColumnName(i + 1);
                        columnNames.append(columnName);
                    }
                    columnQuestionMarks.append("?");
*/
                    if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
                        sqlInsertString.append("insert into BLOCK_INDEX (shard_id, block_id, block_height)")
                                .append(" values (").append("?, ?, ?").append(")");
                    } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(currentTableName)) {
                        sqlInsertString.append("insert into TRANSACTION_SHARD_INDEX (transaction_id, block_id)")
                                .append(" values (").append("?, ?").append(")");
                    }
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = targetConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled insert = {}", sqlInsertString);
                    }
                }
//                if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
                    paginateResultWrapper.limitValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method
//                } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(currentTableName)) {
//                    paginateResultWrapper.limitValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method
//                }

                try {
                    for (int i = 0; i < numColumns; i++) {
                        if (i + 1 != numColumns) {
                            preparedInsertStatement.setObject(i + 1, rs.getObject(i + 1));
                        }
                    }
                    insertedCount += preparedInsertStatement.executeUpdate();
                    log.trace("Inserting '{}' into {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
                } catch (Exception e) {
                    log.error("Failed Inserting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
                    log.error("Failed inserting " + currentTableName, e);
                    targetConnect.rollback();
                    throw e;
                }
                rows++;
            }
            totalRowCount += rows;
        }
        log.trace("Total Records '{}': selected = {}, inserted = {}, rows = {}, {}={}", currentTableName,
                totalRowCount, insertedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);

        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight, String selectValueSql) throws SQLException {
//        String selectValueSql =
//                "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName + " WHERE HEIGHT < ?";
        Long highDbIdValue = null;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
            selectStatement.setInt(1, snapshotBlockHeight.intValue());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                highDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Upper DB_ID value = {}", highDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding Upper DB_ID in = " + currentTableName, e);
            throw e;
        }
        return highDbIdValue;
    }

    private Long selectLowerDbId(Connection sourceConnect, String selectValueSql) throws SQLException {
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
//        String selectValueSql = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                bottomDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND LOW LIMIT DB_ID value = {}", bottomDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding LOW LIMIT DB_ID in = " + currentTableName, e);
            throw e;
        }
        return bottomDbIdValue;
    }

    private void issueConstraintUpdateQuery(Connection sourceConnect, String sqlToExecute) throws SQLException {
        try (Statement stmt = sourceConnect.createStatement()) {
                stmt.executeUpdate(sqlToExecute);
                log.trace("SUCCESS, on constraint SQL = {}", sqlToExecute);
        } catch (Exception e) {
            log.error("Error on 'constraint related' SQL = " + currentTableName, e);
            throw e;
        }
    }

}