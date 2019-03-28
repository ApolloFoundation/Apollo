/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 *
 * @author yuriy.larin
 */
@Deprecated
public class BlockSelectAndInsertHelper extends BlockTransactionInsertHelper {
    private static final Logger log = getLogger(BlockSelectAndInsertHelper.class);

    public BlockSelectAndInsertHelper() {
    }

/*
    @Override
    public long processOperation(Connection sourceConnect, Connection targetConnect,
                                 TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);

        checkMandatoryParameters(sourceConnect, targetConnect, operationParams);
        recoveryValue = shardRecoveryDao.getLatestShardRecovery();
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

    private void assignMainBottomTopSelectSql() throws IllegalAccessException {
        if (BLOCK_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "SELECT * FROM BLOCK WHERE DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound = "SELECT IFNULL(DB_ID, 0) as DB_ID from BLOCK where HEIGHT = ?";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from BLOCK";
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Block' is expected. Pls use another Helper class");
        }
    }

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
                  }
*/

}