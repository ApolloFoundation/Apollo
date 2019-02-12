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
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

public class IndexService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);
    private LuceneFullTextSearchEngine ftl;
    private Path indexDirPath;

    @Inject
    public IndexService(LuceneFullTextSearchEngine ftl, @Named("indexDirPath") Path indexDir) {
        this.ftl = ftl;
        this.indexDirPath = indexDir;
    }

    public void init() {
        try {
            ftl.init();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot init lucene fulltext search");
        }
    }


    public void reindexTable(Connection conn, String tableName, String schemaName) throws SQLException {
        //
        // Build the SELECT statement for just the indexed columns
        //
        TableData tableData = DbUtils.getTableData(conn, tableName, schemaName);
        if (tableData.getDbIdColumnPosition() == -1) {
            LOG.debug("Table {} has not dbId column", tableName);
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
    /**
     * Reindex all of the indexed tables
     *
     * @param   conn                SQL connection
     * @throws  SQLException        Unable to reindex tables
     */
    public void reindexAll(Connection conn, List<String> tables, String schema) throws SQLException {
        LOG.info("Rebuilding the Lucene search index");
        try {
            //
            // Delete the current Lucene index
            //
            clearIndex();
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
    void clearIndex() throws SQLException {
        try {
            //
            // Delete the index files
            //
            ftl.shutdown();
            try (Stream<Path> stream = Files.list(indexDirPath)) {
                Path[] paths = stream.toArray(Path[]::new);
                for (Path path : paths) {
                    Files.delete(path);
                }
            }
            ftl.init();
            LOG.info("Lucene search index deleted");
        } catch (IOException exc) {
            LOG.error("Unable to remove Lucene index files", exc);
            throw new SQLException("Unable to remove Lucene index files", exc);
        }
    }
    public ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException {
        return ftl.search(schema, table, queryText, limit, offset);
    }

    public void shutdown() {
        ftl.shutdown();
    }
}
