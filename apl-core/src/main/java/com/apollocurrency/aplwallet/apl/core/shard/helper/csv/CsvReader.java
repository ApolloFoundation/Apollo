/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import java.io.IOException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class is used reading from CSV (comma separated values) file.
 * Folder for stored and parsed CSV files should be specified in implementation class.
 *
 * @author yuriy.larin
 */
public interface CsvReader extends AutoCloseable {

    /**
     * Reads from the CSV file and returns a result set. The rows in the result
     * set are created on demand, that means the file is kept open until all
     * rows are read or the result set is closed.
     * <br />
     * If the columns are read from the CSV file, then the following rules are
     * used: columns names that start with a letter or '_', and only
     * contain letters, '_', and digits, are considered case insensitive
     * and are converted to uppercase. Other column names are considered
     * case sensitive (that means they need to be quoted when accessed).
     *
     * @param inputFileName the file name, not NULL
     * @param colNames or null if the column names should be read from the CSV file
     * @param charset the charset or null to use the system default charset (see system property file.encoding)
     * @return the result set
     */
    ResultSet read(String inputFileName, String[] colNames, String charset) throws SQLException;

    /**
     * Reads CSV data from a reader and returns a result set. The rows in the
     * result set are created on demand, that means the reader is kept open
     * until all rows are read or the result set is closed.
     *
     * @param reader the reader
     * @param colNames or null if the column names should be read from the CSV
     *            file
     * @return the result set
     */
    ResultSet read(Reader reader, String[] colNames) throws IOException;

    /**
     * TODO: refactor that method to using another configuration approach (properties or similar)
     *
     * Set String for Option values as 'key='value'. Possible parameters with default values are following:
     * escape='\"'
     * fieldDelimiter='\"'
     * fieldSeparator=',' on READ and WRITE
     * lineComment="\n"
     * charset='UTF-8'
     * nullString="null"
     * fileNameExtension=".csv"
     *
     * @param options the options line separated by space. Example : escape='\"' fieldDelimiter='\"'
     * @return the character set
     */
    String setOptions(String options);

    /**
     * UnEscape the fieldDelimiter character that already escaped with the escape character.
     * @param data source escaped string
     * @return unescaped string
     */
    String unEscape(String data);


}
