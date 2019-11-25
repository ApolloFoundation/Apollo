/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class FullTextSearchServiceImpl implements FullTextSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(FullTextSearchServiceImpl.class);
    private FullTextSearchEngine ftl;
    private Set<String> indexTables;
    private String schemaName;
    private DatabaseManager databaseManager;

    @Inject
    public FullTextSearchServiceImpl(DatabaseManager databaseManager, FullTextSearchEngine ftl,
                                     @Named(value = "fullTextTables") Set<String> indexTables,
                                     @Named(value = "tablesSchema") String schemaName) {
        this.databaseManager = databaseManager;
        this.ftl = ftl;
        this.indexTables = indexTables;
        this.schemaName = schemaName;
    }
    /**
     * Create the fulltext index for a table
     *
     * @param   conn                SQL connection
     * @param   schema              Schema name
     * @param   table               Table name
     * @param   columnList          Indexed column names separated by commas
     * @throws  SQLException        Unable to create fulltext index
     */
    public void createIndex(Connection conn, String schema, String table, String columnList)
            throws SQLException {
    }

    /**
     * Drop the fulltext index for a table
     *
     * @param   conn                SQL connection
     * @param   schema              Schema name
     * @param   table               Table name
     * @throws  SQLException        Unable to drop fulltext index
     */
    public void dropIndex(Connection conn, String schema, String table) throws SQLException {
    }

    /**
     * Initialize the fulltext support for a new database
     *
     * This method should be called from AplDbVersion when performing the database version update
     * that enables fulltext search support
     */
    public void init() {
    }
    /**
     * Drop all fulltext indexes
     *
     * @param   conn                SQL connection
     * @throws  SQLException        Unable to drop fulltext indexes
     */
    public void dropAll(Connection conn) throws SQLException {
    }
    public ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException {
        return null;
    }

    @Override
    public void shutdown() {
    }

    public void reindex(Connection conn, String tableName, String schemaName) throws SQLException {
    }

    public void reindexAll(Connection conn) throws SQLException {
    }
    private void reindexAll(Connection conn, Set<String> tables, String schema) throws SQLException {

    }
}
