/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Add fulltext search support for database data, also simplify work with fulltext search and
 * provide indexing functions
 */
public interface FullTextSearchService {
    /**
     * Reindex one table
     *
     * @param conn       SQL connection
     * @param tableName  name of table for reindexing
     * @param schemaName name of db schema, where table is located
     * @throws SQLException when unable to reindex table
     */
    void reindex(Connection conn, String tableName, String schemaName) throws SQLException;

    /**
     * Reindex all indexed tables
     *
     * @param conn SQL connection
     * @throws SQLException when unable to reindex tables
     */
    void reindexAll(Connection conn) throws SQLException;

    /**
     * Create new index for table to support fulltext search
     *
     * @param conn       DB connection
     * @param schema     db schema of table for indexing
     * @param table      name of table for indexing
     * @param columnList list of columns for indexing separated by comma
     * @throws SQLException when unable to create index
     */
    void createIndex(Connection conn, String schema, String table, String columnList) throws SQLException;


    /**
     * Drop the fulltext index for a table
     *
     * @param conn   SQL connection
     * @param schema Schema name
     * @param table  Table name
     * @throws SQLException Unable to drop fulltext index
     */
    void dropIndex(Connection conn, String schema, String table) throws SQLException;

    /**
     * Initialize the fulltext support for a new database.
     * <p>
     * This method should be called from performing the database init
     * that enables fulltext search support
     * <p>
     * <p>
     * Implementations should provide an ability for multiple init and shutdown operations on one instance
     * <p>
     */
    void init();

    /**
     * Drop all fulltext indexes
     *
     * @param conn SQL connection
     * @throws SQLException Unable to drop fulltext indexes
     */
    void dropAll(Connection conn) throws SQLException;

    /**
     * Perform index search. By default should forward method call
     * to {@link FullTextSearchEngine#search(String, String, String, int, int)}
     */
    ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException;

    /**
     * Shutdown fulltext search service.
     * Implementations should provide an ability for multiple init and shutdown operations on one instance
     */
    void shutdown();

}
