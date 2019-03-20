/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 *
 * @author yuriy.larin
 */
public class BlockSelectAndInsertHelper extends AbstractBlockTransactionHelper {
    private static final Logger log = getLogger(BlockSelectAndInsertHelper.class);

    public BlockSelectAndInsertHelper() {
    }

    @Override
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                                      TableOperationParams operationParams)
            throws Exception {
        log.debug("Processing: {}", operationParams);
        checkMandatoryParameters(sourceConnect, targetConnect, operationParams);

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
            String error = String.format("Not Found Block's DB_ID at snapshot Block height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // do selection and insertion process
        long startSelect = doStartSelectAndInsert(sourceConnect, targetConnect, operationParams);

        log.debug("'{}' inserted records [{}] in {} secs", operationParams.tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

}