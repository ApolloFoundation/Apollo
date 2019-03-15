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
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 *
 * @author yuriy.larin
 */
public class BlockSelectAndInsertHelper implements BatchedPaginationOperation {
    private static final Logger log = getLogger(BlockSelectAndInsertHelper.class);
//    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private String currentTableName;
    private ResultSetMetaData rsmd;
    private int numColumns;
    private int[] columnTypes;
    int targetDbIdColumnIndex = -1;

    private StringBuilder sqlInsertString = new StringBuilder(500);
    private StringBuilder columnNames = new StringBuilder();
    private StringBuilder columnQuestionMarks = new StringBuilder();
    private StringBuilder columnValues = new StringBuilder();

    private Long totalRowCount = 0L;
    private Long insertedCount = 0L;
    private String BASE_COLUMN_NAME = "DB_ID";
    private PreparedStatement preparedInsertStatement = null;


    public BlockSelectAndInsertHelper() {
    }

    @Override
    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
        this.columnTypes = null;
        this.targetDbIdColumnIndex = -1;
        this.sqlInsertString = new StringBuilder(500);
        this.columnNames = new StringBuilder();
        this.columnQuestionMarks = new StringBuilder();
        this.columnValues = new StringBuilder();
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
        Objects.requireNonNull(targetConnect, "targetConnect is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = operationParams.tableName;

        String sqlToExecuteWithPaging;
        String sqlSelectUpperBound = null;
        String sqlSelectBottomBound = null;
        Long upperBoundIdValue = null;
        Long lowerBoundIdValue = null;

        if ("block".equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "SELECT * FROM BLOCK WHERE DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Block' is expected. Pls use another Helper class");
        }
        // select DB_ID for target HEIGHT
        upperBoundIdValue = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight, sqlSelectUpperBound);
        if (upperBoundIdValue == null) {
            String error = String.format("Not Found target DB_ID at snapshot Block height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        lowerBoundIdValue = selectLowerDbId(sourceConnect, sqlSelectBottomBound);
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerBoundIdValue;

        long startSelect = System.currentTimeMillis();

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, paginateResultWrapper.limitValue);
                ps.setLong(2, upperBoundIdValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, targetConnect));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        } finally {
            if (this.preparedInsertStatement != null && !this.preparedInsertStatement.isClosed()) {
                this.preparedInsertStatement.close();
            }
        }
        log.debug("'{}' inserted records [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper, Connection targetConnect)
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
                    for (int i = 0; i < numColumns; i++) {
                        columnTypes[i] = rsmd.getColumnType(i + 1);
                        if (i != 0) {
                            columnNames.append(",");
                            columnQuestionMarks.append("?,");
                        }
                        String columnName = rsmd.getColumnName(i + 1);
                        columnNames.append(columnName);
                    }
                    columnQuestionMarks.append("?");
                    sqlInsertString.append("INSERT INTO ").append(currentTableName)
                            .append("(").append(columnNames).append(")").append(" values (").append(columnQuestionMarks).append(")");
                    // precompile sql
                    if (preparedInsertStatement == null) {
                        preparedInsertStatement = targetConnect.prepareStatement(sqlInsertString.toString());
                        log.trace("Precompiled insert = {}", sqlInsertString);
                    }
                }
                paginateResultWrapper.limitValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method

                try {
                    for (int i = 0; i < numColumns; i++) {
                        preparedInsertStatement.setObject(i + 1, rs.getObject(i + 1));
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
        log.trace("Total Records: selected = {}, inserted = {}, rows = {}, {}={}",
                totalRowCount, insertedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);

        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight, String selectValueSql) throws SQLException {
        // select DB_ID for target HEIGHT
        Long highDbIdValue = null;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
            selectStatement.setInt(1, snapshotBlockHeight.intValue());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                highDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Block DB_ID value = {}", highDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding target DB_ID by snapshot block height = " + snapshotBlockHeight, e);
            throw e;
        }
        return highDbIdValue;
    }

    private Long selectLowerDbId(Connection sourceConnect, String selectValueSql) throws SQLException {
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                bottomDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Minimal Block DB_ID value = {}", bottomDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding LOW LIMIT target DB_ID", e);
            throw e;
        }
        return bottomDbIdValue;
    }

