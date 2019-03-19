/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;

import org.slf4j.Logger;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 *
 * @author yuriy.larin
 */
public class TransactionSelectAndInsertHelper extends AbstractBlockTransactionHelper {
    private static final Logger log = getLogger(TransactionSelectAndInsertHelper.class);

    public TransactionSelectAndInsertHelper() {
    }

    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, targetConnect, operationParams);

        if ("transaction".equalsIgnoreCase(currentTableName)) {
            sqlToExecuteWithPaging = "select * from transaction where DB_ID >= ? AND DB_ID < ? limit ?";
            log.trace(sqlToExecuteWithPaging);
            sqlSelectUpperBound =
                    "select DB_ID from transaction where block_timestamp <= (SELECT TIMESTAMP from BLOCK where HEIGHT = ?) order by block_timestamp desc limit 1";
            log.trace(sqlSelectUpperBound);
            sqlSelectBottomBound = "SELECT IFNULL(min(DB_ID), 0) as DB_ID from " + currentTableName;
            log.trace(sqlSelectBottomBound);
        } else {
            throw new IllegalAccessException("Unsupported table. 'Transaction' is expected. Pls use another Helper class");
        }
        // select DB_ID for target HEIGHT
        upperBoundIdValue = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight, sqlSelectUpperBound);
        if (upperBoundIdValue == null) {
            String error = String.format("Not Found Transaction's DB_ID at snapshot Block height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // do selection and insertion process
        long startSelect = doStartSelectAndInsert(sourceConnect, targetConnect, operationParams);

        log.debug("'{}' inserted records [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

}