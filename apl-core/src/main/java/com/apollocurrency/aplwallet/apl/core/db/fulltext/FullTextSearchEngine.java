/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represent fulltext search low-level implementations
 */
public interface FullTextSearchEngine {
    /**
     * Initialize fulltext search engine. New initialization can be performed after shutdown
     * @throws IOException when IO error occurred
     */
    void init() throws IOException;

    /**
     * Shutdown fulltext search engine.
     */
    void shutdown();

    /**
     * Index one row of indexed columns for table
     * @param row represent table row, which consist of indexed columns with values to index
     * @param tableData general information about indexed table(db_id, indexed columns, column names)
     * @throws SQLException when index error occurred
     */
    void indexRow(Object[] row, TableData tableData) throws SQLException;

    /**
     * Commit current index immediately
     * @throws SQLException when index commit error occurred
     */
    void commitIndex() throws SQLException;

    /**
     * Clear index data and restart engine
     * @throws  SQLException  index error occurred
     */
    void clearIndex() throws SQLException;
    /**
     * Update index for a committed row
     * @param oldRow row with old indexed values
     * @param newRow row with new values to index
     * @param tableData general information about indexed table
     * @throws SQLException when index error occurred
     */
    void commitRow(Object[] oldRow, Object[] newRow, TableData tableData) throws SQLException;

    /**
     * Search the index
     *
     * The result set will have the following columns:
     *   SCHEMA  - Schema name (String)
     *   TABLE   - Table name (String)
     *   COLUMNS - Primary key column names (String[]) - this is always DB_ID
     *   KEYS    - Primary key values (Long[]) - this is always the DB_ID value for the table row
     *   SCORE   - Lucene score (Float)
     *
     * @param   schema              Schema name
     * @param   table               Table name
     * @param   queryText           Query expression
     * @param   limit               Number of rows to return
     * @param   offset              Offset with result set
     * @return                      Search results
     * @throws  SQLException        Unable to search the index
     */
    ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException;
}
