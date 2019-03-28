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
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import org.slf4j.Logger;

/**
 * Common fields and methods used by inherited classes.
 */
public abstract class AbstractHelper implements BatchedPaginationOperation {
    private static final Logger log = getLogger(AbstractHelper.class);

    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    protected ShardRecoveryDaoJdbc shardRecoveryDao;

    String currentTableName; // processed table name
    ResultSetMetaData rsmd; // for internal usage
    int numColumns;
    int[] columnTypes;

    StringBuilder sqlInsertString = new StringBuilder(500);
    StringBuilder columnNames = new StringBuilder();
    StringBuilder columnQuestionMarks = new StringBuilder();
    StringBuilder columnValues = new StringBuilder();

    Long totalSelectedRows = 0L;
    Long totalProcessedCount = 0L;
    String BASE_COLUMN_NAME = "DB_ID";
    PreparedStatement preparedInsertStatement = null;

    String sqlToExecuteWithPaging;
    String sqlSelectUpperBound;
    String sqlSelectBottomBound;
    Long upperBoundIdValue;
    Long lowerBoundIdValue;
    ShardRecovery recoveryValue; // updated on every loop

    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
        this.columnTypes = null;
        this.sqlInsertString = new StringBuilder(500);
        this.columnNames = new StringBuilder();
        this.columnQuestionMarks = new StringBuilder();
        this.columnValues = new StringBuilder();
        this.totalSelectedRows = 0L;
        this.totalProcessedCount = 0L;
        this.preparedInsertStatement = null;
        this.sqlSelectUpperBound = null;
        this.sqlSelectBottomBound = null;
        this.upperBoundIdValue = null;
        this.lowerBoundIdValue = null;
        this.recoveryValue = null;
    }

    public abstract long processOperation(Connection sourceConnect, Connection targetConnect,
                                          TableOperationParams operationParams) throws Exception;

    @Override
    public void setShardRecoveryDao(ShardRecoveryDaoJdbc dao) {
        this.shardRecoveryDao = Objects.requireNonNull(dao, "shard Recovery Dao is NULL");;
    }

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

    protected void checkMandatoryParameters(Connection sourceConnect, Connection targetConnect, TableOperationParams operationParams) {
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(targetConnect, "targetConnect is NULL");
        Objects.requireNonNull(operationParams.tableName, "tableName is NULL");
        Objects.requireNonNull(operationParams.snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = operationParams.tableName;
    }

    protected Long selectUpperBoundValue(Connection sourceConnect, TableOperationParams operationParams) throws SQLException {
        Long upperBoundIdValue = selectUpperDbId(sourceConnect, operationParams.snapshotBlockHeight, sqlSelectUpperBound);
        if (upperBoundIdValue == null) {
            String error = String.format("Not Found MAX height = %s", operationParams.snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        return upperBoundIdValue;
    }

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight, String selectValueSql) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(snapshotBlockHeight, "snapshot Block Height is NULL");
        Objects.requireNonNull(selectValueSql, "selectValueSql is NULL");
        // select DB_ID for target HEIGHT
        Long highDbIdValue = null;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
            selectStatement.setInt(1, snapshotBlockHeight.intValue());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                highDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Upper DB_ID value = {}", highDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding Upper DB_ID by snapshot block height = " + snapshotBlockHeight, e);
            throw e;
        }
        return highDbIdValue;
    }

    protected Long selectLowerBoundValue(Connection sourceConnect) throws SQLException {
        // select MIN DB_ID
        return selectLowerDbId(sourceConnect, sqlSelectBottomBound);
    }

    private Long selectLowerDbId(Connection sourceConnect, String selectValueSql) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(selectValueSql, "selectValueSql is NULL");
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(sqlSelectBottomBound)) {
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                bottomDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Bottom DB_ID value = {}", bottomDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding Bottom DB_ID", e);
            throw e;
        }
        return bottomDbIdValue;
    }

    protected void executeUpdateQuery(Connection sourceConnect, String sqlToExecute) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(sqlToExecute, "sqlToExecute is NULL");
        try (Statement stmt = sourceConnect.createStatement()) {
            stmt.executeUpdate(sqlToExecute);
            sourceConnect.commit();
            log.debug("SUCCESS, on execution constraint SQL = {}", sqlToExecute);
        } catch (Exception e) {
            sourceConnect.rollback();
            log.error("Error on 'constraint related' SQL = " + currentTableName, e);
            throw e;
        }
    }

    protected boolean restoreLowerBoundIdOrSkipTable(Connection sourceConnect, TableOperationParams operationParams,
                                                     ShardRecovery recoveryValue) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(operationParams, "operationParams is NULL");
        Objects.requireNonNull(recoveryValue, "recoveryValue is NULL");
        if (recoveryValue.getObjectName() == null || recoveryValue.getObjectName().isEmpty()) {
            // first time run for only first table in list
            this.lowerBoundIdValue = selectLowerBoundValue(sourceConnect);
            log.debug("START object '{}' from = {}", operationParams.tableName, recoveryValue);

        } else if (recoveryValue.getProcessedObject() != null && !recoveryValue.getProcessedObject().isEmpty()
                && isContain(recoveryValue.getProcessedObject(), currentTableName)) {
            // skip current table
            log.debug("SKIP object '{}' by = {}", operationParams.tableName, recoveryValue);
            return true;
        } else {
            if (recoveryValue.getObjectName() != null
                    && recoveryValue.getObjectName().equalsIgnoreCase(currentTableName)
                    && !isContain(recoveryValue.getProcessedObject(), currentTableName)) {
                // process current table because it was not finished
                this.lowerBoundIdValue = recoveryValue.getLastColumnValue() + 1; // last saved/processed value + 1
                log.debug("RESTORED object '{}' from = {}", operationParams.tableName, recoveryValue);
            } else {
                // started next new table
                this.lowerBoundIdValue = selectLowerBoundValue(sourceConnect);
                log.debug("GO NEXT object '{}' by = {}", operationParams.tableName, recoveryValue);
            }
        }
        return false;
    }

    /**
     * Method to match existing table name as full name but not a substring
     * @param sourceString source string to find in
     * @param itemToFind what to be found
     * @return true if full separate itemToFind exists
     */
    public static boolean isContain(String sourceString, String itemToFind){
        if (sourceString == null || sourceString.isEmpty()) return false;
        if (itemToFind == null || itemToFind.isEmpty()) return false;
        String pattern = "\\b"+itemToFind+"\\b";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(sourceString);
        return m.find();
    }

}
