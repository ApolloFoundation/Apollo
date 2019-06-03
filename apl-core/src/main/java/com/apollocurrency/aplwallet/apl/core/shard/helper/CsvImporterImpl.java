/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvImporterImpl implements CsvImporter {
    private static final Logger log = getLogger(CsvImporterImpl.class);

    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private Set<String> excludeTables; // skipped tables

    @Inject
//    public CsvImporterImpl(@Named("dataExportDir") Path dataExportPath, DatabaseManager databaseManager) {
    public CsvImporterImpl(ShardExportDirProducer exportDirProducer, DatabaseManager databaseManager) {
        Objects.requireNonNull(exportDirProducer, "exportDirProducer is NULL");
        Objects.requireNonNull(exportDirProducer.getDataExportDir(), "exportDirProducer 'data Path' is NULL");
        this.dataExportPath = exportDirProducer.getDataExportDir();
//        this.dataExportPath = Objects.requireNonNull(dataExportPath, "data export Path is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = Set.of("genesis_public_key");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDataExportPath() {
        return this.dataExportPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long importCsv(String tableName, int batchLimit, boolean cleanTarget) throws Exception {
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

        // file 'extension' should be checked on file instance actually
        String inputFileName = tableName + CsvAbstractBase.CSV_FILE_EXTENSION; // getFileNameExtension() would be better
        File file = new File(this.dataExportPath.toString(), inputFileName);
        if(!file.exists()) {
            log.warn("Table/File is not found/exist, skipping : {}", file);
            return -1;
        }

        // clean up data if there is a chance the table could be previously filled
        if (cleanTarget) {
            // remove all data from table before importing
            try (Connection con = databaseManager.getDataSource().getConnection();
                 PreparedStatement preparedDelete = con.prepareStatement("TRUNCATE TABLE " + tableName)) {
                log.trace("Deleting data from '{}'", tableName);
                int deleted = preparedDelete.executeUpdate();
                log.trace("Deleted [{}] rows from '{}'", tableName, deleted);
            } catch (SQLException e) {
                String error = String.format("Error cleaning up table's data '%s'", tableName);
                log.error(error, e);
                throw e;
            }
        }

        StringBuilder sqlInsert = new StringBuilder(300);
        StringBuilder columnNames = new StringBuilder(200);
        StringBuilder columnsValues = new StringBuilder(100);

        // open CSV Reader and db connection
        try (CsvReader csvReader = new CsvReaderImpl(this.dataExportPath);
                ResultSet rs = csvReader.read(
                inputFileName, null, null);
             Connection con = databaseManager.getDataSource().getConnection()) {
            csvReader.setOptions("fieldDelimiter="); // do not remove, setting = do not put "" around column/values

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            sqlInsert.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1));
                columnsValues.append("?");
                if (i != columnsCount - 1) {
                    columnNames.append(",");
                    columnsValues.append(",");
                }
            }
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
                        try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(((String) object)))) {
                            // meta.getPrecision(i + 1) - is very IMPORTANT here for H2 db !!!
                            preparedInsertStatement.setBinaryStream(i + 1, is, meta.getPrecision(i + 1));
                        }
                        catch (SQLException e) {
                            log.error("Binary/Varbinary reading error = " + object, e);
                            throw e;
                        }
                        // ignore error here
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
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
        }
        return importedCount;
    }

}
