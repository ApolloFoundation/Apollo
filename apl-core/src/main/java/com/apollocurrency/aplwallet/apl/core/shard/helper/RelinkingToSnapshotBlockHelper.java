/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

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
 * Helper class is used for changing/updating linked table's data from old Block records to snapshot Block record.
 *
 * @author yuriy.larin
 */
public class RelinkingToSnapshotBlockHelper implements BatchedSelectInsert {
    private static final Logger log = getLogger(RelinkingToSnapshotBlockHelper.class);

    private String currentTableName;
    private ResultSetMetaData rsmd;
    private int numColumns;
//    private int[] columnTypes;
//    int targetDbIdColumnIndex = -1;

//    private StringBuilder sqlInsertString = new StringBuilder(500);
//    private StringBuilder columnNames = new StringBuilder();
//    private StringBuilder columnQuestionMarks = new StringBuilder();
//    private StringBuilder columnValues = new StringBuilder();

    private Long totalRowCount = 0L;
    private Long insertedCount = 0L;
    private String BASE_COLUMN_NAME = "DB_ID";
    private PreparedStatement preparedInsertStatement = null;


    public RelinkingToSnapshotBlockHelper() {
    }

    @Override
    public void reset() {
        this.currentTableName = null;
        this.rsmd = null;
        this.numColumns = -1;
//        this.columnTypes = null;
//        this.targetDbIdColumnIndex = -1;
//        this.sqlInsertString = new StringBuilder(500);
//        this.columnNames = new StringBuilder();
//        this.columnQuestionMarks = new StringBuilder();
//        this.columnValues = new StringBuilder();
        this.totalRowCount = 0L;
        this.insertedCount = 0L;
        this.preparedInsertStatement = null;
    }

    @Override
    public long selectInsertOperation(Connection sourceConnect, Connection targetConnect, String tableName,
                                      long batchCommitSize, Long snapshotBlockHeight)
            throws Exception {
        log.debug("Processing: '{}'", tableName);
        Objects.requireNonNull(sourceConnect, "sourceConnect is NULL");
        Objects.requireNonNull(snapshotBlockHeight, "snapshotBlockHeight is NULL");
        currentTableName = tableName;

//        String sqlUpdateLinkedTable = "UPDATE " + currentTableName + " set HEIGHT = ?";
//        log.trace(sqlUpdateLinkedTable);
        long startSelect = System.currentTimeMillis();

/*
        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlUpdateLinkedTable) ) {
            ps.setInt(1, snapshotBlockHeight.intValue());
            totalRowCount += ps.executeUpdate();
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
            throw e;
        }
        log.debug("'{}' = [{}] in {} secs", tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
*/

        String sqlToExecuteWithPaging = "UPDATE " + currentTableName + " set HEIGHT = ? where DB_ID >= ? AND DB_ID <= ? limit ?";
        log.trace(sqlToExecuteWithPaging);
        // select MAX DB_ID
        Long transactionTargetDbID = selectUpperDbId(sourceConnect, snapshotBlockHeight);;
        if (transactionTargetDbID == null) {
            String error = String.format("Not Found MAX height = %s", snapshotBlockHeight);
            log.error(error);
            throw new RuntimeException(error);
        }
        // select MIN DB_ID
        Long lowerIndex = selectLowerDbId(sourceConnect);

        PaginateResultWrapper paginateResultWrapper = new PaginateResultWrapper();
        paginateResultWrapper.limitValue = lowerIndex;

        try (PreparedStatement ps = sourceConnect.prepareStatement(sqlToExecuteWithPaging)) {
            do {
                ps.setLong(1, snapshotBlockHeight);
                ps.setLong(2, paginateResultWrapper.limitValue);
                ps.setLong(3, transactionTargetDbID);
                ps.setLong(4, batchCommitSize);
            } while (handleResultSet(ps, paginateResultWrapper, sourceConnect, batchCommitSize));
        } catch (Exception e) {
            log.error("Processing failed, Table " + currentTableName, e);
        }
        log.debug("'{}' = [{}] in {} secs", tableName, totalRowCount, (System.currentTimeMillis() - startSelect) / 1000);
        return totalRowCount;
    }

    private boolean handleResultSet(PreparedStatement ps, PaginateResultWrapper paginateResultWrapper,
                                    Connection targetConnect, long batchCommitSize)
            throws SQLException {
        int rows = 0;
//        try (ResultSet rs = ps.executeQuery()) {
        try {
//            insertedCount += preparedInsertStatement.executeUpdate();
            rows = ps.executeUpdate();
            log.debug("Updating '{}' in {} : column {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
        } catch (Exception e) {
            log.error("Failed Updating '{}' into {}, {}={}", rows, currentTableName, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);
            log.error("Failed Updating " + currentTableName, e);
        }
        log.trace("Total Records: updated = {}, rows = {}, {}={}",
                totalRowCount, rows, BASE_COLUMN_NAME, paginateResultWrapper.limitValue);

        totalRowCount += rows;
        paginateResultWrapper.limitValue += batchCommitSize;
        targetConnect.commit(); // commit latest records if any
        return rows != 0;
    }

    private Long selectUpperDbId(Connection sourceConnect, Long snapshotBlockHeight) throws SQLException {
        String selectTransactionTimestamp =
                "select IFNULL(max(DB_ID), 0) as DB_ID from " + currentTableName;
        Long highDbIdValue = null;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectTransactionTimestamp)) {
//            selectStatement.setInt(1, snapshotBlockHeight.intValue());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                highDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Transaction's DB_ID value = {}", highDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding LINKED DB_ID in = " + currentTableName, e);
            throw e;
        }
        return highDbIdValue;
    }

    private Long selectLowerDbId(Connection sourceConnect) throws SQLException {
        // select DB_ID as = (min(DB_ID) - 1)  OR  = 0 if value is missing
        String selectDbId = "SELECT IFNULL(min(DB_ID)-1, 0) as DB_ID from " + currentTableName;
        Long bottomDbIdValue = 0L;
        try (PreparedStatement selectStatement = sourceConnect.prepareStatement(selectDbId)) {
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                bottomDbIdValue = rs.getLong("DB_ID");
                log.trace("FOUND Minimal Transaction DB_ID value = {}", bottomDbIdValue);
            }
        } catch (Exception e) {
            log.error("Error finding LOW LIMIT DB_ID in = " + currentTableName, e);
            throw e;
        }
        return bottomDbIdValue;
    }

}