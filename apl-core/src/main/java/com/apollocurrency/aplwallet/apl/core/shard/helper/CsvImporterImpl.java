/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@inheritDoc}
 */
@Singleton
public class CsvImporterImpl implements CsvImporter {
    private static final Logger log = getLogger(CsvImporterImpl.class);

    private Path dataExportPath; // path to folder with CSV files
    private DatabaseManager databaseManager;
    private Set<String> excludeTables; // skipped tables,
    private  AplAppStatus aplAppStatus;

    @Inject
    public CsvImporterImpl(@Named("dataExportDir") Path dataExportPath,
                           DatabaseManager databaseManager,
                           AplAppStatus aplAppStatus) {
        Objects.requireNonNull(dataExportPath, "dataExport path is NULL");
        this.dataExportPath = dataExportPath;
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = Set.of("genesis_public_key");
        this.aplAppStatus = aplAppStatus;
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
        return this.importCsv(tableName, batchLimit, cleanTarget, null, Map.of(), null);
    }

    @Override
    public long importCsvWithRowHook(String tableName, int batchLimit, boolean cleanTarget, Consumer<Map<String, Object>> rowDataHook) throws Exception {
        return this.importCsv(tableName, batchLimit, cleanTarget, null, Map.of(), rowDataHook);
    }

    @Override
    public long importCsvWithDefaultParams(String tableName, int batchLimit, boolean cleanTarget, Map<String, Object> defaultParams) throws Exception {
        return this.importCsv(tableName, batchLimit, cleanTarget, null, defaultParams, null);
    }

    /**
     * {@inheritDoc}
     */
    private long importCsv(String tableName, int batchLimit, boolean cleanTarget,
                           Double stateIncrease, Map<String, Object> defaultParams,
                           Consumer<Map<String, Object>> rowDataConsumer) throws Exception {

        Objects.requireNonNull(tableName, "tableName is NULL");
        // skip hard coded table
        if (!tableName.isEmpty() && excludeTables.contains(tableName.toLowerCase())) {
            // skip not needed table
            log.debug("Skipped excluded Table/File = {}", tableName);
            return -1;
        }
        int importedCount = 0;
        int columnsCount;
        PreparedStatement preparedInsertStatement = null;

        // file 'extension' should be checked on file instance actually
        String inputFileName = tableName + CsvAbstractBase.CSV_FILE_EXTENSION; // getFileNameExtension() would be better
        File file = new File(this.dataExportPath.toString(), inputFileName);
        if(!file.exists()) {
            log.warn("Table/File is not found/exist, skipping : {}", file);
            return -1;
        }

        // clean up data if there is a chance the table could be previously filled
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (cleanTarget) {
            // remove all data from table before importing
            try (Connection con = dataSource.getConnection();
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
             Connection con = dataSource.isInTransaction() ? dataSource.getConnection() : dataSource.begin()) {
            csvReader.setOptions("fieldDelimiter="); // do not remove, setting = do not put "" around column/values

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // create SQL insert statement
            sqlInsert.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsCount; i++) {
                columnNames.append( meta.getColumnLabel(i + 1));
                columnsValues.append("?");
                if (!defaultParams.isEmpty() || i != columnsCount - 1) {
                    columnNames.append(",");
                    columnsValues.append(",");
                }
            }
            if (!defaultParams.isEmpty()) {
                columnNames.append(String.join(",", defaultParams.keySet()));
                columnsValues.append(String.join(",", "?"));
            }
            sqlInsert.append(columnNames).append(") VALUES").append(" (").append(columnsValues).append(")");
            log.debug("SQL = {}", sqlInsert.toString()); // composed insert
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(sqlInsert.toString());
            int rsCounter=1; //start from 1 for "a%b==0" operations
            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
                    String columnName = meta.getColumnName(i + 1);
                    log.trace("{}[{} : {}] = {}", columnName, i + 1, meta.getColumnTypeName(i + 1), object);

                    if (object != null && (meta.getColumnType(i + 1) == Types.BINARY || meta.getColumnType(i + 1) == Types.VARBINARY)) {
                        byte[] decodedBytes = Base64.getDecoder().decode(((String) object));
                        try (InputStream is = new ByteArrayInputStream(decodedBytes)) {
                            // meta.getPrecision(i + 1) - is very IMPORTANT here for H2 db !!!
                            preparedInsertStatement.setBinaryStream(i + 1, is, meta.getPrecision(i + 1));
                            row.put(columnName.toLowerCase(), Base64.getDecoder().decode((String) object));
                        }
                        catch (SQLException e) {
                            log.error("Binary/Varbinary reading error = " + object, e);
                            throw e;
                        }
                        // ignore error here
                    } else if (object != null && (meta.getColumnType(i + 1) == Types.ARRAY)) {
                        String objectArray = (String)object;
                        Object[] actualArray;
                        if (!StringUtils.isBlank(objectArray)) {
                            String[] split = objectArray.split(",");
                            actualArray = new Object[split.length];
                            for (int j = 0; j < split.length; j++) {
                                String value = split[j];
                                if (value.startsWith("b\'") && value.endsWith("\'")) { //find byte arrays
                                    //byte array found
                                    byte[] actualValue = Base64.getDecoder().decode(value.substring(2, value.length() - 1));
                                    actualArray[j] = actualValue;
                                } else if (value.startsWith("\'") && value.endsWith("\'")) { //find string
                                    actualArray[j] = split[j].substring(1, split[j].length() - 1);
                                } else { // try to process number
                                    try {
                                        actualArray[j] = Integer.parseInt(split[j]);
                                    }
                                    catch (NumberFormatException ignored) { // value can be of long type
                                        try {
                                            actualArray[j] = Long.parseLong(split[j]); // try to parse long
                                        }
                                        catch (NumberFormatException e) { // throw exception, when specified value is not a string, long, int or byte array
                                            throw new RuntimeException("Value " + split[j] + " of unsupported type");
                                        }
                                    }
                                }
                            }
                        } else {
                            actualArray = new Object[0];
                        }
                        SimpleResultSet.SimpleArray simpleArray = new SimpleResultSet.SimpleArray(actualArray);
                        preparedInsertStatement.setArray(i + 1, simpleArray);
                        row.put(columnName.toLowerCase(), actualArray);
                    } else {
                        row.put(columnName.toLowerCase(), object);
                        preparedInsertStatement.setObject(i + 1, object);
                    }
                }
                int i = columnsCount + 1;
                for (Object value : defaultParams.values()) {
                    preparedInsertStatement.setObject(i++, value);
                }

                log.trace("sql = {}", sqlInsert.toString());
                importedCount += preparedInsertStatement.executeUpdate();
                if (rowDataConsumer != null) {
                    rowDataConsumer.accept(row);
                }
                if (rsCounter % batchLimit == 0) {
                    dataSource.commit(false);
                    // update state only for ACCOUNT table during LONG running import
                    if (aplAppStatus != null && stateIncrease != null && tableName.equalsIgnoreCase("account")) {
                        Optional<DurableTaskInfo> task = aplAppStatus.findTaskByName("Shard data import");
                        task.ifPresent(durableTaskInfo -> aplAppStatus.durableTaskUpdate(durableTaskInfo.id, "importing " + tableName, 0.01));
                    }
                }
                rsCounter++;
            }
            dataSource.commit(); // final commit
        } catch (Exception e) {
            log.error("Error during importing '" + tableName + "'", e);
            dataSource.rollback();
            throw new RuntimeException(e);
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
        }
        return importedCount;
    }

}