/*

        ResultSet rs = null;
        if (tableName.equalsIgnoreCase("block")) {
            rs = selectStmt.executeQuery(sqlToExecuteWithPaging);
        } else {
//            sqlToExecuteWithPaging = "SELECT * FROM " + tableName;
            rs = selectStmt.executeQuery(sqlToExecuteWithPaging);
        }
        log.debug("Select '{}' in {} secs", tableName, (System.currentTimeMillis() - startSelect) / 1000);

        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        int[] columnTypes = new int[numColumns];

        StringBuilder columnNames = new StringBuilder();
        StringBuilder columnQuestionMarks = new StringBuilder();
        StringBuilder columnValues = new StringBuilder();

        java.util.Date d = null;

        int targetBlockIdColumnIndex = -1;
        int targetDbIdColumnIndex = -1;
        for (int i = 0; i < numColumns; i++) {
            columnTypes[i] = rsmd.getColumnType(i + 1);
            if (i != 0) {
                columnNames.append(",");
                columnQuestionMarks.append("?,");
            }
            String columnName = rsmd.getColumnName(i + 1);
            columnNames.append(columnName);
            if (columnName.equalsIgnoreCase("DB_ID")) {
                targetDbIdColumnIndex = i + 1;
            }
        }
        columnQuestionMarks.append("?");

        long startInsert = System.currentTimeMillis();
        boolean continueSelect = false;

        do {

        while (rs.next()) {
            continueSelect = true;

            // no need to synch by StringBuffer implementation
        StringBuilder sqlInsertString = new StringBuilder(8000);
        sqlInsertString.append("INSERT INTO ").append(tableName)
                .append("(").append(columnNames).append(")").append(" values (").append(columnQuestionMarks).append(")");
        if (log.isTraceEnabled()) {
            log.trace(sqlInsertString.toString());
        }
        try (
                // precompile sql
//                PreparedStatement preparedStatement = targetConnect.prepareStatement(sqlInsertString.toString())
                PreparedStatement preparedStatement = targetConnect.prepareStatement(sqlToExecuteWithPaging)
        ) {

                for (int i = 0; i < numColumns; i++) {
                    // bind values

                    switch (columnTypes[i]) {
                        case Types.BIGINT:
                        case Types.BIT:
                        case Types.BOOLEAN:
                        case Types.DECIMAL:
                        case Types.DOUBLE:
                        case Types.FLOAT:
                        case Types.INTEGER:
                        case Types.SMALLINT:
                        case Types.TINYINT:
                            String v = rs.getString(i + 1);
                            columnValues.append(v);
                            break;

                        case Types.DATE:
                            d = rs.getDate(i + 1);
                        case Types.TIME:
                            if (d == null) d = rs.getTime(i + 1);
                        case Types.TIMESTAMP:
                            if (d == null) d = rs.getTimestamp(i + 1);

                            if (d == null) {
                                columnValues.append("null");
                            } else {
                                columnValues.append("TO_DATE('").append(dateFormat.format(d)).append("', 'YYYY/MM/DD HH24:MI:SS')");
                            }
                            break;

                        default:
                            v = rs.getString(i + 1);
                            if (v != null) {
                                columnValues.append("'").append( v.replaceAll("'", "''")).append("'");
                            } else {
                                columnValues.append("null");
                            }
                            break;
                    }
                    if (i != columnTypes.length - 1) {
                        columnValues.append(",");
                    }
                    if (targetDbIdColumnIndex == i + 1) {
                        lowerIndex = rs.getLong(i + 1);
                    }

                    if (targetBlockIdColumnIndex == i + 1) {
                        // BLOCK_ID should be assigned
                        preparedStatement.setObject(i + 1, snapshotBlockHeight.getId());
                        continue;
                    }
                    if (targetDbIdColumnIndex == i + 1) {
                        // HEIGHT should be assigned
                        preparedStatement.setObject(i + 1, snapshotBlockHeight.getHeight());
                        continue;
                    }
                    preparedStatement.setObject(i + 1, rs.getObject(i + 1));
                }

                insertedBeforeBatchCount += preparedStatement.executeUpdate();
                if (insertedBeforeBatchCount >= batchCommitSize) {
                    targetConnect.commit();
                    totalInsertedCount += insertedBeforeBatchCount;
                    insertedBeforeBatchCount = 0;
                    log.trace("Partial commit = {}", totalInsertedCount);
                }
                columnValues.setLength(0);
            targetConnect.commit(); // commit latest records if any
            totalInsertedCount += insertedBeforeBatchCount;
            log.debug("Finished '{}' inserted [{}] in = {} sec", tableName, totalInsertedCount, (System.currentTimeMillis() - startInsert) / 1000);
        } catch (Exception e) {
            sqlInsertString = new StringBuilder(8000);
            sqlInsertString.append("INSERT INTO ").append(tableName)
                    .append("(").append(columnNames).append(")").append(" values (").append(columnValues).append(")");

            int errorTotalCount = totalInsertedCount + insertedBeforeBatchCount;
            log.error("Insert error on record count=[" + errorTotalCount + "] by SQL =\n" + sqlInsertString.toString() + "\n", e);
//            log.error("Insert error on record count=[" + errorTotalCount + "] by SQL =\n" + sqlToExecuteWithPaging + "\n", e);
        }
      } // rs.next()
     } while (continueSelect); // do ()
*/

//------------------------------------------------------------------------------
}