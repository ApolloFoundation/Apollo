/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.GENESIS_PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

/**
 * Helper class is used for changing/updating linked table's records to point to snapshot Block record.
 *
 * @author yuriy.larin
 */
public class RelinkingToSnapshotBlockHelper extends AbstractHelper {
    private static final Logger log = getLogger(RelinkingToSnapshotBlockHelper.class);

    public RelinkingToSnapshotBlockHelper() {
    }

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect /* targetConnect is NOT used here*/,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        currentTableName = operationParams.tableName;

        long startSelect = System.currentTimeMillis();
        assignMainBottomTopSelectSql();
        recoveryValue = shardRecoveryDao.getLatestShardRecovery(sourceConnect);

        // select upper, bottom DB_ID
        this.upperBoundIdValue = selectUpperBoundValue(sourceConnect, operationParams);

        if (restoreLowerBoundIdOrSkipTable(sourceConnect, operationParams, recoveryValue)) {
            return totalSelectedRows; // skip current table
        }
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);

        // turn OFF HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "alter table IF EXISTS GENESIS_PUBLIC_KEY drop constraint IF EXISTS CONSTRAINT_C11");
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS GENESIS_PUBLIC_KEY_ACCOUNT_ID_HEIGHT_IDX");
            executeUpdateQuery(sourceConnect, "drop index IF EXISTS GENESIS_PUBLIC_KEY_HEIGHT_IDX");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "alter table PUBLIC_KEY drop constraint IF EXISTS CONSTRAINT_8E8");
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;
        try {
            if (TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName) && operationParams.dbIdsExclusionSet.isPresent()) {
                Set<Long> dbIds = operationParams.dbIdsExclusionSet.get();
                long blockId;
                try (PreparedStatement ps = sourceConnect.prepareStatement("select id from block where height = ? ")) {
                    ps.setLong(1, operationParams.snapshotBlockHeight);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            blockId = rs.getLong("id");
                        } else {
                            throw new IllegalStateException("Id of snapshot not found");
                        }
                    }
                }
                try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
                    int counter = 0;
                    for (long dbId : dbIds) {
                        ps.setLong(1, blockId);
                        ps.setLong(2, dbId);
                        ps.addBatch();
                        if (++counter % operationParams.batchCommitSize == 0) {
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                    log.debug("Relinked {} transactions to snapshot block at height {}", counter, operationParams.snapshotBlockHeight);
                }
            } else {
                try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
                    do {
                        ps.setLong(1, operationParams.snapshotBlockHeight);
                        ps.setLong(2, paginateResultWrapper.lowerBoundColumnValue);
                        ps.setLong(3, paginateResultWrapper.upperBoundColumnValue);
                        ps.setLong(4, operationParams.batchCommitSize);
                    } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams.batchCommitSize));
                }
            }
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        }
        // turn ON HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                    "ALTER TABLE GENESIS_PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_C11 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
            executeUpdateQuery(sourceConnect,
                    "CREATE UNIQUE INDEX IF NOT EXISTS genesis_public_key_account_id_height_idx on genesis_public_key(account_id, height)");
            executeUpdateQuery(sourceConnect,
                    "CREATE INDEX IF NOT EXISTS genesis_public_key_height_idx on genesis_public_key(height)");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                    "ALTER TABLE PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_8E8 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
        }
        log.debug("'{}' = [{}] in {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);
        return totalSelectedRows;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection sourceConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
        try {
            rows = ps.executeUpdate();
            log.trace("Updated rows = '{}' in '{}' : column/value {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
        } catch (Exception e) {
            log.error("Failed Updating '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
            log.error("Failed Updating " + currentTableName, e);
            sourceConnect.rollback();
            throw e;
        }
        log.trace("Total Records: updated = {}, rows = {}, {}={}",
                totalSelectedRows, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);

        totalSelectedRows += rows;
        paginateResultWrapper.lowerBoundColumnValue += batchCommitSize;
        // update recovery state + db_id value
        recoveryValue.setObjectName(currentTableName);
        recoveryValue.setState(MigrateState.DATA_RELINK_STARTED);
        recoveryValue.setColumnName(BASE_COLUMN_NAME);
        recoveryValue.setLastColumnValue(paginateResultWrapper.lowerBoundColumnValue);
        shardRecoveryDao.updateShardRecovery(sourceConnect, recoveryValue); // update recovery info
        sourceConnect.commit(); // commit latest records if any

        return rows != 0 || paginateResultWrapper.lowerBoundColumnValue < paginateResultWrapper.upperBoundColumnValue;
    }

    private void assignMainBottomTopSelectSql() {
        if (TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set block_id = ? where db_id = ? ";
        } else {
            sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set HEIGHT = ? where DB_ID > ? AND DB_ID < ? limit ?";
        }
        log.trace(sqlToExecuteWithPaging);
        sqlSelectUpperBound = "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName + " WHERE HEIGHT <= ?";
        log.trace(sqlSelectUpperBound);
        sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        log.trace(sqlSelectBottomBound);
    }

}