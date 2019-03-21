/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.GENESIS_PUBLIC_KEY_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Helper class is used for changing/updating linked table's records to point to snapshot Block record.
 *
 * @author yuriy.larin
 */
public class RelinkingToSnapshotBlockHelper extends AbstractRelinkUpdateHelper {
    private static final Logger log = getLogger(RelinkingToSnapshotBlockHelper.class);

    public RelinkingToSnapshotBlockHelper() {
    }

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        currentTableName = operationParams.tableName;

        long startSelect = System.currentTimeMillis();
        sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set HEIGHT = ? where DB_ID > ? AND DB_ID < ? limit ?";
        log.trace(sqlToExecuteWithPaging);
        sqlSelectUpperBound = "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName + " WHERE HEIGHT < ?";
        log.trace(sqlSelectUpperBound);
        sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        log.trace(sqlSelectBottomBound);

        // select upper, bottom DB_ID
        selectLowerAndUpperBoundValues(sourceConnect, operationParams);

        // turn OFF HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "alter table GENESIS_PUBLIC_KEY drop constraint CONSTRAINT_C11");
            executeUpdateQuery(sourceConnect, "alter table GENESIS_PUBLIC_KEY drop primary key");
            executeUpdateQuery(sourceConnect, "drop index GENESIS_PUBLIC_KEY_ACCOUNT_ID_HEIGHT_IDX");
            executeUpdateQuery(sourceConnect, "drop index GENESIS_PUBLIC_KEY_HEIGHT_IDX");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect, "alter table PUBLIC_KEY drop constraint CONSTRAINT_8E8");
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;

       try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, operationParams.snapshotBlockHeight);
                ps.setLong(2, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(3, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(4, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams.batchCommitSize));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        }
        log.debug("'{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);

        // turn ON HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                    "ALTER TABLE GENESIS_PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_C11 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
            executeUpdateQuery(sourceConnect,
                    "ALTER TABLE GENESIS_PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS PRIMARY_KEY_GENESIS_PUBLIC_KEY primary key (DB_ID)");
            executeUpdateQuery(sourceConnect,
                    "CREATE UNIQUE INDEX IF NOT EXISTS genesis_public_key_account_id_height_idx on genesis_public_key(account_id, height)");
            executeUpdateQuery(sourceConnect,
                    "CREATE INDEX IF NOT EXISTS genesis_public_key_height_idx on genesis_public_key(height)");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            executeUpdateQuery(sourceConnect,
                    "ALTER TABLE PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_8E8 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
        }
        log.debug("'{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
        try {
            rows = ps.executeUpdate();
            log.trace("Updated rows = '{}' in '{}' : column/value {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
        } catch (Exception e) {
            log.error("Failed Updating '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
            log.error("Failed Updating " + currentTableName, e);
            targetConnect.rollback();
            throw e;
        }
        log.trace("Total Records: updated = {}, rows = {}, {}={}",
                totalRowCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);

        totalRowCount += rows;
        paginateResultWrapper.lowerBoundColumnValue += batchCommitSize;
        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }


}