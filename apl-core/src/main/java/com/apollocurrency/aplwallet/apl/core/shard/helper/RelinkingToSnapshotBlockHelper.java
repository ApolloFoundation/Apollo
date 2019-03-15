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
 * Helper class is used for changing/updating linked table's records to point to snapshot Block record.
 *
 * @author yuriy.larin
 */
public class RelinkingToSnapshotBlockHelper implements BatchedPaginationOperation {
    private static final Logger log = getLogger(RelinkingToSnapshotBlockHelper.class);

    private String currentTableName;
    private ResultSetMetaData rsmd;
    private int numColumns;
//    private int[] columnTypes;
//    int targetDbIdColumnIndex = -1;

//    private StringBuilder sqlInsertString = new StringBuilder(500);
//    private StringBuilder columnNames = new StringBuilder();
//    private StringBuilder columnQuestionMarks = new StringBuilder();
//    private StringBuilder columnValues = new StringBuilder();

    private Long totalRowCount = 0L;
    private Long insertedCount = 0L;
    private String BASE_COLUMN_NAME = "DB_ID";
    private PreparedStatement preparedInsertStatement = null;


    public RelinkingToSnapshotBlockHelper() {
    }

    @Override
    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
//        this.columnTypes = null;
//        this.targetDbIdColumnIndex = -1;
//        this.sqlInsertString = new StringBuilder(500);
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
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = operationParams.tableName;

        long startSelect = System.currentTimeMillis();
        String sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set HEIGHT = ? where DB_ID >= ? AND DB_ID <= ? limit ?";
        log.trace(sqlToExecuteWithPaging);
        // select MAX DB_ID
        Long transactionTargetDbID = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight);;
        if (transactionTargetDbID == null) {
            String error = String.format("Not Found MAX height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // select MIN DB_ID
        Long lowerIndex = selectLowerDbId(sourceConnect);

        // turn OFF HEIGHT constraint for specified table
        if (operationParams.tableName.equalsIgnoreCase("GENESIS_PUBLIC_KEY")) {
            issueConstraintUpdateQuery(sourceConnect, "alter table GENESIS_PUBLIC_KEY drop constraint CONSTRAINT_C11");
        } else if (operationParams.tableName.equalsIgnoreCase("PUBLIC_KEY")) {
            issueConstraintUpdateQuery(sourceConnect, "alter table PUBLIC_KEY drop constraint CONSTRAINT_8E8");
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerIndex;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, operationParams.snapshotBlockHeight);
                ps.setLong(2, paginateResultWrapper.limitValue);
                ps.setLong(3, transactionTargetDbID);
                ps.setLong(4, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, operationParams.batchCommitSize));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        }
        log.debug("'{}' = [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);

        // turn ON HEIGHT constraint for specified table
        if (operationParams.tableName.equalsIgnoreCase("GENESIS_PUBLIC_KEY")) {
            issueConstraintUpdateQuery(sourceConnect,
                    "ALTER TABLE GENESIS_PUBLIC_KEY ADD CONSTRAINT IF NOT EXISTS CONSTRAINT_C11 FOREIGN KEY (HEIGHT) REFERENCES block (HEIGHT) ON DELETE CASCADE");
        } else if (operationParams.tableName.equalsIgnoreCase("PUBLIC_KEY")) {
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

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight) throws SQLException {
        String selectTransactionTimestamp =
                "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName + " WHERE HEIGHT < ?";
        Long highDbIdValue = null;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectTransactionTimestamp)) {
            selectStatement.setInt(1, snapshotBlockHeight.intValue());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                highDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Transaction's DB_ID value = {}", highDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding LINKED DB_ID in = " + currentTableName, e);
            throw e;
        }
        return highDbIdValue;
    }

    private Long selectLowerDbId(Connection sourceConnect) throws SQLException {
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
        String selectDbId = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectDbId)) {
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                bottomDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Minimal Transaction DB_ID value = {}", bottomDbIdValue);
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