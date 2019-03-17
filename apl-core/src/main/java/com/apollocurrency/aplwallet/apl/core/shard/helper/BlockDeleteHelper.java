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
 * Helper class is used for deleting block/transaction data from main database previously copied to shard db.
 *
 * @author yuriy.larin
 */
public class BlockDeleteHelper implements BatchedPaginationOperation {
    private static final Logger log = getLogger(BlockDeleteHelper.class);

    private String currentTableName;
    private ResultSetMetaData rsmd;
//    private int numColumns;
//    private int[] columnTypes;
//    int targetDbIdColumnIndex = -1;

    private StringBuilder sqlInsertString = new StringBuilder(500);
//    private StringBuilder columnNames = new StringBuilder();
//    private StringBuilder columnQuestionMarks = new StringBuilder();
//    private StringBuilder columnValues = new StringBuilder();

    private Long totalRowCount = 0L;
    private Long insertedCount = 0L;
    private String BASE_COLUMN_NAME = "DB_ID";
    private PreparedStatement preparedInsertStatement = null;


    public BlockDeleteHelper() {
    }

    @Override
    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
//        this.numColumns = -1;
//        this.columnTypes = null;
//        this.targetDbIdColumnIndex = -1;
        this.sqlInsertString = new StringBuilder(500);
//        this.columnNames = new StringBuilder();
//        this.columnQuestionMarks = new StringBuilder();
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

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight, String selectValueSql) throws SQLException {
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