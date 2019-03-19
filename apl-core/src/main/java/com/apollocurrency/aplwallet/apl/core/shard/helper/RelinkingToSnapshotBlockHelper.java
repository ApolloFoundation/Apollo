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
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = operationParams.tableName;

        long startSelect = System.currentTimeMillis();
        sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set HEIGHT = ? where DB_ID >= ? AND DB_ID <= ? limit ?";
        log.trace(sqlToExecuteWithPaging);
        sqlSelectUpperBound = "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName + " WHERE HEIGHT < ?";
        log.trace(sqlSelectUpperBound);
        sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        log.trace(sqlSelectBottomBound);

        // select upper, bottom DB_ID
        selectUpperBottomValues(sourceConnect, operationParams);

        // turn OFF HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect, "alter table GENESIS_PUBLIC_KEY drop constraint CONSTRAINT_C11");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect, "alter table PUBLIC_KEY drop constraint CONSTRAINT_8E8");
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, operationParams.snapshotBlockHeight);
                ps.setLong(2, paginateResultWrapper.limitValue);
                ps.setLong(3, upperBoundIdValue);
                ps.setLong(4, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams.batchCommitSize));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        }
        log.debug("'{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);

        // turn ON HEIGHT constraint for specified table
        if (GENESIS_PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect,
                    "ALTER TABLE GENESIS_PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_C11 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
        } else if (PUBLIC_KEY_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            issueConstraintUpdateQuery(sourceConnect,
                    "ALTER TABLE PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_8E8 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
        }

        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
        try {
            rows = ps.executeUpdate();
            log.trace("Updated rows = '{}' in '{}' : column/value {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
        } catch (Exception e) {
            log.error("Failed Updating '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
            log.error("Failed Updating " + currentTableName, e);
            targetConnect.rollback();
            throw e;
        }
        log.trace("Total Records: updated = {}, rows = {}, {}={}",
                totalRowCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);

        totalRowCount += rows;
        paginateResultWrapper.limitValue += batchCommitSize;
        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }


}