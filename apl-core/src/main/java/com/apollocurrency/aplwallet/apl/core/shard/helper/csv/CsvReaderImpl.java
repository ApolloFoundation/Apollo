/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.ColumnMetaData;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleRowSource;
import com.apollocurrency.aplwallet.apl.core.shard.util.ConversionUtils;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * {@inheritDoc}
 */
public class CsvReaderImpl extends CsvAbstractBase
        implements CsvReader, SimpleRowSource, AutoCloseable {
    private static final Logger log = getLogger(CsvReaderImpl.class);

    private Reader input;
    private char[] inputBuffer;
    private int inputBufferPos;
    private int inputBufferStart = -1;
    private int inputBufferEnd;

    private boolean endOfLine;
    private boolean endOfFile;

    public CsvReaderImpl(Path dataExportPath) {
        super.dataExportPath = Objects.requireNonNull(dataExportPath, "dataExportPath is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet read(String inputFileName, String[] colNames, String charset) throws SQLException {
        Objects.requireNonNull(inputFileName, "inputFileName is NULL");
        assignNewFileName(inputFileName, true);
        try {
            return readResultSet(colNames);
        } catch (IOException e) {
            throw new SQLException("Exception reading (or not found) file '"
                    + inputFileName + "' in path = " + super.dataExportPath, e);
        }
    }

    private void assignNewFileName(String newFileName, boolean closeWhenAppend) {
        Objects.requireNonNull(newFileName, "fileName is NULL");
        this.fileName = newFileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet read(Reader reader, String[] colNames) throws IOException {
        Objects.requireNonNull(reader, "reader is NULL");
        this.input = reader;
        return readResultSet(colNames);
    }

    private ResultSet readResultSet(String[] colNames) throws IOException {
        this.columnNames = colNames;
        initRead();
        SimpleResultSet result = new SimpleResultSet(this);
        makeColumnNamesUnique();
        int index = 0;
        //reading from header like COLUMN_NAME(TYPE|PRECISION|SCALE)
        for (String columnName : columnNames) { // reading header meta data
            ColumnMetaData metaData = columnsMetaData[index];
            if (metaData != null) {
                // actual meta data
                result.addColumn(columnName, metaData.getSqlTypeInt(), metaData.getPrecision(), metaData.getScale());
            } else {
                // default meta data values
                result.addColumn(columnName, Types.VARCHAR, Integer.MAX_VALUE, 0);
            }
            index++;
        }
        return result;
    }

    private void initRead() throws IOException {
        if (input == null) {
            try {
                Path filePath = this.dataExportPath.resolve(!this.fileName.endsWith(CSV_FILE_EXTENSION) ? this.fileName + CSV_FILE_EXTENSION : this.fileName);
                InputStream in = Files.newInputStream(filePath);
                in = new BufferedInputStream(in, IO_BUFFER_SIZE);
                input = new InputStreamReader(in, characterSet);
            } catch (IOException e) {
                close();
                throw e;
            }
        }
        if (!input.markSupported()) {
            input = new BufferedReader(input);
        }
        input.mark(1);
        int bom = input.read();
        if (bom != 0xfeff) {
            // Microsoft Excel compatibility
            // ignore pseudo-BOM
            input.reset();
        }
        inputBuffer = new char[IO_BUFFER_SIZE * 2];
        if (columnNames == null) {
            readHeader();
        }
    }

    /**
     * When CSV doesn't have columns in header
     */
    private void makeColumnNamesUnique() {
        for (int i = 0; i < columnNames.length; i++) {
            StringBuilder buff = new StringBuilder();
            String n = columnNames[i];
            if (n == null || n.length() == 0) {
                buff.append('C').append(i + 1);
            } else {
                buff.append(n);
            }
            for (int j = 0; j < i; j++) {
                String y = columnNames[j];
                if (buff.toString().equals(y)) {
                    buff.append('1');
                    j = -1;
                }
            }
            columnNames[i] = buff.toString();
        }
    }

    private void readHeader() throws IOException {
        ArrayList<String> list = new ArrayList<>(4);
        ArrayList<ColumnMetaData> listMeta = new ArrayList<>(4);
        while (true) {
            String v = readValue();
            if (v == null) {
                if (endOfLine) {
                    if (endOfFile || list.size() > 0) {
                        break;
                    }
                } else {
                    v = "COLUMN" + list.size();
                    list.add(v);
                }
            } else {
                if (v.length() == 0) {
                    v = "COLUMN" + list.size();
                } else if (!caseSensitiveColumnNames && isSimpleColumnName(v)) {
                    v = ConversionUtils.toUpperEnglish(v);
                }
                // process HEADER with meta data = COLUMN_NAME(TYPE|PRECISION|SCALE)
                if (v.contains(fieldTypeSeparatorStart + "")) {
                    ColumnMetaData metaData = parseMetaDataFromHeaderString(v);
                    listMeta.add(metaData);
                    list.add(metaData.getName()); // only name
                } else {
                    list.add(v); // no meta data is present
                }
                if (endOfLine) {
                    break;
                }
            }
        }
        columnNames = new String[list.size()];
        columnsMetaData = new ColumnMetaData[list.size()];
        if (listMeta.size() > 0 && listMeta.get(0) != null) {
            for (int i = 0; i < listMeta.size(); i++) {
                columnsMetaData[i] = listMeta.get(i);
            }
        }
        list.toArray(columnNames);
    }

    private boolean isEOL(int ch){
        return (ch == '\n' || ch < 0 || ch == '\r');
    }

    private boolean isWhiteSpace(int ch){
        return (ch == ' ' || ch == '\t');
    }
    private String readDelimitedValue() throws IOException {
        int ch;
        boolean containsEscape = false;
        inputBufferStart = inputBufferPos;
        int sep;
        while (true) {
            ch = readChar();
            if (ch == fieldDelimiter) {
                ch = readChar();
                if (ch != fieldDelimiter) {
                    sep = 2;
                    break;
                }
                containsEscape = true;
            } else if (ch == escapeCharacter) {
                ch = readChar();
                if (ch < 0) {
                    sep = 1;
                    break;
                }
                containsEscape = true;
            } else if (ch < 0) {
                sep = 1;
                break;
            }
        }
        String s = new String(inputBuffer,
                inputBufferStart, inputBufferPos - inputBufferStart - sep);
        if (containsEscape) {
            s = unEscape(s);
        }
        inputBufferStart = -1;
        while (true) {
            if (ch == fieldSeparatorRead) {
                break;
            } else if (isEOL(ch)) {
                endOfLine = true;
                break;
            } else if (isWhiteSpace(ch)) {
                // ignore
            } else {
                pushBack();
                break;
            }
            ch = readChar();
        }
        return s;
    }
    private String readQuotedTextValue() throws IOException {
        int ch;
        int state=1;
        boolean endLex=false;
        while (!endLex) {
            ch = readChar();
            switch (state) {
                case 0: //Error state
                    break;
                case 1:
                    if (ch == textFieldCharacter) {
                        state = 2;
                    } else if (isEOL(ch)) {
                        state = 0;//error state
                        endLex = true;
                        endOfLine = true;
                    }
                    break;
                case 2:
                    if (ch == textFieldCharacter) {
                        state = 1;
                    } else if (ch == fieldSeparatorRead) {
                        state=3;//end state
                        endLex = true;
                    } else if (isEOL(ch)) {
                        state=3;//end state
                        endLex = true;
                        endOfLine = true;
                    } else if (isWhiteSpace(ch)) {
                        //ignore
                    } else {
                        state = 0;
                        endLex = true;
                    }
                    break;
            }
        }
        String s = new String(inputBuffer,
                inputBufferStart, inputBufferPos - inputBufferStart - 1);
        if (!preserveWhitespace) {
            s = s.trim();
        }
        inputBufferStart = -1;
        return s;
    }

    private String readArrayValue() throws IOException {
        // start SQL ARRAY processing written as = (x, y, z)
        // read until of 'arrayEndToken' symbol
        inputBufferStart = inputBufferPos;
        int ch;
        int state=1;
        boolean endLex=false;
        while (!endLex) {
            ch = readChar();
            switch (state){
                case 1:
                    if (ch == arrayEndToken) {
                        endLex = true;
                        state = 5;
                    }else if (isEOL(ch)){
                        endOfLine = true;
                        endLex = true;
                        state = 0;
                    }else if(ch == textFieldCharacter) {
                        state = 2;
                    }else if(ch == fieldSeparatorRead){
                        replaceChar(eotCharacter);
                    }else{
                        state = 3;
                    }
                    break;
                case 2:
                    if (isEOL(ch)){
                        endOfLine = true;
                        endLex = true;
                        state = 0;
                    }else if(ch == textFieldCharacter) {
                        state = 4;
                    }
                    break;
                case 3:
                    if (ch == arrayEndToken) {
                        endLex = true;
                        state = 5;
                    }else if (isEOL(ch)){
                        endOfLine = true;
                        endLex = true;
                        state = 0;
                    }else if(ch == fieldSeparatorRead){
                        replaceChar(eotCharacter);
                    }
                    break;
                case 4:
                    if (ch == arrayEndToken) {
                        endLex = true;
                        state = 5;
                    }else if (isEOL(ch)){
                        endOfLine = true;
                        endLex = true;
                        state = 0;
                    }else if(ch == textFieldCharacter) {
                        state = 2;
                    }else if(ch == fieldSeparatorRead) {
                        replaceChar(eotCharacter);
                        state = 1;
                    }else if (isWhiteSpace(ch)){
                        //ignore
                    }else{
                        endLex = true;
                        state = 0;
                    }
                    break;
            }
        }
        String s = new String(inputBuffer,
                inputBufferStart, inputBufferPos - inputBufferStart - 1);
        if (!preserveWhitespace) {
            s = s.trim();
        }
        if(!endOfLine) {
            ch = readChar();
            endOfLine=isEOL(ch);
        }
        inputBufferStart = -1; // reset
        return readNull(s);
    }

    private String readUndelimitedValue() throws IOException {
        // un-delimited value
        int ch;
        while (true) {
            ch = readChar();
            if (ch == fieldSeparatorRead) {
                break;
            } else if (isEOL(ch)) {
                endOfLine = true;
                break;
            }
        }
        String s = new String(inputBuffer,
                inputBufferStart, inputBufferPos - inputBufferStart - 1);
        if (!preserveWhitespace) {
            s = s.trim();
        }
        inputBufferStart = -1;
        // check un-delimited value for nullString
        return readNull(s);
    }

    private void skipComments() throws IOException {
        // comment until end of line
        int ch;
        inputBufferStart = -1;
        while (true) {
            ch = readChar();
            if (isEOL(ch)) {
                break;
            }
        }
        endOfLine = true;
    }

    /**
     * Key method for reading one column value as string.
     * It reads HEADER column first and data column in row.
     *
     * @return one column data OR NULL (for header or data row)
     * @throws IOException
     */
    private String readValue() throws IOException {
        endOfLine = false;
        inputBufferStart = inputBufferPos;
        while (true) {
            int ch = readChar();
            if (ch == fieldDelimiter) {
                // delimited value
                return readDelimitedValue();
            } else if (ch == textFieldCharacter) {
                return readQuotedTextValue();
            } else if (isEOL(ch)) {
                endOfLine = true;
                return null;
            } else if (ch == fieldSeparatorRead) {
                // null
                return null;
            } else if (ch <= ' ') {
                // ignore spaces
            } else if (lineComment != 0 && ch == lineComment) {
                skipComments();
                return null;
            } else if (arrayStartToken != 0 && ch == arrayStartToken) { // ARRAY processing stuff - first check '('
                return readArrayValue();
            } else {
                return readUndelimitedValue();
            }
        }
    }

    private void replaceChar(char r){
        if(inputBufferPos>=1) {
            inputBuffer[inputBufferPos - 1] = r;
        }
    }

    private void pushBack() {
        inputBufferPos--;
    }

    private int readChar() throws IOException {
        if (inputBufferPos >= inputBufferEnd) {
            return readBuffer();
        }
        return inputBuffer[inputBufferPos++];
    }

    private int readBuffer() throws IOException {
        if (endOfFile) {
            return -1;
        }
        int keep;
        if (inputBufferStart >= 0) {
            keep = inputBufferPos - inputBufferStart;
            if (keep > 0) {
                char[] src = inputBuffer;
                if (keep + IO_BUFFER_SIZE > src.length) {
                    inputBuffer = new char[src.length * 2];
                }
                System.arraycopy(src, inputBufferStart, inputBuffer, 0, keep);
            }
            inputBufferStart = 0;
        } else {
            keep = 0;
        }
        inputBufferPos = keep;
        int len = input.read(inputBuffer, keep, IO_BUFFER_SIZE);
        if (len == -1) {
            // ensure bufferPos > bufferEnd
            // even after pushBack
            inputBufferEnd = -1024;
            endOfFile = true;
            // ensure the right number of characters are read
            // in case the input buffer is still used
            inputBufferPos++;
            return -1;
        }
        inputBufferEnd = keep + len;
        return inputBuffer[inputBufferPos++];
    }

    private String readNull(String s) {
        return s.equals(nullString) ? null : s;
    }

    private String unEscape(String s) {
        StringBuilder buff = new StringBuilder(s.length());
        int start = 0;
        char[] chars = null;
        while (true) {
            int idx = s.indexOf(escapeCharacter, start);
            if (idx < 0) {
                idx = s.indexOf(fieldDelimiter, start);
                if (idx < 0) {
                    break;
                }
            }
            if (chars == null) {
                chars = s.toCharArray();
            }
            buff.append(chars, start, idx - start);
            if (idx == s.length() - 1) {
                start = s.length();
                break;
            }
            buff.append(chars[idx + 1]);
            start = idx + 2;
        }
        buff.append(s.substring(start));
        return buff.toString();
    }

    /**
     * Method is triggered when code:
     * <pre>{@code
     *      while(ResultSet rs.next() ) {
     *          // internal loop over columns values here
     *      }
     * }</pre>
     * @return
     * @throws SQLException
     */
    @Override
    public Object[] readRow() throws SQLException {
        if (input == null) {
            return null;
        }
        String[] row = new String[columnNames.length];
        try {
            int i = 0;
            while (true) {
                String v = readValue(); // main method to read single column value
//                log.trace("readValue() = '{}'", v);
                if (v == null) {
                    if (endOfLine) {
                        if (i == 0) {
                            if (endOfFile) {
                                return null;
                            }
                            // empty line
                            continue;
                        }
                        break;
                    }
                }
                if (i < row.length) {
                    row[i++] = v;
                }
                if (endOfLine) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new SQLException("IOException reading from " + fileName, e);
        }
//        log.trace("Prepared row = {}", Arrays.toString(row));
        return row;
    }

    private boolean isSimpleColumnName(String columnName) {
        for (int i = 0, length = columnName.length(); i < length; i++) {
            char ch = columnName.charAt(i);
            if (i == 0) {
                if (ch != '_' && !Character.isLetter(ch)) {
                    return false;
                }
            } else {
                if (ch != '_' && !Character.isLetterOrDigit(ch)) {
                    return false;
                }
            }
        }
        if (columnName.length() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Extract meta data from CVS Header for one column in row
     * @param columnWithMetaData string with meta data like - ID(-5|7|2)
     * @return meta data instance
     */
    private ColumnMetaData parseMetaDataFromHeaderString(String columnWithMetaData) {
        log.debug("Column string to parse '{}'", columnWithMetaData);
        int starTypeDelimiter = columnWithMetaData.indexOf(fieldTypeSeparatorStart + "");
        String columnName = columnWithMetaData.substring(0, starTypeDelimiter);
        int endTypeDelimiter = columnWithMetaData.lastIndexOf(fieldTypeSeparatorEnd + "");
        String columnTypeInfo = columnWithMetaData.substring(starTypeDelimiter + 1, endTypeDelimiter);
        log.trace("Column '{}' TypeInfo to parse '{}'", columnName, columnTypeInfo);
        String[] typePrecisionScale = columnTypeInfo.split("\\|");

        if (typePrecisionScale.length != 3) {
            String error = "Incorrect type info was supplied from CSV file," +
                    " not enough data, 3 is expected, but found " + typePrecisionScale.length;
            log.error(error);
            throw new IllegalStateException(error);
        }
        int sqlType = -1, sqlPrecision = -1, sqlScale = -1;
        for (int j = 0; j < typePrecisionScale.length; j++) {
            String typeValueAsString = typePrecisionScale[j];
            String error = "Incorrect type VALUE was supplied from CSV file," +
                    " found " + typeValueAsString;
            if (typeValueAsString == null || typeValueAsString.isEmpty()) {
                log.error(error);
                throw new IllegalStateException(error);
            }
            try {
                int parsedValue = Integer.parseInt(typeValueAsString);
                switch (j) {
                    case 0: sqlType = parsedValue; break;
                    case 1: sqlPrecision = parsedValue; break;
                    case 2: sqlScale = parsedValue; break;
                    default: throw new IllegalStateException(error);
                }
            } catch (Exception e) {
                log.error("Incorrect number was supplied = " + typeValueAsString, e);
            }
        }
        return new ColumnMetaData(columnName, null, sqlType, sqlPrecision, sqlScale);
    }

    @Override
    public void close() {
        CsvFileUtils.closeSilently(input);
        input = null;
        columnsMetaData = null;
        this.endOfLine = false; // prepare for next CSV file
        this.endOfFile = false; // prepare for next CSV file
    }

    /**
     * INTERNAL
     */
    @Override
    public void reset() throws SQLException {
        throw new SQLException("Method is not supported by CsvReader", "CSV");
    }


}
