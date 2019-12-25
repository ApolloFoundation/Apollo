/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExportData;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.ColumnMetaData;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@inheritDoc}
 */
public class CsvWriterImpl extends CsvAbstractBase implements CsvWriter {
    private static final Logger log = getLogger(CsvWriterImpl.class);
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    //private static final String QUOTE = String.valueOf(TEXT_FIELD_START);
    //private static final String DOUBLE_QUOTE = QUOTE + QUOTE;
    private static final String EMPTY_ARRAY = "()";

    private Writer output;
    private final StringBuilder outputBuffer = new StringBuilder(400);
    private final Set<String> excludeColumn = new HashSet<>();

    public CsvWriterImpl(Path dataExportPath, Set<String> excludeColumnNames, CsvEscaper translator) {
        super.dataExportPath = Objects.requireNonNull(dataExportPath, "dataExportPath is NULL.");
        super.translator = Objects.requireNonNull(translator, "Csv escaper is NULL.");
        if (excludeColumnNames != null && !excludeColumnNames.isEmpty()) {
            // assign non empty Set
            this.excludeColumn.addAll(excludeColumnNames);
            log.debug("Config Excluded columns = {}", Arrays.toString(excludeColumnNames.toArray()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CsvExportData write(Writer writer, ResultSet rs) throws SQLException {
        this.output = writer;
        return writeResultSet(rs, true, Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CsvExportData write(String outputFileName, ResultSet rs) throws SQLException {
        Objects.requireNonNull(outputFileName, "outputFileName is NULL");
        Objects.requireNonNull(rs, "resultSet is NULL");
        assignNewFileName(outputFileName);
        try {
            initWrite(false);
            return writeResultSet(rs, true, Map.of());
        } catch (IOException e) {
            throw new SQLException("IOException writing " + outputFileName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CsvExportData append(String outputFileName, ResultSet rs) throws SQLException {
        return append(outputFileName, rs, Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CsvExportData append(String outputFileName, ResultSet rs, Map<String, String> defaultValues) throws SQLException {
        Objects.requireNonNull(outputFileName, "outputFileName is NULL");
        Objects.requireNonNull(rs, "resultSet is NULL");
        assignNewFileName(outputFileName);
        try {
            initWrite(true);
            return writeResultSet(rs, false, defaultValues);
        } catch (IOException e) {
            throw new SQLException("IOException writing " + outputFileName, e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CsvExportData write(Connection conn, String outputFileName, String sql, String charset) throws SQLException {
        CsvExportData exportData;
        try (Statement stat = conn.createStatement()) {
            ResultSet rs = stat.executeQuery(sql);
            exportData = write(outputFileName, rs);
        }
        return exportData;
    }

    private void initWrite(boolean appendMode) throws IOException {
        if (output == null) {
            try {

                Path filePath = this.dataExportPath.resolve(!this.fileName.endsWith(CSV_FILE_EXTENSION) ? this.fileName + CSV_FILE_EXTENSION : this.fileName);
                boolean fileExist = Files.exists(filePath);
                if (!fileExist && appendMode) { // check CSV file in dataExport folder
                    Files.createFile(filePath);
                }
                if (fileExist && appendMode) {
                    int lines = Files.readAllLines(filePath).size();
                    if (lines > 0) {
                        writeColumnHeader = false;
                    }
                }
                OutputStream out = Files.newOutputStream(filePath, appendMode ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW);
                out = new BufferedOutputStream(out, IO_BUFFER_SIZE);
                output = new BufferedWriter(new OutputStreamWriter(out, characterSet));
            } catch (Exception e) {
                close();
                log.error("initWrite() exception, appendMode=" + appendMode, e);
                throw e;
            }
        }
    }

    protected void assignNewFileName(String newFileName) {
        Objects.requireNonNull(newFileName, "fileName is NULL");
        if (!newFileName.equalsIgnoreCase(this.fileName)) {
            // new file name is assigned
            this.writeColumnHeader = true; // will write header column names
        }
        this.fileName = newFileName;
    }

    private CsvExportData writeResultSet(ResultSet rs, boolean closeWhenNotAppend, Map<String, String> defaultValues) throws SQLException {
        try {
            Map<String, Object> lastRow = new HashMap<>();
            int rows = 0;
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (columnsMetaData == null) {
                // meta data array for current table
                columnsMetaData = new ColumnMetaData[columnCount];
            }
            Object[] rowColumnNames = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                rowColumnNames[i] = meta.getColumnLabel(i + 1);
                // fill in meta data
                columnsMetaData[i] = new ColumnMetaData(meta.getColumnLabel(i + 1),
                        meta.getColumnTypeName(i + 1), meta.getColumnType(i + 1),
                        meta.getPrecision(i + 1), meta.getScale(i + 1));
            }
            log.trace("Table/File = '{}', MetaData = {}", this.fileName, Arrays.toString(columnsMetaData));
            if (writeColumnHeader) {
                log.debug("Header columns = {}", Arrays.toString(rowColumnNames));
                writeHeaderRow(columnsMetaData);
                this.writeColumnHeader = false;// write header columns only once after fileName/tableName has been changed
            }
            while (rs.next()) {
                for (int i = 0; i < columnCount; i++) {
                    java.util.Date date = null;
                    Object o;
                    String defaultValue = defaultValues.get(columnsMetaData[i].getName().toLowerCase());
                if (defaultValue != null) {
                    o = defaultValue;
                    lastRow.put(rs.getMetaData().getColumnName(i + 1), o);
                } else {
                    lastRow.put(rs.getMetaData().getColumnName(i + 1), rs.getObject(i + 1));
                        switch (columnsMetaData[i].getSqlTypeInt()) {
                            case Types.BLOB:
                                o = rs.getBlob(i + 1);
                                if (o == null) {
                                    o = nullString;
                                }
                                break;
                            case Types.BIGINT:
                            case Types.BIT:
                            case Types.BOOLEAN:
                            case Types.DECIMAL:
                            case Types.DOUBLE:
                            case Types.FLOAT:
                            case Types.INTEGER:
                            case Types.SMALLINT:
                            case Types.TINYINT:
                                o = rs.getString(i + 1);
                                if (o == null) {
                                    o = nullString;
                                }
                                break;
                            case Types.DATE:
                                date = rs.getDate(i + 1);
                            case Types.TIME:
                                if (date == null) date = rs.getTime(i + 1);
                            case Types.TIMESTAMP:
                                if (date == null) date = rs.getTimestamp(i + 1);
                                if (date == null) {
                                    o = nullString;
                                } else {
                                    o = "TO_DATE('" + DATE_FORMAT.format(date) + "', 'YYYY/MM/DD HH24:MI:SS')";
                                }
                                break;
                            case Types.ARRAY:
                                Array array = rs.getArray(i + 1);
                                if (array != null && array.getArray() instanceof Object[] && ((Object[]) array.getArray()).length > 0) {
                                    Object[] objectArray = (Object[]) array.getArray();
                                    StringBuilder outputValue = new StringBuilder();
                                    outputValue.append(arrayStartToken);
                                    for (int j = 0; j < objectArray.length; j++) {
                                        if (j>0){
                                            outputValue.append(fieldSeparatorWrite);
                                        }
                                        Object item = objectArray[j];
                                        String objectValue;
                                        if (item instanceof byte[]) {
                                            objectValue = translator.translate((byte[]) item);
                                        } else if (item instanceof String) {
                                            objectValue = translator.quotedText((String)item);
                                        } else if (item instanceof Long || item instanceof Integer) {
                                            objectValue = item.toString();
                                        } else {
                                            throw new RuntimeException("Unsupported array type: " + item.getClass());
                                        }
                                        outputValue.append(objectValue);
                                    }
                                    outputValue.append(arrayEndToken);
                                    o = outputValue.toString();
                                    break;
                                } else {
                                    o = array != null ? EMPTY_ARRAY : nullString;
                                }
                                break;
                            case Types.VARBINARY:
                            case Types.BINARY:
                                o = rs.getBytes(i + 1);
                                if (o != null) {
                                    o = translator.translate((byte[]) o);
                                } else {
                                    o = nullString;
                                }
                                break;
                            case Types.NVARCHAR:
                            case Types.VARCHAR:
                            default:
                                o = rs.getString(i + 1);
                                if (o != null) {
                                    o = translator.quotedText((String) o);
                                } else {
                                    o = nullString;
                                }
                                break;
                        }
                    }
                    rowColumnNames[i] = o == null ? null : o.toString();
                }
                if(log.isTraceEnabled()) {
                    log.trace("Row = {}", Arrays.toString(rowColumnNames));
                }
                writeRow(rowColumnNames);
                rows++;
            }

            if (closeWhenNotAppend) {
                output.close(); // close file on 'write mode'
            } else {
                output.flush(); // flush unfinished file on 'append mode'
            }
            log.trace("CSV file '{}' written rows=[{}]", fileName, rows);
            return new CsvExportData(rows, lastRow);
        } catch (IOException e) {
            log.error("IO exception", e);
            throw new SQLException(e);
        } finally {
            if (closeWhenNotAppend) {
                close();
            }
            DbUtils.closeSilently(rs);
        }
    }

    /**
     * Write CSV header row in format = COLUMN_NAME(TYPE|PRECISION|SCALE)
     *
     * @param columnsMetaData meta data - COLUMN_NAME(TYPE|PRECISION|SCALE)
     * @throws IOException
     */
    private void writeHeaderRow(ColumnMetaData[] columnsMetaData) throws IOException {
        Objects.requireNonNull(columnsMetaData, "columnsMetaData is NULL");
        boolean isSkippedColumn = false;
        for (int i = 0; i < columnsMetaData.length; i++) {
            if (columnsMetaData[i] != null && columnsMetaData[i].getName() != null) {
                String s = columnsMetaData[i].getName(); // column name
                if (i > 0 && !isSkippedColumn) {
                    if (fieldSeparatorWrite != null) {
                        // do not write comma-separator in case skipped column
                        outputBuffer.append(fieldSeparatorWrite);
                    }
                }
                if (excludeColumn.contains(s)) {
                    // skip processing specified columns
                    isSkippedColumn = true;
                    continue;
                }
                if (fieldDelimiter != 0) {
                    outputBuffer.append(fieldDelimiter);
                }
                // writing 'header columns' row into output file
                if ((!Character.isLetterOrDigit(fieldTypeSeparatorStart) && !Character.isSpaceChar(fieldTypeSeparatorStart))
                        && (!Character.isLetterOrDigit(fieldTypeSeparatorEnd) && !Character.isSpaceChar(fieldTypeSeparatorEnd))) {
                    // write 'complex' csv Header columns as COLUMN_NAME_1(TYPE_1|PRECISION_1|SCALE_1),COLUMN_NAME_2(TYPE_2|PRECISION_2|SCALE_2)
                    outputBuffer.append(columnsMetaData[i].toString());
                } else {
                    // write simple header columns as : COLUMN_NAME_1,COLUMN_NAME_2
                    outputBuffer.append(s);
                }
                if (fieldDelimiter != 0) {
                    outputBuffer.append(fieldDelimiter);
                }
            } else {
                // we can't proceed if column name is empty
                log.error("ERROR, column name is EMPTY. Array = {}", Arrays.toString(columnsMetaData));
                throw new IllegalArgumentException("ERROR, column name is EMPTY");
            }
            isSkippedColumn = false; // reset flag
        }
        if (isSkippedColumn) {
            // remove latest, not needed extra comma at end
            outputBuffer.deleteCharAt(outputBuffer.lastIndexOf(","));
        }
        outputBuffer.append(lineSeparator);
        output.write(outputBuffer.toString());
        outputBuffer.setLength(0); // reset
    }

    private void writeRow(Object[] rowColumnValues) throws IOException {
        boolean isSkippedColumn = false;
        for (int i = 0; i < rowColumnValues.length; i++) {
            if (rowColumnValues[i] != null && rowColumnValues[i].toString() != null) {
                if (i > 0 && !isSkippedColumn) {
                    if (fieldSeparatorWrite != null) {
                        // do not write comma-separator in case skipped column
                        outputBuffer.append(fieldSeparatorWrite);
                    }
                }
                if (excludeColumn.contains(columnsMetaData[i].getName())) {
                    // skip column value processing
                    isSkippedColumn = true; // do not put not needed comma
                    continue;
                }
                String s = rowColumnValues[i].toString(); // column value
                outputEscapedValueWithDelimiter(s);
            } else if (nullString != null && nullString.length() > 0 && !nullString.equalsIgnoreCase("null")) {
                outputBuffer.append(nullString);
            }
            isSkippedColumn = false; // reset flag
        }
        // remove last comma, when latest column was skipped
        if (isSkippedColumn) {
            outputBuffer.deleteCharAt(outputBuffer.lastIndexOf(","));
        }
        outputBuffer.append(lineSeparator);
        output.write(outputBuffer.toString());
        outputBuffer.setLength(0); // reset
    }

    private String arrayToString(Object[] data){
        StringBuilder buffer = new StringBuilder();
        buffer.append("(");
        for (int j = 0; j < data.length; j++) {
                if(j>0){
                    buffer.append(",");
                }
                Object rowColumnValue = data[j];
                buffer.append(rowColumnValue);
        }
        buffer.append(")");
        return buffer.toString();
    }

    private void outputEscapedValueWithDelimiter(String s) {
        if (escapeCharacter != 0) {
            if (fieldDelimiter != 0) {
                outputBuffer.append(fieldDelimiter);
            }
            outputBuffer.append(escape(s));
            if (fieldDelimiter != 0) {
                outputBuffer.append(fieldDelimiter);
            }
        } else {
            outputBuffer.append(s);
        }
    }

    private String escape(String data) {
        return translator.escape(data);
    }

    @Override
    public void close() {
        outputBuffer.setLength(0);
        CsvFileUtils.closeSilently(output);
        output = null;
        columnsMetaData = null;
    }

}
