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
 * Helper class is used for deleting block/transaction data from main database previously copied to shard db.
 *
 * @author yuriy.larin
 */
public class BlockDeleteHelper extends AbstractRelinkUpdateHelper {
//public class BlockDeleteHelper extends AbstractBlockTransactionHelper {
    private static final Logger log = getLogger(BlockDeleteHelper.class);

    public BlockDeleteHelper() {
    }

    @Override
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, operationParams);

        long startSelect = System.currentTimeMillis();

        if ("BLOCK".equalsIgnoreCase(currentTableName)) {
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
        // select upper, bottom DB_ID
        selectUpperBottomValues(sourceConnect, operationParams);

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerBoundIdValue;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.limitValue);
                ps.setLong(2, upperBoundIdValue);
                ps.setLong(3, operationParams.batchCommitSize);
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

        log.debug("Total (with CONSTRAINTS) '{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                 // handle rows here
                if (rsmd == null) {
                    rsmd = rs.getMetaData();
                    if ("BLOCK".equalsIgnoreCase(currentTableName)) {
                        sqlInsertString.append("delete from BLOCK WHERE DB_ID > ? LIMIT ?");
                    }
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = targetConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled delete = {}", sqlInsertString);
                    }
                }
                paginateResultWrapper.limitValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method

                try {
                    preparedInsertStatement.setObject(1, rs.getObject(1));
                    preparedInsertStatement.setObject(2, batchCommitSize);
                    insertedCount += preparedInsertStatement.executeUpdate();
                    log.trace("Deleting '{}' into {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
                } catch (Exception e) {
                    log.error("Failed Deleting '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
                    log.error("Failed Deleting " + currentTableName, e);
                    targetConnect.rollback();
                    throw e;
                }
                rows++;
            }
            totalRowCount += rows;
        }
        log.trace("Total Records '{}': selected = {}, deleted = {}, rows = {}, {}={}", currentTableName,
                totalRowCount, insertedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);

        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }

}