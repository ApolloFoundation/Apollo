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
public class TransactionSelectAndInsertHelper extends BlockTransactionInsertHelper {
    private static final Logger log = getLogger(TransactionSelectAndInsertHelper.class);

    public TransactionSelectAndInsertHelper() {
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
        assignMainBottomTopSelectSql();
        // select DB_ID for target HEIGHT
        this.upperBoundIdValue = selectUpperBoundValue(sourceConnect, operationParams);

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
        if (TRANSACTION_TABLE_NAME.equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "select * from transaction where DB_ID > ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                    "select DB_ID from transaction where block_timestamp < (SELECT TIMESTAMP from BLOCK where HEIGHT = ?) order by block_timestamp desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Transaction' is expected. Pls use another Helper class");
        }
    }
*/

}