/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;

public class IndexService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);
    private LuceneFullTextSearchEngine ftl;
    private Path indexDirPath;

    @Inject
    public IndexService(LuceneFullTextSearchEngine ftl, Path indexDir) {
        this.ftl = ftl;
        this.indexDirPath = indexDir;
    }

    /**
     * Reindex the table
     *
     * @param conn SQL connection
     * @throws SQLException Unable to reindex table
     */
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
        String upperSchema = schema.toUpperCase();
        String upperTable = table.toUpperCase();
        String tableName = upperSchema + "." + upperTable;
        getIndexAccess(conn);
        //
        // Drop an existing index and the associated database trigger
        //
        dropIndex(conn, schema, table);
        //
        // Update our schema and create a new database trigger.  Note that the trigger
        // will be initialized when it is created.
        //
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("INSERT INTO FTL.INDEXES (schema, table, columns) "
                            + "VALUES('%s', '%s', '%s')",
                    upperSchema, upperTable, columnList.toUpperCase()));
            stmt.execute(String.format("CREATE TRIGGER FTL_%s AFTER INSERT,UPDATE,DELETE ON %s "
                            + "FOR EACH ROW CALL \"%s\"",
                    upperTable, tableName, FullText.class.getName()));
        }
        //
        // Index the table
        //
        FullText trigger = indexTriggers.get(tableName);
        if (trigger == null) {
            LOG.error("ARS fulltext trigger for table " + tableName + " was not initialized");
        } else {
            try {
                trigger.reindexTable(conn);
                LOG.info("Lucene search index created for table " + tableName);
            } catch (SQLException exc) {
                LOG.error("Unable to create Lucene search index for table " + tableName);
                throw new SQLException("Unable to create Lucene search index for table " + tableName, exc);
            }
        }
    }
    public void reindexTable(Connection conn, String tableName, String schemaName) throws SQLException {
        //
        // Build the SELECT statement for just the indexed columns
        //
        TableData tableData = DbUtils.getTableData(conn, tableName, schemaName);
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
    /**
     * Reindex all of the indexed tables
     *
     * @param   conn                SQL connection
     * @throws  SQLException        Unable to reindex tables
     */
    public void reindexAll(Connection conn, List<String> tables, String schema) throws SQLException, IOException {
        LOG.info("Rebuilding the Lucene search index");
        try {
            //
            // Delete the current Lucene index
            //
            ftl.shutdown();
            removeIndexFiles();
            ftl.init();
            //
            // Reindex each table
            //
            for (String tableName : tables) {
                reindexTable(conn, tableName, schema);
            }
        } catch (SQLException exc) {
            throw new SQLException("Unable to rebuild the Lucene index", exc);
        }
        LOG.info("Lucene search index successfully rebuilt");
    }

    /**
     * Remove the Lucene index files
     *
     * @throws  SQLException        I/O error occurred
     */
    private void removeIndexFiles() throws SQLException {
        try {
            //
            // Delete the index files
            //
            try (Stream<Path> stream = Files.list(indexDirPath)) {
                Path[] paths = stream.toArray(Path[]::new);
                for (Path path : paths) {
                    Files.delete(path);
                }
            }
            LOG.info("Lucene search index deleted");
        } catch (IOException exc) {
            LOG.error("Unable to remove Lucene index files", exc);
            throw new SQLException("Unable to remove Lucene index files", exc);
        }
    }
}
