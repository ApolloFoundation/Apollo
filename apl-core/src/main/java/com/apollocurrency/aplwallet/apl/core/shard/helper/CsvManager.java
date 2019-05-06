package com.apollocurrency.aplwallet.apl.core.shard.helper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.derived.MinMaxDbId;

public interface CsvManager {

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
     * @param inputFileName the file name
     * @param colNames or null if the column names should be read from the CSV
     *          file
     * @param charset the charset or null to use the system default charset
     *          (see system property file.encoding)
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
}
