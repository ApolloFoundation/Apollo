/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvEscaper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvImportException;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReader;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvReaderImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.ValueParser;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import lombok.SneakyThrows;
import org.slf4j.Logger;

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

    private final Path dataExportPath; // path to folder with CSV files
    private final DatabaseManager databaseManager;
    private final Set<String> excludeTables; // skipped tables,
    private final AplAppStatus aplAppStatus;
    private final ValueParser parser;
    private final CsvEscaper translator;

    @Inject
    public CsvImporterImpl(@Named("dataExportDir") Path dataExportPath,
                           DatabaseManager databaseManager,
                           AplAppStatus aplAppStatus,
                           ValueParser parser,
                           CsvEscaper translator) {
        Objects.requireNonNull(dataExportPath, "dataExport path is NULL");
        this.dataExportPath = dataExportPath;
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.excludeTables = Set.of("genesis_public_key");
        this.aplAppStatus = aplAppStatus;
        this.parser = Objects.requireNonNull(parser, "value parser is NULL");
        this.translator = Objects.requireNonNull(translator, "Csv escaper is NULL.");
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
     * Return true if column type is binary.
     *
     * @param meta      the result set meta
     * @param columnIdx column index, the first column is at index 0
     * @return true if column type is BINARY or VARBINARY
     * @throws SQLException
     */
    private boolean isBinaryColumn(final ResultSetMetaData meta, final int columnIdx) throws SQLException {
        return (meta.getColumnType(columnIdx + 1) == Types.BINARY
            || meta.getColumnType(columnIdx + 1) == Types.VARBINARY
            || meta.getColumnType(columnIdx + 1) == Types.LONGVARBINARY
            || meta.getColumnType(columnIdx + 1) == Types.BLOB
            || meta.getColumnType(columnIdx + 1) == Types.JAVA_OBJECT
        );
    }

    /**
     * Return true if column type is array.
     *
     * @param meta      the result set meta
     * @param columnIdx column index, the first column is at index 0
     * @return true if column type is ARRAY
     * @throws SQLException
     */
    private boolean isArrayColumn(final ResultSetMetaData meta, final int columnIdx) throws SQLException {
        return (meta.getColumnType(columnIdx + 1) == Types.ARRAY);
    }

    /**
     * Return true if column type is varchar.
     *
     * @param meta      the result set meta
     * @param columnIdx column index, the first column is at index 0
     * @return true if column type is VARCHAR or NVARCHAR
     * @throws SQLException
     */
    private boolean isVarcharColumn(final ResultSetMetaData meta, final int columnIdx) throws SQLException {
        return (meta.getColumnType(columnIdx + 1) == Types.VARCHAR
            || meta.getColumnType(columnIdx + 1) == Types.NVARCHAR
            || meta.getColumnType(columnIdx + 1) == Types.CHAR
            || meta.getColumnType(columnIdx + 1) == Types.NCHAR
            || meta.getColumnType(columnIdx + 1) == Types.LONGVARCHAR
            || meta.getColumnType(columnIdx + 1) == Types.LONGNVARCHAR
            || meta.getColumnType(columnIdx + 1) == Types.CLOB
            || meta.getColumnType(columnIdx + 1) == Types.NCLOB
        );
    }

    /**
     * {@inheritDoc}
     */
    private long importCsv(String tableName, int batchLimit, boolean cleanTarget,
                           Double stateIncrease, Map<String, Object> defaultParams,
                           Consumer<Map<String, Object>> rowDataConsumer) throws CsvImportException {

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
        if (!file.exists()) {
            log.warn("Table/File is not found/exist, skipping : {}", file);
            return -1;
        }

        // clean up data if there is a chance the table could be previously filled
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        if (cleanTarget) {
            // remove all data from table before importing
            truncateTable(dataSource, tableName);
        }

        Map<String, Object> row = null;
        int rsCounter = 1; //start from 1 for "a%b==0" operations
        // open CSV Reader and db connection
        try (CsvReader csvReader = new CsvReaderImpl(this.dataExportPath, translator);
             ResultSet rs = csvReader.read(
                 inputFileName, null, null);
             Connection con = dataSource.isInTransaction() ? dataSource.getConnection() : dataSource.begin()) {
            csvReader.setOptions("fieldDelimiter="); // do not remove, setting = do not put "" around column/values

            // get CSV meta data info
            ResultSetMetaData meta = rs.getMetaData();
            columnsCount = meta.getColumnCount(); // columns count is main
            // precompile insert SQL
            preparedInsertStatement = con.prepareStatement(generateInsertStatement(tableName, meta, defaultParams));

            // loop over CSV data reading line by line, column by column
            while (rs.next()) {
                row = new HashMap<>();
                for (int i = 0; i < columnsCount; i++) {
                    Object object = rs.getObject(i + 1);
                    String columnName = meta.getColumnName(i + 1);
                    log.trace("{}[{} : {}] = {}", columnName, i + 1, meta.getColumnTypeName(i + 1), object);

                    if (object != null && isBinaryColumn(meta, i)) {
                        row.put(columnName.toLowerCase(), prepareBinaryObject(object, preparedInsertStatement, i + 1, meta.getPrecision(i + 1)));
                    } else if (object != null && isArrayColumn(meta, i)) {
                        row.put(columnName.toLowerCase(), prepareArrayObject(object, preparedInsertStatement, i + 1));
                    } else if (object != null && isVarcharColumn(meta, i)) {
                        row.put(columnName.toLowerCase(), prepareVarcharObject(object, preparedInsertStatement, i + 1));
                    } else {
                        row.put(columnName.toLowerCase(), prepareObject(object, preparedInsertStatement, i + 1));
                    }
                }
                int i = columnsCount + 1;
                for (Object value : defaultParams.values()) {
                    preparedInsertStatement.setObject(i++, value);
                }

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
            dataSource.commit(false); // final commit
        } catch (Exception e) {
            dataSource.rollback(false);
            log.error("Imported so far={}, rsCounter={}, row={}", importedCount, rsCounter, row);
            throw new CsvImportException("Error during importing '" + tableName + "'", e);
        } finally {
            if (preparedInsertStatement != null) {
                DbUtils.close(preparedInsertStatement);
            }
        }
        return importedCount;
    }

    @SneakyThrows
    private String generateInsertStatement(String tableName, ResultSetMetaData meta, Map<String, Object> defaultParams) {
        StringBuilder sqlInsert = new StringBuilder(300);
        StringBuilder columnNames = new StringBuilder(200);
        StringBuilder columnsValues = new StringBuilder(100);
        int columnsCount = meta.getColumnCount(); // columns count is main

        // create SQL insert statement
        sqlInsert.append("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columnsCount; i++) {
            columnNames.append(meta.getColumnLabel(i + 1));
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
        log.debug("SQL = {}", sqlInsert); // composed insert
        return sqlInsert.toString();
    }

    private void truncateTable(TransactionalDataSource dataSource, String tableName) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement preparedDelete = con.prepareStatement("TRUNCATE TABLE " + tableName)) {
            log.trace("Deleting data from '{}'", tableName);
            int deleted = preparedDelete.executeUpdate();
            log.trace("Deleted [{}] rows from '{}'", tableName, deleted);
        } catch (SQLException e) {
            throw new CsvImportException(String.format("Error cleaning up table's data, table='%s'", tableName), e);
        }
    }

    private Object prepareBinaryObject(Object object, PreparedStatement preparedInsertStatement, int index, int precision) {
        final byte[] decodedBytes = parser.parseBinaryObject(object);
        try (InputStream is = new ByteArrayInputStream(decodedBytes)) {
            preparedInsertStatement.setBinaryStream(index, is, precision);
        } catch (SQLException | IOException e) {
            throw new CsvImportException("Binary/Varbinary reading error = " + object, e);
        }
        return decodedBytes;
    }

    @SneakyThrows
    private Object prepareArrayObject(Object object, PreparedStatement preparedInsertStatement, int index) {
        Object[] actualArray = parser.parseArrayObject(object);
        SimpleResultSet.SimpleArray simpleArray = new SimpleResultSet.SimpleArray(actualArray);
        preparedInsertStatement.setArray(index, simpleArray);
        return actualArray;
    }

    @SneakyThrows
    private Object prepareVarcharObject(Object object, PreparedStatement preparedInsertStatement, int index) {
        String value = parser.parseStringObject(object);
        preparedInsertStatement.setString(index, value);
        return value;
    }

    @SneakyThrows
    private Object prepareObject(Object object, PreparedStatement preparedInsertStatement, int index) {
        Object value = parser.parseObject(object);
        preparedInsertStatement.setObject(index, value);
        return value;
    }

}
