/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExportData;

import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Class is used for writing into CSV (comma separated values) file.
 * Folder for stored and parsed CSV files should be specified in implementation class.
 *
 * @author yuriy.larin
 */
public interface CsvWriter extends AutoCloseable {

    /**
     * Writes the result set to a file in the CSV format.
     *
     * @param writer the writer
     * @param rs     the result set
     * @return the number of rows written
     */
    CsvExportData write(Writer writer, ResultSet rs) throws SQLException;

    /**
     * Writes the result set to a newly create file in the CSV format.
     *
     * @param outputFileName the name of the csv file
     * @param rs             the result set - the result set must be positioned before the first row.
     * @return the number of rows written
     */
    CsvExportData write(String outputFileName, ResultSet rs) throws SQLException;

    /**
     * Appends the result set to a previously create file in the CSV format.
     *
     * @param outputFileName the name of the csv file
     * @param rs             the result set - the result set must be positioned before the first row.
     * @return the number of rows written
     */
    CsvExportData append(String outputFileName, ResultSet rs) throws SQLException;

    /**
     * Appends rs to the file (if the file exists - will append to the end, otherwise - create new)
     * Will set default values for specified columns
     *
     * @param outputFileName name of file to export
     * @param rs             result set with a data to export
     * @param defaultValues  map of pairs (column_name->column_value)
     * @return {@link CsvExportData} with number of exported rows and last row itself
     * @throws SQLException when access to the rs cause to the exception
     */
    CsvExportData append(String outputFileName, ResultSet rs, Map<String, String> defaultValues) throws SQLException;

    /**
     * Writes the result set of a query to a file in the CSV format.
     *
     * @param conn           the connection
     * @param outputFileName the file name
     * @param sql            the query
     * @param charset        the charset or null to use the system default charset
     *                       (see system property file.encoding)
     * @return the number of rows written
     */
    CsvExportData write(Connection conn, String outputFileName, String sql, String charset) throws SQLException;

    /**
     * TODO: refactor that method to using another configuration approach (properties or similar)
     * <p>
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
     * Close all files and reset metadata
     */
    void close();

}
