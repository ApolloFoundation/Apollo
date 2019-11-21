/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.charset.Charset;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.ColumnMetaData;
import com.apollocurrency.aplwallet.apl.core.shard.util.ConversionUtils;
import org.slf4j.Logger;

/**
 * Abstract Base class for CSV writer, reader classes. It is manly used for storing and
 * configuration common properties.
 *
 * @author yuriy.larin
 */
public abstract class CsvAbstractBase {
    private static final Logger log = getLogger(CsvAbstractBase.class);

    /**
     * The block size for I/O operations.
     */
    public static final int IO_BUFFER_SIZE = 4 * 1024;

    /**
     * System property <code>line.separator</code> (default: \n).<br />
     * It is usually set by the system, and used by the script and trace tools.
     */
    public static final String LINE_SEPARATOR = "\n";
    /**
     * UTF-8 is expected here
     */
    public static final String FILE_ENCODING = Charset.forName("UTF-8").name(); // UTF-8 default
    protected String characterSet = FILE_ENCODING;

    public static final String CSV_FILE_EXTENSION = ".csv"; // UTF-8

    protected Path dataExportPath; // common path for all CSV files are stored in
    protected String fileName; // file name (by table name)
    protected String fileNameExtension = CSV_FILE_EXTENSION; // file name extension

    protected String[] columnNames;
    protected ColumnMetaData[] columnsMetaData; // full meta data about sql table columns

    protected String lineSeparator = LINE_SEPARATOR;
    protected String nullString = "null";// it's better do not change that value

    protected char escapeCharacter = '\"';
    protected char fieldDelimiter = '\"';
    protected char fieldTypeSeparatorStart = '('; // use here only non-alphanumeric characters, no space here
    protected char fieldTypeSeparatorEnd = ')'; // use here only non-alphanumeric characters, no space here
    protected char fieldSeparatorRead = ',';
    protected String fieldSeparatorWrite = ",";

    // CSV READER only config parameters
    protected boolean caseSensitiveColumnNames; // config param
    protected boolean preserveWhitespace; // config param
    protected char lineComment;
    protected char arrayStartToken = '('; // start sql array
    protected char arrayEndToken = ')'; // finish sql array
    protected char textFieldCharacter = '\'';

    // CVS WRITER only config parameters
    protected boolean writeColumnHeader = true; // if HEADER is not written (false), we CAN'T store skipped column index !!

    /**
     * TODO: refactor that functionality to using another configuration approach (properties or similar)
     * Parse and set the CSV options.
     *
     * @param options the the options
     * @return the character set
     */
    public String setOptions(String options) {
        String charset = null;
        String[] keyValuePairs = ConversionUtils.arraySplit(options, ' ', false);
        if (keyValuePairs == null) {
            log.debug("No OPTIONS were found");
            return "";
        }
        for (String pair : keyValuePairs) {
            if (pair.length() == 0) {
                continue;
            }
            int index = pair.indexOf('=');
            String key = ConversionUtils.trim(pair.substring(0, index), true, true, " ");
            String value = pair.substring(index + 1);
            char ch = value.length() == 0 ? 0 : value.charAt(0);
            if (isParam(key, "escape", "esc", "escapeCharacter")) {
                setEscapeCharacter(ch);
            } else if (isParam(key, "fieldDelimiter", "fieldDelim")) {
                setFieldDelimiter(ch);
            } else if (isParam(key, "fileNameExtension", "fieldDelim")) {
                setFileNameExtension(value);
            } else if (isParam(key, "fieldSeparator", "fieldSep")) {
                setFieldSeparatorRead(ch);
                setFieldSeparatorWrite(value);
            } else if (isParam(key, "lineComment", "lineCommentCharacter")) {
                setLineCommentCharacter(ch);
            } else if (isParam(key, "lineSeparator", "lineSep")) {
                setLineSeparator(value);
            } else if (isParam(key, "null", "nullString")) {
                setNullString(value);
            } else if (isParam(key, "charset", "characterSet")) {
                charset = value;
            } else if (isParam(key, "preserveWhitespace")) {
                setPreserveWhitespace(Boolean.parseBoolean(value));
            } else if (isParam(key, "writeColumnHeader")) {
                setWriteColumnHeader(Boolean.parseBoolean(value));
            } else if (isParam(key, "caseSensitiveColumnNames")) {
                setCaseSensitiveColumnNames(Boolean.parseBoolean(value));
            } else {
                throw new RuntimeException("Key '" + key + "' not found in config");
            }
        }
        return charset;
    }

