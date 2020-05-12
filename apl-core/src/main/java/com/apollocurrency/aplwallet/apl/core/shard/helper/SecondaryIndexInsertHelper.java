/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper class is used for inserting block/transaction data into secondary index tables.
 *
 * @author yuriy.larin
 */
public class SecondaryIndexInsertHelper extends AbstractHelper {
    private static final Logger log = getLogger(SecondaryIndexInsertHelper.class);

    public SecondaryIndexInsertHelper() {
    }

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect, /* targetConnect is NOT used here*/
                                 TableOperationParams operationParams)
        throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, operationParams);

        long startSelect = System.currentTimeMillis();

        assignMainBottomTopSelectSql(operationParams);
        // select upper, bottom DB_ID
        this.upperBoundIdValue = selectUpperBoundValue(sourceConnect, operationParams);
        recoveryValue = shardRecoveryDao.getLatestShardRecovery(sourceConnect);

        if (restoreLowerBoundIdOrSkipTable(sourceConnect, operationParams, recoveryValue)) {
            return totalSelectedRows; // skip current table
        }
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);

        // turn OFF HEIGHT constraint for specified table
/*
       // after moving from old H2 PageStore engine into new engine MVStore (1.4.199)
       // we no longer need DROP and CREATE index(s) for performance
        if (ShardConstants.BLOCK_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS block_index_block_id_idx");
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS block_index_block_height_idx");
        } else if (ShardConstants.TRANSACTION_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS transaction_shard_index_height_transaction_index_idx");
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS transaction_shard_index_transaction_id_height_idx");
        }
*/
        Set<Long> exludeDbIds = operationParams.excludeInfo != null ? operationParams.excludeInfo.getNotCopyDbIds() : Set.of();

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(2, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams, exludeDbIds));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null /*&& !this.preparedInsertStatement.isClosed()*/) {
                log.trace("preparedInsertStatement will be CLOSED!");
                DbUtils.close(this.preparedInsertStatement);
            }
        }
        log.debug("Inserted '{}' = [{}] within {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);

        // create HEIGHT constraint for specified table if NOT EXISTS
        if (ShardConstants.BLOCK_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                "CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_id_idx ON block_index (block_id)");
            executeUpdateQuery(sourceConnect,
                "CREATE UNIQUE INDEX IF NOT EXISTS block_index_block_height_idx ON block_index (block_height)");
        } else if (ShardConstants.TRANSACTION_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                "CREATE UNIQUE INDEX IF NOT EXISTS transaction_shard_index_height_transaction_index_idx ON transaction_shard_index (height, transaction_index)");
            executeUpdateQuery(sourceConnect,
                "CREATE UNIQUE INDEX IF NOT EXISTS transaction_shard_index_transaction_id_height_idx ON transaction_shard_index (transaction_id, height)");
        }

        log.debug("Total (with CONSTRAINTS) '{}' = [{}] in {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);
        return totalSelectedRows;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection sourceConnect, TableOperationParams operationParams, Set<Long> exludeDbIds)
        throws SQLException {
        int rows = 0;
        int processedRows = 0;
        boolean isTransactionTable = operationParams.tableName.equalsIgnoreCase(ShardConstants.TRANSACTION_INDEX_TABLE_NAME);
        try (ResultSet rs = ps.executeQuery()) {
            log.trace("SELECT...where DB_ID > {} AND DB_ID < {} LIMIT {}",
                paginateResultWrapper.lowerBoundColumnValue, paginateResultWrapper.upperBoundColumnValue,
                operationParams.batchCommitSize);
            while (rs.next()) {
                // it called one time one first loop only
                extractMetaDataCreateInsert(sourceConnect, rs);
                rows++;
                paginateResultWrapper.lowerBoundColumnValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method
                if (ShardConstants.TRANSACTION_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName) && exludeDbIds.contains(paginateResultWrapper.lowerBoundColumnValue)) {
                    log.debug("Will skip row with db_id = {}", paginateResultWrapper.lowerBoundColumnValue);
                    continue;
                }
                try {
                    for (int i = 0; i < numColumns; i++) {
                        // here we are skipping DB_ID latest column in ResultSet
                        // we don't need it for INSERT, only for next SELECT
                        if (i + 1 != numColumns) {
                            Object object = rs.getObject(i + 1);
                            if (isTransactionTable && columnTypes[i] == Types.VARBINARY) { // extract and shorten hash (id + shortened hash = full_hash)
                                byte[] fullHash = (byte[]) object;
                                object = Convert.toPartialHash(fullHash);
                            }
                            preparedInsertStatement.setObject(i + 1, object);
                        }
                    }
                    processedRows += preparedInsertStatement.executeUpdate();
                    log.trace("Inserting '{}' into {} : next column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                } catch (Exception e) {
                    log.error("Failed Inserting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                    log.error("Failed inserting " + currentTableName, e);
                    sourceConnect.rollback();
                    throw e;
                }

            }
            totalSelectedRows += rows;
            totalProcessedCount += processedRows;
        }
        log.trace("Total Records '{}': selected = {}, updated = {}, rows = {}, {}={}", currentTableName,
            totalSelectedRows, totalProcessedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
        if (rows == 1) {
            // in case we have only 1 RECORD selected, move lower bound
            paginateResultWrapper.lowerBoundColumnValue += operationParams.batchCommitSize;
        }
        // update recovery state + db_id value
        recoveryValue.setObjectName(currentTableName);
        recoveryValue.setState(MigrateState.SECONDARY_INDEX_STARTED);
        recoveryValue.setColumnName(BASE_COLUMN_NAME);
        recoveryValue.setLastColumnValue(paginateResultWrapper.lowerBoundColumnValue);
        shardRecoveryDao.updateShardRecovery(sourceConnect, recoveryValue);
        sourceConnect.commit(); // commit latest records if any
        return rows != 0;
    }

    private void assignMainBottomTopSelectSql(TableOperationParams operationParams) throws IllegalAccessException {
        if (ShardConstants.BLOCK_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
                "select ID, HEIGHT, DB_ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(max(DB_ID), 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else if (ShardConstants.TRANSACTION_INDEX_TABLE_NAME.equalsIgnoreCase(operationParams.tableName)) {
            sqlToExecuteWithPaging =
                "select ID, FULL_HASH, HEIGHT, TRANSACTION_INDEX, DB_ID from transaction where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                "select DB_ID + 1 as DB_ID from transaction where block_timestamp < (SELECT \"TIMESTAMP\" from BLOCK where HEIGHT = ?) order by block_timestamp desc, transaction_index desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from TRANSACTION";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'BLOCK_INDEX' OR 'TRANSACTION_SHARD_INDEX' is expected. Pls use another Helper class");
        }
    }

    private void extractMetaDataCreateInsert(Connection targetConnect, ResultSet resultSet) throws SQLException {
        Objects.requireNonNull(targetConnect, "targetConnect is NULL");
        Objects.requireNonNull(resultSet, "resultSet is NULL");
        if (rsmd == null) {
            rsmd = resultSet.getMetaData();
            numColumns = rsmd.getColumnCount();
            columnTypes = new int[numColumns];
            for (int i = 0; i < numColumns; i++) {
                columnTypes[i] = rsmd.getColumnType(i + 1);
            }
            if (ShardConstants.BLOCK_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
                sqlInsertString.append("insert into BLOCK_INDEX (block_id, block_height)")
                    .append(" values (").append("?, ?").append(")");
            } else if (ShardConstants.TRANSACTION_INDEX_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
                sqlInsertString.append("insert into TRANSACTION_SHARD_INDEX (transaction_id, partial_transaction_hash, height, transaction_index)")
                    .append(" values (").append("?, ?, ?, ?").append(")");
            }
            // precompile sql
            if (preparedInsertStatement == null) {
                preparedInsertStatement = targetConnect.prepareStatement(sqlInsertString.toString());
                log.trace("Precompiled insert = {}", sqlInsertString);
            }
        }
    }


}