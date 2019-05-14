/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.helper.csv;

import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;

/**
 * Class is used for writing into CSV (comma separated values) file.
 * Folder for stored and parsed CSV files should be specified in implementation class.
 *
 * @author yuriy.larin
 */
public interface CsvWriter {

    /**
     * Writes the result set to a file in the CSV format.
     *
     * @param writer the writer
     * @param rs the result set
     * @param minMaxDbId instance with current minimal / maximal DB_ID values got pagination
     * @return the number of rows written
     */
    int write(Writer writer, ResultSet rs, MinMaxDbId minMaxDbId) throws SQLException;

    /**
     * Writes the result set to a newly create file in the CSV format.
     *
     * @param outputFileName the name of the csv file
     * @param rs the result set - the result set must be positioned before the first row.
     * @param minMaxDbId instance with current minimal / maximal DB_ID values got pagination
     * @return the number of rows written
     */
    int write(String outputFileName, ResultSet rs, MinMaxDbId minMaxDbId) throws SQLException;

    /**
     * Appends the result set to a previously create file in the CSV format.
     *
     * @param outputFileName the name of the csv file
     * @param rs the result set - the result set must be positioned before the first row.
     * @param minMaxDbId instance with current minimal / maximal DB_ID values got pagination
     * @return the number of rows written
     */
    int append(String outputFileName, ResultSet rs, MinMaxDbId minMaxDbId) throws SQLException;

    /**
     * Method is used with 'append' mode for explicit releasing resources
     */
    void close();

    /**
     * Writes the result set of a query to a file in the CSV format.
     *
     * @param conn the connection
     * @param outputFileName the file name
     * @param sql the query
     * @param charset the charset or null to use the system default charset
     *          (see system property file.encoding)
     * @param minMaxDbId instance with current minimal / maximal DB_ID values got pagination
     * @return the number of rows written
     */
    int write(Connection conn, String outputFileName, String sql, String charset, MinMaxDbId minMaxDbId) throws SQLException;

    /**
     * Set String for Option values as 'key='value'. Possible parameters with default values are following:
     * escape='\"'
     * fieldDelimiter='\"'
     * fieldSeparator=',' on READ and WRITE
     * lineComment="\n"
     * charset='UTF-8'
     * nullString="null"
     *
     * @param options the options line separated by space. Example : escape='\"' fieldDelimiter='\"'
     * @return the character set
     */
    String setOptions(String options);
}