    private static boolean isParam(String key, String... values) {
        for (String v : values) {
            if (key.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Override the field separator for writing. The default is ",".
     *
     * @param fieldSeparatorWrite the field separator
     */
    public void setFieldSeparatorWrite(String fieldSeparatorWrite) {
        this.fieldSeparatorWrite = fieldSeparatorWrite;
    }

    /**
     * Get the current field separator for writing.
     *
     * @return the field separator
     */
    public String getFieldSeparatorWrite() {
        return fieldSeparatorWrite;
    }

    /**
     * Override the case sensitive column names setting. The default is false.
     * If enabled, the case of all column names is always preserved.
     *
     * @param caseSensitiveColumnNames whether column names are case sensitive
     */
    public void setCaseSensitiveColumnNames(boolean caseSensitiveColumnNames) {
        this.caseSensitiveColumnNames = caseSensitiveColumnNames;
    }

    /**
     * Get the current case sensitive column names setting.
     *
     * @return whether column names are case sensitive
     */
    public boolean getCaseSensitiveColumnNames() {
        return caseSensitiveColumnNames;
    }

    /**
     * Override the field separator for reading. The default is ','.
     *
     * @param fieldSeparatorRead the field separator
     */
    public void setFieldSeparatorRead(char fieldSeparatorRead) {
        this.fieldSeparatorRead = fieldSeparatorRead;
    }

    /**
     * Get the current field separator for reading.
     *
     * @return the field separator
     */
    public char getFieldSeparatorRead() {
        return fieldSeparatorRead;
    }

    /**
     * Set the line comment character. The default is character code 0 (line
     * comments are disabled).
     *
     * @param lineCommentCharacter the line comment character
     */
    public void setLineCommentCharacter(char lineCommentCharacter) {
        this.lineComment = lineCommentCharacter;
    }

    /**
     * Get the line comment character.
     *
     * @return the line comment character, or 0 if disabled
     */
    public char getLineCommentCharacter() {
        return lineComment;
    }

    /**
     * Set the field delimiter. The default is " (a double quote).
     * The value 0 means no field delimiter is used.
     *
     * @param fieldDelimiter the field delimiter
     */
    public void setFieldDelimiter(char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    /**
     * Get the current field delimiter.
     *
     * @return the field delimiter
     */
    public char getFieldDelimiter() {
        return fieldDelimiter;
    }

    public String getFileNameExtension() {
        return fileNameExtension;
    }

    public void setFileNameExtension(String fileNameExtension) {
        this.fileNameExtension = fileNameExtension;
    }

    /**
     * Set the escape character. The escape character is used to escape the
     * field delimiter. This is needed if the data contains the field delimiter.
     * The default escape character is " (a double quote), which is the same as
     * the field delimiter. If the field delimiter and the escape character are
     * both " (double quote), and the data contains a double quote, then an
     * additional double quote is added. Example:
     * <pre>
     * Data: He said "Hello".
     * Escape character: "
     * Field delimiter: "
     * CSV file: "He said ""Hello""."
     * </pre>
     * If the field delimiter is a double quote and the escape character is a
     * backslash, then escaping is done similar to Java (however, only the field
     * delimiter is escaped). Example:
     * <pre>
     * Data: He said "Hello".
     * Escape character: \
     * Field delimiter: "
     * CSV file: "He said \"Hello\"."
     * </pre>
     * The value 0 means no escape character is used.
     *
     * @param escapeCharacter the escape character
     */
    public void setEscapeCharacter(char escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }

    /**
     * Get the current escape character.
     *
     * @return the escape character
     */
    public char getEscapeCharacter() {
        return escapeCharacter;
    }

    /**
     * Set the line separator used for writing. This is usually a line feed (\n
     * or \r\n depending on the system settings). The line separator is written
     * after each row (including the last row), so this option can include an
     * end-of-row marker if needed.
     *
     * @param lineSeparator the line separator
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    /**
     * Get the line separator used for writing.
     *
     * @return the line separator
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Set the value that represents NULL. It is only used for non-delimited
     * values.
     *
     * @param nullString the null
     */
    public void setNullString(String nullString) {
        this.nullString = nullString;
    }

    /**
     * Get the current null string.
     *
     * @return the null string.
     */
    public String getNullString() {
        return nullString;
    }

    /**
     * Enable or disable preserving whitespace in unquoted text.
     *
     * @param value the new value for the setting
     */
    public void setPreserveWhitespace(boolean value) {
        this.preserveWhitespace = value;
    }

    /**
     * Whether whitespace in unquoted text is preserved.
     *
     * @return the current value for the setting
     */
    public boolean getPreserveWhitespace() {
        return preserveWhitespace;
    }

    /**
     * Enable or disable writing the column header.
     *
     * @param value the new value for the setting
     */
    public void setWriteColumnHeader(boolean value) {
        this.writeColumnHeader = value;
    }

    /**
     * Whether the column header is written.
     *
     * @return the current value for the setting
     */
    public boolean getWriteColumnHeader() {
        return writeColumnHeader;
    }


}
