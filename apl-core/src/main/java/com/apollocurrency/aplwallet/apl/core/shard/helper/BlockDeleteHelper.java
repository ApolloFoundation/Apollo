/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

/**
 * Helper class is used for deleting block/transaction data from main database previously copied to shard db.
 *
 * @author yuriy.larin
 */
public class BlockDeleteHelper extends AbstractHelper {
    private static final Logger log = getLogger(BlockDeleteHelper.class);

    public BlockDeleteHelper() {
    }

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, operationParams);

        long startSelect = System.currentTimeMillis();

        assignMainBottomTopSelectSql();
        // select upper, bottom DB_ID
        this.upperBoundIdValue = selectUpperBoundValue(sourceConnect, operationParams);

        recoveryValue = shardRecoveryDao.getLatestShardRecovery(sourceConnect);

        if (restoreLowerBoundIdOrSkipTable(sourceConnect, operationParams, recoveryValue)) {
            return totalSelectedRows; // skip current table
        }
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(2, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null && !this.preparedInsertStatement.isClosed()) {
                this.preparedInsertStatement.close();
            }
        }
        log.debug("Deleted '{}' = [{}] within {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);

        log.debug("Total (with CONSTRAINTS) '{}' = [{}] in {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);
        return totalSelectedRows;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection sourceConnect, TableOperationParams operationParams)
            throws SQLException {
        int rows = 0;
        int processedRows = 0;
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { // handle rows here
                if (rsmd == null) {
                    // it's called one time only
                    rsmd = rs.getMetaData();
                    if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
                        sqlInsertString.append("delete from BLOCK WHERE DB_ID < ? LIMIT ?");
                    }
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = sourceConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled delete = {}", sqlInsertString);
                    }
                }

                try {
                    preparedInsertStatement.setObject(1, paginateResultWrapper.upperBoundColumnValue);
                    preparedInsertStatement.setObject(2, operationParams.batchCommitSize);
                    processedRows = preparedInsertStatement.executeUpdate();
                    log.debug("Deleting '{}' into {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                } catch (Exception e) {
                    log.error("Failed Deleting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                    log.error("Failed Deleting " + currentTableName, e);
                    sourceConnect.rollback();
                    throw e;
                }
                rows++;
            }
            totalSelectedRows += rows;
            totalProcessedCount += processedRows;
        }
        log.trace("Total Records '{}': selected = {}, deleted = {}, rows = {}, {}={}", currentTableName,
                totalSelectedRows, totalProcessedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);

        if (rows == 1) {
            // in case we have only 1 RECORD selected, move lower bound
            // move lower bound
            paginateResultWrapper.lowerBoundColumnValue += operationParams.batchCommitSize;
        }
        // update recovery state + db_id value
        recoveryValue.setObjectName(currentTableName);
        recoveryValue.setState(MigrateState.DATA_REMOVE_STARTED);
        recoveryValue.setColumnName(BASE_COLUMN_NAME);
        recoveryValue.setLastColumnValue(paginateResultWrapper.lowerBoundColumnValue);
        shardRecoveryDao.updateShardRecovery(sourceConnect, recoveryValue);
        sourceConnect.commit(); // commit latest records if any
        return rows != 0;
    }

    private void assignMainBottomTopSelectSql() throws IllegalAccessException {
        if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
                    "select DB_ID from BLOCK where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Block' is expected. Pls use another Helper class");
        }
    }

}