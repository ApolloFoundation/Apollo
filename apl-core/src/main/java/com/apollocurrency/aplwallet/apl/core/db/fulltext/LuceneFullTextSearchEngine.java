/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.ReadWriteUpdateLock;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.h2.tools.SimpleResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

public class LuceneFullTextSearchEngine implements FullTextSearchEngine {
    private static final Logger LOG = LoggerFactory.getLogger(LuceneFullTextSearchEngine.class);

    /** Index lock */
    private final ReadWriteUpdateLock indexLock = new ReadWriteUpdateLock();

    /** Lucene analyzer (thread-safe) */
    private final Analyzer analyzer = new StandardAnalyzer();

    private NtpTime ntpTime;
    private Path indexDirPath;

    /** Lucene index reader (thread-safe) */
    private static DirectoryReader indexReader;

    /** Lucene index searcher (thread-safe) */
    private static IndexSearcher indexSearcher;

    /** Lucene index writer (thread-safe) */
    private static IndexWriter indexWriter;


    @Inject
    public LuceneFullTextSearchEngine(NtpTime ntpTime, @Named("indexDirPath") Path indexPath) throws IOException {
        this.ntpTime = ntpTime;
        this.indexDirPath = indexPath;
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexRow(Object[] row, TableData tableData) throws SQLException {
            indexLock.readLock().lock();
            try {
                List<String> columnNames = tableData.getColumnNames();
                List<Integer> indexColumns = tableData.getIndexColumns();
                int dbColumn = tableData.getDbIdColumnPosition();
                String tableName = tableData.getSchema().toUpperCase() + "." + tableData.getTable().toUpperCase();
                String query = tableName + ";" + columnNames.get(dbColumn) + ";" + (long) row[dbColumn];
                Document document = new Document();
                document.add(new StringField("_QUERY", query, Field.Store.YES));
                long now = ntpTime.getTime();
                document.add(new TextField("_MODIFIED", DateTools.timeToString(now, DateTools.Resolution.SECOND), Field.Store.NO));
                document.add(new TextField("_TABLE", tableName, Field.Store.NO));
                StringJoiner sj = new StringJoiner(" ");
                for (int index : indexColumns) {
                    String data = (row[index] != null ? (String)row[index] : "NULL");
                    document.add(new TextField(columnNames.get(index), data, Field.Store.NO));
                    sj.add(data);
                }
                document.add(new TextField("_DATA", sj.toString(), Field.Store.NO));
                indexWriter.updateDocument(new Term("_QUERY", query), document);
            } catch (IOException exc) {
                LOG.error("Unable to index row", exc);
                throw new SQLException("Unable to index row", exc);
            } finally {
                indexLock.readLock().unlock();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitRow(Object[] oldRow, Object[] newRow, TableData tableData) throws SQLException {
        if (oldRow != null) {
            if (newRow != null) {
                indexRow(newRow, tableData);
            } else {
                deleteRow(oldRow, tableData);
            }
        } else if (newRow != null) {
            indexRow(newRow, tableData);
        }
    }
    private void deleteRow(Object[] row, TableData tableData) throws SQLException {
        String query =
                tableData.getTable() + ";" + tableData.getColumnNames().get(tableData.getDbIdColumnPosition()) + ";" + (long) row[tableData.getDbIdColumnPosition()];
        indexLock.readLock().lock();
        try {
            indexWriter.deleteDocuments(new Term("_QUERY", query));
        } catch (IOException exc) {
            LOG.error("Unable to delete indexed row", exc);
            throw new SQLException("Unable to delete indexed row", exc);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws IOException {
        boolean obtainedUpdateLock = false;
        if (!indexLock.writeLock().hasLock()) {
            indexLock.updateLock().lock();
            obtainedUpdateLock = true;
        }
        try {

                indexLock.writeLock().lock();
                try {
                        IndexWriterConfig config = new IndexWriterConfig(analyzer);
                        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                        Directory indexDir = FSDirectory.open(indexDirPath);
                        indexWriter = new IndexWriter(indexDir, config);
                        Document document = new Document();
                        document.add(new StringField("_QUERY", "_CONTROL_DOCUMENT_", Field.Store.YES));
                        indexWriter.updateDocument(new Term("_QUERY", "_CONTROL_DOCUMENT_"), document);
                        indexWriter.commit();
                        indexReader = DirectoryReader.open(indexDir);
                        indexSearcher = new IndexSearcher(indexReader);
                } finally {
                    indexLock.writeLock().unlock();
                }

        } catch (IOException exc) {
            LOG.error("Unable to access the Lucene index", exc);
            throw new IOException("Unable to access the Lucene index", exc);
        }  finally {
            if (obtainedUpdateLock) {
                indexLock.updateLock().unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitIndex() throws SQLException {
        indexLock.writeLock().lock();
        try {
            indexWriter.commit();
            DirectoryReader newReader = DirectoryReader.openIfChanged(indexReader);
            if (newReader != null) {
                indexReader.close();
                indexReader = newReader;
                indexSearcher = new IndexSearcher(indexReader);
            }
        } catch (IOException exc) {
            LOG.error("Unable to commit Lucene index updates", exc);
            throw new SQLException("Unable to commit Lucene index updates", exc);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Remove the Lucene index files
     *
     * @throws  SQLException        I/O error occurred
     */
    @Override
    public void clearIndex() throws SQLException {
        indexLock.writeLock().lock();
        try {
            try {
                //
                // Delete the index files
                //
                shutdown();
                try (Stream<Path> stream = Files.list(indexDirPath)) {
                    Path[] paths = stream.toArray(Path[]::new);
                    for (Path path : paths) {
                        Files.delete(path);
                    }
                }
                init();
                LOG.info("Lucene search index deleted");
            }
            catch (IOException exc) {
                LOG.error("Unable to remove Lucene index files", exc);
                throw new SQLException("Unable to remove Lucene index files", exc);
            }
        }
        finally {
            indexLock.writeLock().unlock();
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet search(String schema, String table, String queryText, int limit, int offset)
            throws SQLException {
        //
        // Create the result set columns
        //
        SimpleResultSet result = new SimpleResultSet();
        result.addColumn("SCHEMA", Types.VARCHAR, 0, 0);
        result.addColumn("TABLE", Types.VARCHAR, 0, 0);
        result.addColumn("COLUMNS", Types.ARRAY, 0, 0);
        result.addColumn("KEYS", Types.ARRAY, 0, 0);
        result.addColumn("SCORE", Types.FLOAT, 0, 0);
        //
        // Perform the search
        //
        // The _QUERY field contains the table and row identification (schema.table;keyName;keyValue)
        // The _TABLE field is used to limit the search results to the current table
        // The _DATA field contains the indexed row data (this is the default search field)
        // The _MODIFIED field contains the row modification time (YYYYMMDDhhmmss) in GMT
        //
        indexLock.readLock().lock();
        try {
            QueryParser parser = new QueryParser("_DATA", analyzer);
            parser.setDateResolution("_MODIFIED", DateTools.Resolution.SECOND);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            Query query = parser.parse("_TABLE:" + schema.toUpperCase() + "." + table.toUpperCase() + " AND (" + queryText + ")");
            TopDocs documents = indexSearcher.search(query, limit);
            ScoreDoc[] hits = documents.scoreDocs;
            int resultCount = Math.min(hits.length, (limit == 0 ? hits.length : limit));
            int resultOffset = Math.min(offset, resultCount);
            for (int i=resultOffset; i<resultCount; i++) {
                Document document = indexSearcher.doc(hits[i].doc);
                String[] indexParts = document.get("_QUERY").split(";");
                String[] nameParts = indexParts[0].split("\\.");
                result.addRow(nameParts[0],
                        nameParts[1],
                        new String[] {indexParts[1]},
                        new Long[] {Long.parseLong(indexParts[2])},
                        hits[i].score);
            }
        } catch (ParseException exc) {
            LOG.debug("Lucene parse exception for query: " + queryText + "\n" + exc.getMessage());
            throw new SQLException("Lucene parse exception for query: " + queryText + "\n" + exc.getMessage());
        } catch (IOException exc) {
            LOG.error("Unable to search Lucene index", exc);
            throw new SQLException("Unable to search Lucene index", exc);
        } finally {
            indexLock.readLock().unlock();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        indexLock.writeLock().lock();
        try {
            commitIndex();
            if (indexReader != null) {
                indexReader.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
        } catch (IOException | SQLException exc) {
            LOG.error("Unable to remove Lucene index access", exc);
        } finally {
            indexLock.writeLock().unlock();
        }
    }
}
