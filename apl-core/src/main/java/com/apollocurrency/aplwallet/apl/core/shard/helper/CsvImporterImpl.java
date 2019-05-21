/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvImporterImpl implements CsvImporter {
    private static final Logger log = getLogger(CsvImporterImpl.class);

    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private Set<String> excludeTables; // skipped tables
    private CsvReader csvReader;

    private StringBuffer sqlInsert = new StringBuffer(300);
    private StringBuffer columnNames = new StringBuffer(200);
    private StringBuffer columnsValues = new StringBuffer(100);
    private StringBuffer sql = null;


    @Inject
    public CsvImporterImpl(@Named("dataExportDir") Path dataExportPath, DatabaseManager databaseManager) {
        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = new HashSet<>(1) {{
//            add("genesis_public_key");
        }};
        csvReader = new CsvReaderImpl(this.dataExportPath);
        csvReader.setOptions("fieldDelimiter="); // do not put "" around column/values
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long importCsv(String tableName, int batchLimit, boolean cleanTarget) {
        Objects.requireNonNull(tableName, "tableName is NULL");
        // skip hard coded table
        if (!tableName.isEmpty() && excludeTables.contains(tableName.toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table/File = {}", tableName);
            return -1;
        }
        int importedCount = 0;
        int columnsCount = 0;
        PreparedStatement preparedInsertStatement = null;
//        Statement stm = null;

        String inputFileName = tableName + ((CsvAbstractBase)csvReader).getFileNameExtension();
        File file = new File(this.dataExportPath.toString(), inputFileName);
        if(!file.exists()) {
            log.debug("Table/File is not found/exist, skipping : {}", file);
            return -1;
        }

        // clean up data if there is a chance the table could be previously filled
        if (cleanTarget) {
            // remove all data from table before importing
            try (Connection con = databaseManager.getDataSource().getConnection();
                 PreparedStatement preparedDelete = con.prepareStatement("DELETE from " + tableName)) {
                log.trace("Deleting data from '{}'", tableName);
                int deleted = preparedDelete.executeUpdate();
                log.trace("Deleted [{}] rows from '{}'", tableName, deleted);
            } catch (SQLException e) {
                String error = String.format("Error cleaning up table's data '%s'", tableName);
                log.error(error, e);
            }
        }

        // open CSV Reader and db connection
        try (ResultSet rs = csvReader.read(
                inputFileName, null, null);
//             Connection con = databaseManager.getDataSource().getConnection()) {
             Connection con = databaseManager.getDataSource().begin()) {

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            sqlInsert.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1)).append(",");
                columnsValues.append("?").append(",");
            }
            columnNames.deleteCharAt(columnNames.lastIndexOf(",")); // remove latest tail comma
            columnsValues.deleteCharAt(columnsValues.lastIndexOf(",")); // remove latest tail comma
            sqlInsert.append(columnNames).append(") VALUES").append(" (").append(columnsValues).append(")");
//            sqlInsert.append(columnNames).append(") VALUES").append(" (");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(sqlInsert.toString());
//            stm = con.createStatement();

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
//                    if (object instanceof String && ((String) object).startsWith("X'")) {
                    if (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY) {
                        preparedInsertStatement.setBytes(i + 1, ((String)object).getBytes(StandardCharsets.UTF_8));
//                        preparedInsertStatement.setBytes(i + 1, Convert.parseHexString((String)object));
//                        InputStream is = new ByteArrayInputStream( ((String)object).getBytes(StandardCharsets.UTF_8) );
//                        preparedInsertStatement.setBinaryStream(i + 1, is);

                    } else {
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                    log.trace("[{}] = {}\n", i+ 1, object);
//                    columnsValues.append(object).append(",");
                }
//                columnsValues.deleteCharAt(columnsValues.lastIndexOf(",")); // remove latest tail comma
//                sql = new StringBuffer(sqlInsert).append(columnsValues).append(")");
//                log.trace("sql = {}", sql);
                importedCount += preparedInsertStatement.executeUpdate();
                log.trace("sql = {}", sql);
//                importedCount += stm.executeUpdate(sql.toString());
                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
                columnsValues.setLength(0);
            }
            con.commit(); // final commit
        } catch (SQLException e) {
            log.error("Error on importing data on table = \n'{}'", sql, e);
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
//            if (stm != null) {
//                DbUtils.close(stm);
//            }
            // reset all buffers
            sqlInsert.setLength(0);
            columnNames.setLength(0);
            columnsValues.setLength(0);
        }
        return importedCount;
    }

/*
    @Override
    public long importCsv(String tableName, int batchLimit, boolean cleanTarget) {
        Objects.requireNonNull(tableName, "tableName is NULL");
        // skip hard coded table
        if (!tableName.isEmpty() && excludeTables.contains(tableName.toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table/File = {}", tableName);
            return -1;
        }
        int importedCount = 0;
        int columnsCount = 0;
        Statement stm = null;

        String inputFileName = tableName + ((CsvAbstractBase)csvReader).getFileNameExtension();
        File file = new File(this.dataExportPath.toString(), inputFileName);
        if(!file.exists()) {
            log.debug("Table/File is not found/exist, skipping : {}", file);
            return -1;
        }

        // clean up data if there is a chance the table could be previously filled
        if (cleanTarget) {
            // remove all data from table before importing
            try (Connection con = databaseManager.getDataSource().getConnection();
                 PreparedStatement preparedDelete = con.prepareStatement("DELETE from " + tableName)) {
                log.trace("Deleting data from '{}'", tableName);
                int deleted = preparedDelete.executeUpdate();
                log.trace("Deleted [{}] rows from '{}'", tableName, deleted);
            } catch (SQLException e) {
                String error = String.format("Error cleaning up table's data '%s'", tableName);
                log.error(error, e);
            }
        }

        // open CSV Reader and db connection
        try (ResultSet rs = csvReader.read(
                inputFileName, null, null);
             Connection con = databaseManager.getDataSource().begin()) {

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            sqlInsert.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1)).append(",");
            }
            columnNames.deleteCharAt(columnNames.lastIndexOf(",")); // remove latest tail comma
            sqlInsert.append(columnNames).append(") VALUES").append(" (");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            stm = con.createStatement();

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    String object = rs.getString(i + 1);
                    log.trace("{}: {}\n", object, rs.getString(i + 1));
                    columnsValues.append(object).append(",");
                }
                columnsValues.deleteCharAt(columnsValues.lastIndexOf(",")); // remove latest tail comma
                StringBuffer sql = new StringBuffer(sqlInsert).append(columnsValues).append(")");
                log.trace("sql = {}", sql);
                importedCount += stm.executeUpdate(sql.toString());
                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
                columnsValues.setLength(0);
            }
            con.commit(); // final commit
        } catch (SQLException e) {
            log.error("Error on importing data on table = '{}'", tableName, e);
        } finally {
            if (stm != null) {
                DbUtils.close(stm);
            }
            // reset all buffers
            sqlInsert.setLength(0);
            columnNames.setLength(0);
            columnsValues.setLength(0);
        }
        return importedCount;
    }
*/


}
