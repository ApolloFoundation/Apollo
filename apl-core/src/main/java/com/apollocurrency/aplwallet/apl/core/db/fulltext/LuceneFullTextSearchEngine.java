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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;
import javax.inject.Inject;

public class LuceneFullTextSearchEngine {
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
    public LuceneFullTextSearchEngine(NtpTime ntpTime, Path indexPath) throws IOException {
        this.ntpTime = ntpTime;
        this.indexDirPath = indexPath;
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }
    }

    public void indexRow(Object[] row, TableData tableData) throws SQLException {
            indexLock.readLock().lock();
            try {
                List<String> columnNames = tableData.getColumnNames();
                List<Integer> indexColumns = tableData.getIndexColumns();
                int dbColumn = tableData.getDbIdColumnPosition();
                String tableName = tableData.getTable();
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
     * Update the Lucene index for a committed row
     *
     * @param   oldRow              Old row column data
     * @param   newRow              New row column data
     * @throws  SQLException        Unable to commit row
     */
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

    public void init() throws IOException {
        boolean obtainedUpdateLock = false;
        if (!indexLock.writeLock().hasLock()) {
            indexLock.updateLock().lock();
            obtainedUpdateLock = true;
        }
        try {
            if (indexWriter == null) {
                indexLock.writeLock().lock();
                try {
                    if (indexWriter == null) {
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
                    }
                } finally {
                    indexLock.writeLock().unlock();
                }
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
     * Commit the index updates
     *
     * @throws  SQLException        Unable to commit index updates
     */
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
     * Remove Lucene index access
     */
    public void shutdown() {
        indexLock.writeLock().lock();
        try {
//            should not be null when initialization was successful
            if (indexReader != null) {
                indexReader.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
        } catch (IOException exc) {
            LOG.error("Unable to remove Lucene index access", exc);
        } finally {
            indexLock.writeLock().unlock();
        }
    }























//    /**
//     * Get the Lucene index path.
//     * Note: it's a current implementation
//     *
//     * @param   conn                SQL connection
//     * @throws  SQLException        Unable to get the Lucene index path
//     */
//    private Path getIndexPath(Connection conn) throws SQLException {
//        indexLock.writeLock().lock();
//        Path indexPath;
//        try {
//                try (Statement stmt = conn.createStatement();
//                     ResultSet rs = stmt.executeQuery("CALL DATABASE_PATH()")) {
//                    rs.next();
//                    indexPath = Paths.get(rs.getString(1));
//                    if (!Files.exists(indexPath)) {
//                        Files.createDirectory(indexPath);
//                    }
//                } catch (IOException exc) {
//                    LOG.error("Unable to create the Lucene index directory", exc);
//                    throw new SQLException("Unable to create the Lucene index directory", exc);
//            }
//        } finally {
//            indexLock.writeLock().unlock();
//        }
//        return indexPath;
//    }
}
