/*
 *  Copyright © 2018-2019 Apollo Foundation
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

@Slf4j
@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class FullTextSearchServiceImpl implements FullTextSearchService {

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
     * Drop the fulltext index for a table
     *
     * @param conn   SQL connection
     * @param schema Schema name
     * @param table  Table name
     * @throws SQLException Unable to drop fulltext index
     */
    public void dropIndex(Connection conn, String schema, String table) throws SQLException {
        String upperSchema = schema.toUpperCase();
        String upperTable = table.toUpperCase();
        boolean reindex = false;
        //
        // Drop an existing database trigger
        //
        try (Statement qstmt = conn.createStatement();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = qstmt.executeQuery(String.format(
                "SELECT COLUMNS FROM FTL.INDEXES WHERE SCHEMA = '%s' AND \"TABLE\" = '%s'",
                upperSchema, upperTable))) {
                if (rs.next()) {
                    stmt.execute("DROP TRIGGER IF EXISTS FTL_" + upperTable);
                    stmt.execute(String.format("DELETE FROM FTL.INDEXES WHERE SCHEMA = '%s' AND \"TABLE\" = '%s'",
                        upperSchema, upperTable));
                    reindex = true;
                }
            }
        }
        //
        // Rebuild the Lucene index
        //
        if (reindex) {
            reindexAll(conn, indexTables, schema);
        }
    }

    /**
     * Initialize the fulltext support for a new database
     * <p>
     * This method should be called from AplDbVersion when performing the database version update
     * that enables fulltext search support
     */
    public void init() {
        try {
            ftl.init();
        } catch (IOException e) {
            throw new RuntimeException("Unable to init fulltext engine", e);
        }
        String triggerClassName = FullTextTrigger.class.getName();
        try (Connection conn = databaseManager.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             Statement qstmt = conn.createStatement()) {
            //
            // Check if we have already been initialized.
            //
            boolean alreadyInitialized = true;
            boolean triggersExist = false;
            try (ResultSet rs = qstmt.executeQuery("SELECT JAVA_CLASS FROM INFORMATION_SCHEMA.TRIGGERS "
                + "WHERE SUBSTRING(TRIGGER_NAME, 0, 4) = 'FTL_'")) {
                while (rs.next()) {
                    triggersExist = true;
                    if (!rs.getString(1).startsWith(triggerClassName)) {
                        alreadyInitialized = false;
                    }
                }
            }
            if (triggersExist && alreadyInitialized) {
                log.info("Fulltext support is already initialized");
                return;
            }
            //
            // We need to delete an existing Lucene index since the V3 file format is not compatible with V5
            //
            ftl.clearIndex();
            //
            // Drop the H2 Lucene V3 function aliases
            // Mainly for backward compatibility with old databases, which
            // full text search was implemented by using built-in h2 trigger
            // org.h2.fulltext.FullTextLucene.init
            //
            stmt.execute("DROP ALIAS IF EXISTS FTL_INIT");
            stmt.execute("DROP ALIAS IF EXISTS FTL_DROP_ALL");
            stmt.execute("DROP ALIAS IF EXISTS FTL_REINDEX");
            stmt.execute("DROP ALIAS IF EXISTS FTL_SEARCH_DATA");

            // Drop our fulltext function aliases, we should not depend on stored procedures
            // since it hard wire us with h2
            //
            stmt.execute("DROP ALIAS IF EXISTS FTL_SEARCH");
            stmt.execute("DROP ALIAS IF EXISTS FTL_CREATE_INDEX");
            stmt.execute("DROP ALIAS IF EXISTS FTL_DROP_INDEX");

            log.info("H2 fulltext function aliases dropped");
            //
            // Create our schema and table
            //
            stmt.execute("CREATE SCHEMA IF NOT EXISTS FTL");
            stmt.execute("CREATE TABLE IF NOT EXISTS FTL.INDEXES "
                + "(SCHEMA VARCHAR, \"TABLE\" VARCHAR, COLUMNS VARCHAR, PRIMARY KEY(SCHEMA, \"TABLE\"))");
            log.info(" fulltext schema created");
            //
            // Drop existing triggers and create our triggers.  H2 will initialize the trigger
            // when it is created.  H2 has already initialized the existing triggers and they
            // will be closed when dropped.  The H2 Lucene V3 trigger initialization will work with
            // Lucene V5, so we are able to open the database using the Lucene V5 library files.
            //
            try (ResultSet rs = qstmt.executeQuery("SELECT * FROM FTL.INDEXES")) {
                while (rs.next()) {
                    String schema = rs.getString("SCHEMA");
                    String table = rs.getString("TABLE");
                    stmt.execute("DROP TRIGGER IF EXISTS FTL_" + table);
                    stmt.execute(String.format("CREATE TRIGGER FTL_%s AFTER INSERT,UPDATE,DELETE ON %s.%s "
                            + "FOR EACH ROW CALL \"%s\"",
                        table, schema, table, triggerClassName));
                }
            }
            //
            // Rebuild the Lucene index since the Lucene V3 index is not compatible with Lucene V5
            //
            reindexAll(conn);
//            //
//            // Create our function aliases
//            //
            stmt.execute("CREATE ALIAS FTL_SEARCH NOBUFFER FOR \"" + FullTextStoredProcedures.class.getName() + ".search\"");
            log.info("Fulltext aliases created");
        } catch (SQLException exc) {
            log.error("Unable to initialize fulltext search support", exc);
            throw new RuntimeException(exc.toString(), exc);
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
        // Drop existing triggers
        //
        try (Statement qstmt = conn.createStatement();
             Statement stmt = conn.createStatement();
             ResultSet rs = qstmt.executeQuery("SELECT \"TABLE\" FROM FTL.INDEXES")) {
            while (rs.next()) {
                String table = rs.getString(1);
                stmt.execute("DROP TRIGGER IF EXISTS FTL_" + table);
            }
            stmt.execute("TRUNCATE TABLE FTL.INDEXES");
        }
        //
        // Delete the Lucene index
        //
        ftl.clearIndex();
    }

    public ResultSet search(String schema, String table, String queryText, int limit, int offset)
        throws SQLException {
        return ftl.search(schema, table, queryText, limit, offset);
    }

    @Override
    public void shutdown() {
        try {
            ftl.shutdown();
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
        sb.append("SELECT DB_ID");
        for (int index : tableData.getIndexColumns()) {
            sb.append(", ").append(tableData.getColumnNames().get(index));
        }
        sb.append(" FROM ").append(tableName);
        Object[] row = new Object[tableData.getColumnNames().size()];
        //
        // Index each row in the table
        //
        try (Statement qstmt = conn.createStatement();
             ResultSet rs = qstmt.executeQuery(sb.toString())) {
            while (rs.next()) {
                row[tableData.getDbIdColumnPosition()] = rs.getObject(1);
                int i = 2;
                for (int index : tableData.getIndexColumns()) {
                    row[index] = rs.getObject(i++);
                }
                ftl.indexRow(row, tableData);
            }
        }
        //
        // Commit the index updates
        //
        ftl.commitIndex();
    }

    public void reindexAll(Connection conn) throws SQLException {
        reindexAll(conn, indexTables, schemaName);
    }

    private void reindexAll(Connection conn, Set<String> tables, String schema) throws SQLException {
        long start = System.currentTimeMillis();
        log.info("Rebuilding Lucene search index");
        try {
            //
            // Delete the current Lucene index
            //
            ftl.clearIndex();
            //
            // Reindex each table
            //
            for (String tableName : tables) {
                long startTable = System.currentTimeMillis();
                log.debug("Reindexing {}", tableName);
                reindex(conn, tableName, schema);
                log.debug("Reindexing {} DONE in '{}' ms", tableName, System.currentTimeMillis() - startTable);
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
            String table1 = table.toUpperCase();
            String upperSchema = schemaName.toUpperCase();
            String upperTable = table1.toUpperCase();
            String tableName = upperSchema + "." + upperTable;
            //
            // Drop an existing index and the associated database trigger
            //
            dropIndex(con, schemaName, table1);
            //
            // Update our schema and create a new database trigger.  Note that the trigger
            // will be initialized when it is created.
            //
            try (Statement stmt = con.createStatement()) {
                stmt.execute(String.format("INSERT INTO FTL.INDEXES (schema, \"TABLE\", columns) "
                        + "VALUES('%s', '%s', '%s')",
                    upperSchema, upperTable, fullTextSearchColumns.toUpperCase().toUpperCase()));
                stmt.execute(String.format("CREATE TRIGGER FTL_%s AFTER INSERT,UPDATE,DELETE ON %s "
                        + "FOR EACH ROW CALL \"%s\"",
                    upperTable, tableName, FullTextTrigger.class.getName()));
            }
            //
            // Index the table
            //
            try {
                reindex(con, upperTable, schemaName);
                log.info("Lucene search index created for table " + tableName);
            } catch (SQLException exc) {
                log.error("Unable to create Lucene search index for table " + tableName);
                throw new SQLException("Unable to create Lucene search index for table " + tableName, exc);
            }
        }
    }
}
