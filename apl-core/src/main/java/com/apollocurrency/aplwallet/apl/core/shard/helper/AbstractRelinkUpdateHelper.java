package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

public abstract class AbstractRelinkUpdateHelper extends AbstractHelper {
    private static final Logger log = getLogger(AbstractRelinkUpdateHelper.class);

    protected void checkMandatoryParameters(Connection sourceConnect, TableOperationParams operationParams) {
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        if (!operationParams.shardId.isPresent()) {
            String error = "Error, Optional shardId is not present";
            log.error(error);
            throw new IllegalArgumentException(error);
        }
        currentTableName = operationParams.tableName;
    }

    protected void selectLowerAndUpperBoundValues(Connection sourceConnect, TableOperationParams operationParams) throws SQLException {
        upperBoundIdValue = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight, sqlSelectUpperBound);
        if (upperBoundIdValue == null) {
            String error = String.format("Not Found MAX height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // select MIN DB_ID
        lowerBoundIdValue = selectLowerDbId(sourceConnect, sqlSelectBottomBound);
        log.debug("'{}' bottomBound = {}, upperBound = {}", currentTableName, lowerBoundIdValue, upperBoundIdValue);
    }

}
