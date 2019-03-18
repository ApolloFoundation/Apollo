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
public abstract class AbstractHelper implements BatchedPaginationOperation {
    private static final Logger log = getLogger(AbstractHelper.class);

    String currentTableName; // processed table name
    ResultSetMetaData rsmd; // for internal usage
    int numColumns;
    int[] columnTypes;

    StringBuilder sqlInsertString = new StringBuilder(500);
    StringBuilder columnNames = new StringBuilder();
    StringBuilder columnQuestionMarks = new StringBuilder();

    Long totalRowCount = 0L;
    Long insertedCount = 0L;
    String BASE_COLUMN_NAME = "DB_ID";
    PreparedStatement preparedInsertStatement = null;

    String sqlToExecuteWithPaging;
    String sqlSelectUpperBound;
    String sqlSelectBottomBound;
    Long upperBoundIdValue;
    Long lowerBoundIdValue;

    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
        this.columnTypes = null;
        this.sqlInsertString = new StringBuilder(500);
        this.columnNames = new StringBuilder();
        this.columnQuestionMarks = new StringBuilder();
        this.totalRowCount = 0L;
        this.insertedCount = 0L;
        this.preparedInsertStatement = null;
        sqlSelectUpperBound = null;
        sqlSelectBottomBound = null;
        upperBoundIdValue = null;
        lowerBoundIdValue = null;
    }

    public abstract long selectInsertOperation(Connection sourceConnect, Connection targetConnect,
                               TableOperationParams operationParams) throws Exception;


    protected Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight, String selectValueSql) throws SQLException {
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

    protected Long selectLowerDbId(Connection sourceConnect, String selectValueSql) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(selectValueSql, "selectValueSql is NULL");
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectValueSql)) {
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

    protected void issueConstraintUpdateQuery(Connection sourceConnect, String sqlToExecute) throws SQLException {
        Objects.requireNonNull(sourceConnect, "source connection is NULL");
        Objects.requireNonNull(sqlToExecute, "sqlToExecute is NULL");
        try (Statement stmt = sourceConnect.createStatement()) {
            stmt.executeUpdate(sqlToExecute);
            log.trace("SUCCESS, on constraint SQL = {}", sqlToExecute);
        } catch (Exception e) {
            log.error("Error on 'constraint related' SQL = " + currentTableName, e);
            throw e;
        }
    }


}
