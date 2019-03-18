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
 * Common fields and methods used by inherited classes.
 */
public abstract class AbstractBlockTransactionHelper extends AbstractHelper {
    private static final Logger log = getLogger(AbstractBlockTransactionHelper.class);

    protected void checkMandatoryParameters(Connection sourceConnect, Connection targetConnect, TableOperationParams operationParams) {
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(targetConnect, "targetConnect is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = operationParams.tableName;
    }

    protected long doStartSelectAndInsert(Connection sourceConnect, Connection targetConnect, TableOperationParams operationParams) throws SQLException {
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
        return startSelect;
    }

    protected boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper, Connection targetConnect)
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



}
