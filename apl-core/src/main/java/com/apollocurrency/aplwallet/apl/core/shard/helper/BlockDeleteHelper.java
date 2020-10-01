/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

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
        Set<Long> excludeDbIds = operationParams.excludeInfo != null ? operationParams.excludeInfo.getNotDeleteDbIds() : Set.of();
        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(2, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams, excludeDbIds));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null /*&& !this.preparedInsertStatement.isClosed()*/) {
                log.trace("preparedInsertStatement will be CLOSED!");
                DbUtils.close(this.preparedInsertStatement);
//                this.preparedInsertStatement.close();
            }
        }
        log.debug("Deleted '{}' = [{} / {}] within {} secs", operationParams.tableName, totalProcessedCount, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);

        log.debug("Total (with CONSTRAINTS) '{}' = [{} / {}] in {} secs", operationParams.tableName, totalProcessedCount, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);
        return totalSelectedRows;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection sourceConnect, TableOperationParams operationParams, Set<Long> excludeDbIds)
        throws SQLException {
        long start = System.currentTimeMillis();
        int rows = 0;
        int processedRows = 0;
        boolean excludeRows = operationParams.excludeInfo != null;
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { // handle rows here
                if (rsmd == null) {
                    // it's called one time only
                    rsmd = rs.getMetaData();
                    if (ShardConstants.BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
//                        sqlInsertString.append("delete from BLOCK WHERE DB_ID >= ? AND DB_ID < ? LIMIT ?");
                        sqlInsertString.append("DELETE FROM block WHERE db_id = ?");
                    } else if (ShardConstants.TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
                        sqlInsertString.append("DELETE tx, us FROM transaction AS tx LEFT JOIN update_status AS us ON tx.id = us.transaction_id WHERE tx.db_id = ?");
                    }
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = sourceConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled delete = {}", sqlInsertString);
                    }
                }
                paginateResultWrapper.lowerBoundColumnValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method
                rows++;
                if (excludeRows // skip transaction db_id
                    && ShardConstants.TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName) // only phased transactions
                    && excludeDbIds.contains(paginateResultWrapper.lowerBoundColumnValue)) {
                    log.trace("Skip excluded '{}' db_id = {}", currentTableName, paginateResultWrapper.lowerBoundColumnValue);
                    continue;
                }
                try {
                    preparedInsertStatement.setObject(1, paginateResultWrapper.lowerBoundColumnValue);
                    processedRows += preparedInsertStatement.executeUpdate();
                    log.trace("Deleting '{}' into {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                } catch (Exception e) {
                    log.error("Failed Deleting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
                    log.error("Failed Deleting " + currentTableName, e);
                    sourceConnect.rollback();
                    throw e;
                }
            }
        }

        totalSelectedRows += rows;
        totalProcessedCount += processedRows;
        log.debug("Total Records '{}': selected = {}, deleted = {}, rows = {}, {}={} in {} ms", currentTableName,
            totalSelectedRows, totalProcessedCount, rows, BASE_COLUMN_NAME,
            paginateResultWrapper.lowerBoundColumnValue, System.currentTimeMillis() - start);

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
        if (ShardConstants.BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging =
                "SELECT db_id FROM block WHERE db_id > ? AND db_id < ? LIMIT ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(MAX(db_id), 0) as db_id FROM block WHERE height = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(MIN(db_id)-1, 0) as db_id FROM block";
            log.trace(sqlSelectBottomBound);
        } else if (ShardConstants.TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            // transaction table queries
            sqlToExecuteWithPaging = "SELECT * FROM transaction WHERE db_id > ? AND db_id < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                "SELECT db_id + 1 as db_id FROM transaction WHERE block_timestamp < (SELECT `TIMESTAMP` FROM block where height = ?) ORDER BY block_timestamp DESC, transaction_index DESC LIMIT 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(MIN(db_id)-1, 0) AS db_id FROM " + currentTableName;
            log.trace(sqlSelectBottomBound);
            sqlDeleteFromBottomBound = "DELETE tx, us FROM transaction AS tx LEFT JOIN update_status AS us ON tx.id = us.transaction_id WHERE tx.db_id > ? AND tx.db_id < ?";
            log.trace(sqlDeleteFromBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Block' is expected. Pls use another Helper class");
        }
    }

}