/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Helper class is used for inserting block/transaction data into secondary index tables.
 *
 * @author yuriy.larin
 */
public class SecondaryIndexSelectAndInsertHelper extends AbstractRelinkUpdateHelper {
    private static final Logger log = getLogger(SecondaryIndexSelectAndInsertHelper.class);

    public SecondaryIndexSelectAndInsertHelper() {
    }

    @Override
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, operationParams);

        long startSelect = System.currentTimeMillis();

        if ("BLOCK_INDEX".equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
                    "select ? as shard_id, ID, HEIGHT, DB_ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else if ("TRANSACTION_SHARD_INDEX".equalsIgnoreCase(operationParams.tableName)) {
            sqlToExecuteWithPaging =
                    "select ID, BLOCK_ID, DB_ID from transaction where DB_ID > ? AND DB_ID <= ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                    "select DB_ID from transaction where block_timestamp <= (SELECT TIMESTAMP from BLOCK where HEIGHT = ?) order by block_timestamp desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from TRANSACTION";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'BLOCK_INDEX' OR 'TRANSACTION_SHARD_INDEX' is expected. Pls use another Helper class");
        }
        // select upper, bottom DB_ID
        selectUpperBottomValues(sourceConnect, operationParams);

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
                paginateResultWrapper.limitValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method

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


}