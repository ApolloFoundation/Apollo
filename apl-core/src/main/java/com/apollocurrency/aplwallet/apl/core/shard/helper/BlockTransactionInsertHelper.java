/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.BLOCK_TABLE_NAME;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.TRANSACTION_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 *
 * @author yuriy.larin
 */
public class BlockTransactionInsertHelper extends AbstractHelper {
    private static final Logger log = getLogger(BlockTransactionInsertHelper.class);

    private String sqlDeleteFromBottomBound;

    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);

        checkMandatoryParameters(sourceConnect, targetConnect, operationParams);
        recoveryValue = shardRecoveryDao.getLatestShardRecovery(sourceConnect);
        // check previous state is correct
        assignMainBottomTopSelectSql(); // define all SQLs
        // select DB_ID for target HEIGHT
        this.upperBoundIdValue = selectUpperBoundValue(sourceConnect, operationParams); // select block upper dbId value

        if (restoreLowerBoundIdOrSkipTable(sourceConnect, operationParams, recoveryValue)) {
            return totalSelectedRows; // skip current table
        }

        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);
        // do selection and insertion process
        long startSelect = doStartSelectAndInsert(sourceConnect, targetConnect, operationParams);

        log.debug("'{}' inserted records [{}] in {} secs", operationParams.tableName, totalSelectedRows, (System.currentTimeMillis() - startSelect) / 1000);
        return totalSelectedRows;
    }

    protected long doStartSelectAndInsert(Connection sourceConnect, Connection targetConnect,
                                          TableOperationParams operationParams) throws SQLException {

        // clean up previously stored values from latest saved recovery
        if (lowerBoundIdValue != null && lowerBoundIdValue > 0) {
            try (PreparedStatement deletePs = targetConnect.prepareStatement(sqlDeleteFromBottomBound)) {
                deletePs.setLong(1, lowerBoundIdValue);
                deletePs.setLong(2, upperBoundIdValue);
                int deleted = deletePs.executeUpdate();
                sourceConnect.commit();
                log.trace("Previous DELETE '{}', lower > {} AND upper < {}, deleted = {}",
                        currentTableName, lowerBoundIdValue, upperBoundIdValue, deleted);
            } catch (Exception e) {
                log.error("Previous DELETE failed, Table " + currentTableName, e);
                throw e;
            }
        }

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.lowerBoundColumnValue = lowerBoundIdValue;
        paginateResultWrapper.upperBoundColumnValue = upperBoundIdValue;

        long startSelect = System.currentTimeMillis();

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            // select loop
            do {
                ps.setLong(1, paginateResultWrapper.lowerBoundColumnValue);
                ps.setLong(2, paginateResultWrapper.upperBoundColumnValue);
                ps.setLong(3, operationParams.batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, targetConnect, operationParams));
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

    protected boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                      Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws SQLException {
        int rows = 0;
        int processedRows = 0;
        boolean excludeRows = operationParams.dbIdsExclusionSet.isPresent();
        try (ResultSet rs = ps.executeQuery()) {
            log.trace("SELECT...from {} where DB_ID > {} AND DB_ID < {} LIMIT {}",
                    operationParams.tableName,
                    paginateResultWrapper.lowerBoundColumnValue, paginateResultWrapper.upperBoundColumnValue,
                    operationParams.batchCommitSize);
            while (rs.next()) {
                // execute one time
                extractMetaDataCreateInsert(targetConnect, rs);
                formatBindValues(rs);// format readable values
                rows++;
                paginateResultWrapper.lowerBoundColumnValue = rs.getLong(BASE_COLUMN_NAME); // assign latest value for usage outside method
                if (excludeRows && operationParams.dbIdsExclusionSet.get().contains(rs.getLong(BASE_COLUMN_NAME))) {
                    continue;
                }
                try {
                    for (int i = 0; i < numColumns; i++) {
                        Object object = rs.getObject(i + 1);
                        columnValues.append(object).append(", ");
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                    processedRows += preparedInsertStatement.executeUpdate();
                    log.trace("Inserting '{}' into {} : next value {}={}", rows, currentTableName, BASE_COLUMN_NAME,
                            paginateResultWrapper.lowerBoundColumnValue);
                } catch (Exception e) {
                    log.error("Failed Inserting '{}' into {}, {}", rows, currentTableName, paginateResultWrapper);
                    log.error("Failed inserting = {}, value={}", currentTableName, columnValues);
                    targetConnect.rollback();
                    throw e;
                }
            }
            totalSelectedRows += rows;
            totalProcessedCount += processedRows;
            columnValues.setLength(0);
        }
        log.trace("Total Records: selected = {}, inserted = {}, rows = {}, {}={}",
                totalSelectedRows, totalProcessedCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.lowerBoundColumnValue);
        if (rows == 1) {
            // in case we have only 1 RECORD selected, move lower bound
            paginateResultWrapper.lowerBoundColumnValue += operationParams.batchCommitSize;
        }

        // update recovery state + db_id value
        recoveryValue.setObjectName(currentTableName);
        recoveryValue.setState(DATA_COPY_TO_SHARD_STARTED);
        recoveryValue.setColumnName(BASE_COLUMN_NAME);
        recoveryValue.setLastColumnValue(paginateResultWrapper.lowerBoundColumnValue);
        shardRecoveryDao.updateShardRecovery(sourceConnect, recoveryValue);
        sourceConnect.commit(); // commit recovery info
        targetConnect.commit(); // commit latest copied records if any
        return rows != 0;// || paginateResultWrapper.lowerBoundColumnValue < paginateResultWrapper.upperBoundColumnValue;
    }

    private void extractMetaDataCreateInsert(Connection targetConnect, ResultSet rs) throws SQLException {
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
    }

    private void formatBindValues(ResultSet rs) throws SQLException {
        for (int i = 0; i < numColumns; i++) {
            java.util.Date d = null;
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
                        columnValues.append("TO_DATE('").append(super.dateFormat.format(d)).append("', 'YYYY/MM/DD HH24:MI:SS')");
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
        }
    }

    private void assignMainBottomTopSelectSql() throws IllegalAccessException {
        if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "SELECT * FROM BLOCK WHERE DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
            sqlDeleteFromBottomBound = "DELETE from BLOCK WHERE DB_ID > ? AND DB_ID < ?";
            log.trace(sqlDeleteFromBottomBound);
        } else if (TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "select * from transaction where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                    "select DB_ID + 1 as DB_ID from transaction where block_timestamp < (SELECT TIMESTAMP from BLOCK where HEIGHT = ?) order by block_timestamp desc, transaction_index desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
            log.trace(sqlSelectBottomBound);
            sqlDeleteFromBottomBound = "DELETE from TRANSACTION WHERE  DB_ID > ? AND DB_ID < ?";
            log.trace(sqlDeleteFromBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. Either 'Block' or 'Transaction' is expected. Pls use another Helper class");
        }
    }

}
