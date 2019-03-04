package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import org.slf4j.Logger;

/**
 * Helper class for selecting Table data from one Db and inserting those data into another DB.
 */
public class SqlSelectAndInsertHelper {
    private static final Logger log = getLogger(SqlSelectAndInsertHelper.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public SqlSelectAndInsertHelper() {
    }

    int generateInsertStatements(Connection sourceConnect, Connection targetConnect, String tableName,
                                 long batchCommitSize, Block snapshotBlock)
            throws Exception {
        log.debug("Generating Insert statements for: '{}'", tableName);

        Statement selectStmt = sourceConnect.createStatement();
        long lowerIndex = 0, upperIndex = batchCommitSize;

        int insertedBeforeBatchCount = 0;
        int totalInsertedCount = 0;
        String unFormattedSql;
        if (tableName.equalsIgnoreCase("block")) {
            unFormattedSql = "SELECT * FROM %s where HEIGHT BETWEEN %d AND %d limit %d";
        } else {
//            unFormattedSql = "SELECT * FROM %s where HEIGHT BETWEEN %d AND %d limit %d";
            unFormattedSql = "SELECT * FROM %s";
        }
        String sqlToExecute;

        long startSelect = System.currentTimeMillis();
        ResultSet rs = null;
        if (tableName.equalsIgnoreCase("block")) {
            sqlToExecute = String.format(unFormattedSql, tableName, lowerIndex, upperIndex, batchCommitSize);
            rs = selectStmt.executeQuery(sqlToExecute);
        } else {
            rs = selectStmt.executeQuery("SELECT * FROM " + tableName);
        }
        log.debug("Select '{}' in {} secs", tableName, (System.currentTimeMillis() - startSelect) / 1000);
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        int[] columnTypes = new int[numColumns];

        StringBuilder columnNames = new StringBuilder();
        StringBuilder columnQuestionMarks = new StringBuilder();
        StringBuilder columnValues = new StringBuilder();

        java.util.Date d = null;

        int targetBlockIdColumnIndex = -1;
        int targetHeightColumnIndex = -1;
        for (int i = 0; i < numColumns; i++) {
            columnTypes[i] = rsmd.getColumnType(i + 1);
            if (i != 0) {
                columnNames.append(",");
                columnQuestionMarks.append("?,");
            }
//            columnNames.append(rsmd.getColumnName(i + 1));
            String columnName = rsmd.getColumnName(i + 1);
            columnNames.append(columnName);
            if (snapshotBlock != null
                    && (columnName.equalsIgnoreCase("BLOCK_ID") || columnName.equalsIgnoreCase("HEIGHT"))) {
                if (columnName.equalsIgnoreCase("BLOCK_ID")) {
                    targetBlockIdColumnIndex = i + 1;
                }
                if (columnName.equalsIgnoreCase("HEIGHT")) {
                    targetHeightColumnIndex = i + 1;
                }
            }

        }
        columnQuestionMarks.append("?");

        // no need to synch by StringBuffer implementation
        StringBuilder sqlInsertString = new StringBuilder(8000);
        sqlInsertString.append("INSERT INTO ").append(tableName)
                .append("(").append(columnNames).append(")").append(" values (").append(columnQuestionMarks).append(")");

        if (log.isTraceEnabled()) {
            log.trace(sqlInsertString.toString());
        }
        try (
                // precompile sql
                PreparedStatement preparedStatement = targetConnect.prepareStatement(sqlInsertString.toString())
        ) {
            long startInsert = System.currentTimeMillis();
            while (rs.next()) {
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
                    if (i != columnTypes.length - 1) {
                        columnValues.append(",");
                    }

                    if (targetBlockIdColumnIndex == i + 1) {
                        // BLOCK_ID should be assigned
                        preparedStatement.setObject(i + 1, snapshotBlock.getId());
                        continue;
                    }
                    if (targetHeightColumnIndex == i + 1) {
                        // HEIGHT should be assigned
                        preparedStatement.setObject(i + 1, snapshotBlock.getHeight());
                        continue;
                    }
                    preparedStatement.setObject(i + 1, rs.getObject(i + 1));

                }
                insertedBeforeBatchCount += preparedStatement.executeUpdate();
                if (insertedBeforeBatchCount >= batchCommitSize) {
                    targetConnect.commit();
                    totalInsertedCount += insertedBeforeBatchCount;
                    insertedBeforeBatchCount = 0;
                    log.trace("Partial commit = {}", totalInsertedCount);
                }
                columnValues.setLength(0);
            }
            targetConnect.commit(); // commit latest records if any
            totalInsertedCount += insertedBeforeBatchCount;
            log.debug("Finished '{}' inserted [{}] in = {} sec", tableName, totalInsertedCount, (System.currentTimeMillis() - startInsert) / 1000);
        } catch (Exception e) {
            sqlInsertString = new StringBuilder(8000);
            sqlInsertString.append("INSERT INTO ").append(tableName)
                    .append("(").append(columnNames).append(")").append(" values (").append(columnValues).append(")");

            int errorTotalCount = totalInsertedCount + insertedBeforeBatchCount;
            log.error("Insert error on record count=[" + errorTotalCount + "] by SQL =\n" + sqlInsertString.toString() + "\n", e);
        }
        return totalInsertedCount;
    }
}