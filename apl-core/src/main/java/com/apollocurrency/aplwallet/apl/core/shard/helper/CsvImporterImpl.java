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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
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
    public long importCsv(String tableName, int batchLimit, boolean cleanTarget) throws SQLException {
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

        String inputFileName = tableName + ((CsvAbstractBase)csvReader).getFileNameExtension();
        File file = new File(this.dataExportPath.toString(), inputFileName);
        if(!file.exists()) {
            log.warn("Table/File is not found/exist, skipping : {}", file);
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
                throw e;
            }
        }

        // open CSV Reader and db connection
        try (ResultSet rs = csvReader.read(
                inputFileName, null, null);
             Connection con = databaseManager.getDataSource().getConnection()) {

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
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(sqlInsert.toString());

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
                    log.trace("{}[{} : {}] = {}", meta.getColumnName(i + 1), i + 1, meta.getColumnTypeName(i + 1), object);

                    if (object != null && (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY)) {
                        InputStream is = null;
                        try {
                            is = new ByteArrayInputStream( Base64.getDecoder().decode(((String)object)) );
                            preparedInsertStatement.setBinaryStream(i + 1, is, meta.getPrecision(i + 1));
                        } catch (SQLException e) {
                            log.error("Binary/Varbinary reading error = " + object, e);
                            throw e;
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {} // ignore error here
                            }
                        }
                    } else if (object != null && (meta.getColumnType(i + 1) == Types.ARRAY)) {
                        String objectArray = (String)object;
                        Object[] split = objectArray.split(",");
                        SimpleResultSet.SimpleArray simpleArray = new SimpleResultSet.SimpleArray(split);
                        preparedInsertStatement.setArray(i + 1, simpleArray);
                    } else {
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                }

                log.trace("sql = {}", sqlInsert.toString());
                importedCount += preparedInsertStatement.executeUpdate();

                if (batchLimit % importedCount == 0) {
                    con.commit();
                }
            }
            con.commit(); // final commit
        } catch (SQLException e) {
            log.error("Error on importing data on table = \n'{}'", sql, e);
            throw e;
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
            // reset all temporal buffers
            sqlInsert.setLength(0);
            columnNames.setLength(0);
            columnsValues.setLength(0);
        }
        return importedCount;
    }

}
