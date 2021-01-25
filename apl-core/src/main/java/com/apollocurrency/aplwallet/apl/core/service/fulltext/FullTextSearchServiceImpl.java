/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;

@Slf4j
@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class FullTextSearchServiceImpl implements FullTextSearchService {
public static final String FTL_INDEXES_TABLE = "ftl_indexes";
    public static final String UNABLE_TO_CREATE_LUCENE_SEARCH_INDEX_FOR_TABLE_MSG = "Unable to create Lucene search index for table ";
    private final FullTextSearchEngine fullTextSearchEngine;
    private final Map<String, String> fullTextSearchIndexedTables;
    private final String schemaName;
    private final DatabaseManager databaseManager;

    @Inject
    public FullTextSearchServiceImpl(DatabaseManager databaseManager, FullTextSearchEngine fullTextSearchEngine,
                                     @Named(value = "fullTextTables") Map<String, String> fullTextSearchIndexedTables,
                                     @Named(value = "tablesSchema") String schemaName) {
        this.databaseManager = databaseManager;
        this.fullTextSearchEngine = fullTextSearchEngine;
        this.fullTextSearchIndexedTables = fullTextSearchIndexedTables;
        this.schemaName = schemaName;
    }

    /**
     * Drop the fulltext index for a table
     *
     * @param conn   SQL connection
     * @param schema Schema name
     * @param table  Table name
     * @throws SQLException Unable to drop fulltext index
     */
    public void dropIndex(Connection conn, String schema, String table) throws SQLException {
        String upperSchema = schema.toLowerCase();
        String upperTable = table.toLowerCase();
        boolean reindex = false;
        //
        // Drop an existing database trigger
        //
        try (Statement qstmt = conn.createStatement();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = qstmt.executeQuery(String.format(
                "SELECT columns FROM " + FTL_INDEXES_TABLE
                    + " WHERE `schema` = '%s' AND `table` = '%s'",
                upperSchema, upperTable))) {
                if (rs.next()) {
                    stmt.execute(String.format("DELETE FROM " + FTL_INDEXES_TABLE
                            + " WHERE `schema` = '%s' AND `table` = '%s'",
                        upperSchema, upperTable));
                    reindex = true;
                }
            }
        }
        //
        // Rebuild the Lucene index
        //
        if (reindex) {
            reindexAll(conn, schema);
        }
    }

    /**
     * Initialize the fulltext support for a new database
     * <p>
     * This method should be called from AplDbVersion when performing the database version update
     * that enables fulltext search support
     */
    public void init() {
        boolean isIndexFolderEmpty;
        try {
            isIndexFolderEmpty = fullTextSearchEngine.isIndexFolderEmpty(); // first check if index is deleted manually
            log.debug("init = (isIndexFolderEmpty = {})", isIndexFolderEmpty);
            fullTextSearchEngine.init(); // some files are created by initialization
        } catch (IOException e) {
            throw new RuntimeException("Unable to init fulltext engine", e);
        }
        // store lucene indexed data: table + schema + columns
        log.debug("fullTextTableName = {}", FTL_INDEXES_TABLE);
//        log.debug("triggerClassName = {}", triggerClassName);
        try (Connection conn = databaseManager.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             Statement qstmt = conn.createStatement()) {
            //
            // Check if we have already been initialized.
            //
            boolean alreadyInitialized = true;
            boolean indexesInfoTableExist = false;
            try (ResultSet rs = qstmt.executeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_NAME = '" + FTL_INDEXES_TABLE + "'"
                    + "  AND TABLE_SCHEMA = '" + databaseManager.getBaseDbProperties().getDbName() + "'")) {
                while (rs.next()) {
                    indexesInfoTableExist = true;
                    if (!rs.getString(1).startsWith(FTL_INDEXES_TABLE)) {
                        alreadyInitialized = false;
                    }
                }
            }
            if (indexesInfoTableExist && alreadyInitialized && !isIndexFolderEmpty) {
                log.info("Fulltext support is already initialized");
                return;
            }
            //
            // We need to delete an existing Lucene index since the V3 file format is not compatible with V5
            //
            fullTextSearchEngine.clearIndex();

            //
            // Create our schema and table
            //
            boolean createResult = stmt.execute("CREATE TABLE IF NOT EXISTS " + FTL_INDEXES_TABLE + " "
                + "(`schema` VARCHAR(20), `table` VARCHAR(100), columns VARCHAR(200), PRIMARY KEY(`schema`, `table`))");
            log.info("fulltext table is created = '{}'", createResult);
            //
            // Drop existing triggers and create our triggers.  H2 will initialize the trigger
            // when it is created.  H2 has already initialized the existing triggers and they
            // will be closed when dropped.  The H2 Lucene V3 trigger initialization will work with
            // Lucene V5, so we are able to open the database using the Lucene V5 library files.
            //
            long recordCount = -1L;
            try (ResultSet rs = qstmt.executeQuery("SELECT count(*) as count FROM " + FTL_INDEXES_TABLE)) {
                while (rs.next()) {
                    recordCount = rs.getLong("count");
                }
            }

            if (recordCount == 0 && this.fullTextSearchIndexedTables != null) { // skip if table if filled with initial data
                for (String tableName : this.fullTextSearchIndexedTables.keySet()) {
                    initTableLazyIfNotPresent(conn, stmt, tableName); // try to create something if it's present
                }
            }
            log.info("Fulltext init is DONE");
        } catch (SQLException exc) {
            log.error("Unable to initialize fulltext search support", exc);
            throw new RuntimeException(exc.toString(), exc);
        }
    }

    public void initTableLazyIfNotPresent(Connection conn, Statement stmt, String tableName) throws SQLException {
        Objects.requireNonNull(conn, "connection is NULL");
        Objects.requireNonNull(stmt, "statement is NULL");
        Objects.requireNonNull(tableName, "tableName is NULL");
        try {
            String indexedColumns = fullTextSearchIndexedTables.get(tableName);
            if (indexedColumns == null) {
                String error = String.format(
                    "Something wrong with Searchable Tables registration, because '%s' is not found in Map, sorry..."
                    , tableName);
                throw new RuntimeException(error);
            }
            // check if record has been inserted before
            log.debug("select record count from " + FTL_INDEXES_TABLE + " '{}.{}'"
                , this.schemaName.toLowerCase(), tableName.toLowerCase());
            ResultSet rs = stmt.executeQuery(
                String.format("SELECT count(*) as count FROM " + FTL_INDEXES_TABLE
                        + " WHERE `schema` = '%s' AND `table` = '%s'",
                    this.schemaName.toLowerCase(), tableName.toLowerCase()));
            if (rs.next() && rs.getLong("count") == 0) {
                log.debug("found 0 count from " + FTL_INDEXES_TABLE + "...");
                // insert if empty
                stmt.execute(String.format("INSERT INTO " + FTL_INDEXES_TABLE + " VALUES('%s', '%s', '%s')",
                    this.schemaName.toLowerCase(), tableName.toLowerCase(), indexedColumns.trim().toLowerCase()));
                reindex(conn, tableName, schemaName);
            }
            log.info("Lucene search index created for table '{}'", tableName);
        } catch (SQLException exc) {
            log.error("Unable to create Lucene search index for table '{}'", tableName);
            throw new SQLException(UNABLE_TO_CREATE_LUCENE_SEARCH_INDEX_FOR_TABLE_MSG + tableName, exc);
        }
    }

    /**
     * Drop all fulltext indexes
     *
     * @param conn SQL connection
     * @throws SQLException Unable to drop fulltext indexes
     */
    public void dropAll(Connection conn) throws SQLException {
        //
        // Drop records about stored 'searchable' and indexed tables
        //
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE " + FTL_INDEXES_TABLE);
        }
        //
        // Delete the Lucene index
        //
        fullTextSearchEngine.clearIndex();
    }

    public ResultSet search(String schema, String table, String queryText, int limit, int offset)
        throws SQLException {
        return fullTextSearchEngine.search(schema, table, queryText, limit, offset);
    }

    @Override
    public void shutdown() {
        try {
            fullTextSearchEngine.shutdown();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void reindex(Connection conn, String tableName, String schemaName) throws SQLException {
        //
        // Build the SELECT statement for just the indexed columns
        //
        TableData tableData = DbUtils.getTableData(conn, tableName, schemaName);
        if (tableData.getDbIdColumnPosition() == -1) {
            log.debug("Table {} has not dbId column", tableName);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT db_id");
        for (int index : tableData.getIndexColumns()) {
            sb.append(", ").append(tableData.getColumnNames().get(index));
        }
        sb.append(" FROM ").append(tableName);

        boolean isIndexAppended = false;
        int insertedDocsCount = 0;
        //
        // Index each row in the table
        //
        try (Statement qstmt = conn.createStatement();
             ResultSet rs = qstmt.executeQuery(sb.toString())) {
            while (rs.next()) {
                // create full text search data set for every row fetched from DB
                FullTextOperationData operationData = new FullTextOperationData(
                    DEFAULT_SCHEMA, tableName, Thread.currentThread().getName());
                operationData.setOperationType(FullTextOperationData.OperationType.INSERT_UPDATE);
                int i = 0;
                Object dbId = rs.getObject(i + 1); // put DB_ID value
                operationData.setDbIdValue((BigInteger) dbId);
                i++;
                Iterator it = tableData.getIndexColumns().iterator();
                while (it.hasNext()) {
                    Object indexedColumnValue = rs.getObject(i + 1); // value from table can be null here
                    operationData.addColumnData(indexedColumnValue); // when it's null, we'll add "NULL" as data
                    it.next(); // move forward
                    i++;
                }
                if (!operationData.getColumnsWithData().isEmpty()) {
                    log.debug("Index data = {}", operationData);
                    fullTextSearchEngine.indexRow(operationData, tableData);
                    insertedDocsCount++;
                    isIndexAppended = true;
                }
            }
        }
        //
        // Commit the index updates
        //
        if (isIndexAppended) {
            fullTextSearchEngine.commitIndex();
            log.debug("'{}' inserted Docs from table '{}'", insertedDocsCount, tableName);
        }
    }

    public void reindexAll(Connection conn) throws SQLException {
        reindexAll(conn, schemaName);
    }

    private void reindexAll(Connection conn, String schema) throws SQLException {
        long start = System.currentTimeMillis();
        log.info("Rebuilding Lucene search index");
        try {
            //
            // Delete the current Lucene index
            //
            fullTextSearchEngine.clearIndex();
            //
            // Reindex each table
            //
            for (String tableName : this.fullTextSearchIndexedTables.keySet()) {
                long startTable = System.currentTimeMillis();
                log.debug("Reindexing '{}' starting...", tableName);
                reindex(conn, tableName, schema);
                log.debug("Reindexing '{}' DONE in '{}' ms", tableName, System.currentTimeMillis() - startTable);
            }
        } catch (SQLException exc) {
            throw new SQLException("Unable to rebuild the Lucene index", exc);
        }
        log.info("Rebuilding Lucene search index DONE in '{}' ms", System.currentTimeMillis() - start);
    }


    /**
     * Creates a new index for table to support fulltext search.
     * Note that a schema is always PUBLIC.
     *
     * @param con                   DB connection
     * @param table                 name of table for indexing
     * @param fullTextSearchColumns list of columns for indexing separated by comma
     * @throws SQLException when unable to create index
     */
    @Override
    public final void createSearchIndex(
        final Connection con,
        final String table,
        final String fullTextSearchColumns
    ) throws SQLException {
        if (fullTextSearchColumns != null) {
            log.debug("Creating search index on {} ({})", table, fullTextSearchColumns);
            String table1 = table.toLowerCase();
            String upperSchema = schemaName.toLowerCase();
            //
            // Drop an existing index and the associated database trigger
            //
            dropIndex(con, schemaName, table1);
            //
            // Update our schema and create a new database trigger.  Note that the trigger
            // will be initialized when it is created.
            //
            boolean isTableDataExist = false;
            try (Statement stmt = con.createStatement()) {
                ResultSet rs = stmt.executeQuery(String.format("SELECT count(*) as count FROM " + FTL_INDEXES_TABLE
                        + " WHERE `schema` = '%s' AND `table` = '%s'",
                    upperSchema.toLowerCase(), table1.toLowerCase()));
                if (rs.next()) {
                    isTableDataExist = rs.getLong("count") > 0;
                }
            }
            //
            // Index the table
            //
            if (!isTableDataExist) {
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(String.format("INSERT INTO " + FTL_INDEXES_TABLE + " VALUES('%s', '%s', '%s')",
                        upperSchema.toLowerCase(), table1.toLowerCase(), fullTextSearchColumns.toLowerCase()));
                    reindex(con, table1, schemaName);
                    log.info("Lucene search index created for table " + table1);
                } catch (SQLException exc) {
                    log.error(UNABLE_TO_CREATE_LUCENE_SEARCH_INDEX_FOR_TABLE_MSG + table1);
                    throw new SQLException(UNABLE_TO_CREATE_LUCENE_SEARCH_INDEX_FOR_TABLE_MSG + table1, exc);
                }
            }
        }
    }
}
