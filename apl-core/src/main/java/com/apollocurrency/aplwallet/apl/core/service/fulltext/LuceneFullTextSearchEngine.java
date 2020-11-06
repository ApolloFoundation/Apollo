/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.ReadWriteUpdateLock;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;
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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

@Slf4j
@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class LuceneFullTextSearchEngine implements FullTextSearchEngine {
    /**
     * Lucene index reader (thread-safe)
     */
    private static DirectoryReader indexReader;
    /**
     * Lucene index searcher (thread-safe)
     */
    private static IndexSearcher indexSearcher;
    /**
     * Lucene index writer (thread-safe)
     */
    private static IndexWriter indexWriter;
    /**
     * Index lock
     */
    private final ReadWriteUpdateLock indexLock = new ReadWriteUpdateLock();
    /**
     * Lucene analyzer (thread-safe)
     */
    private final Analyzer analyzer = new StandardAnalyzer();
    private NtpTime ntpTime;
    private Path indexDirPath;
    private final Blockchain blockchain;


    @Inject
    public LuceneFullTextSearchEngine(NtpTime ntpTime, @Named("indexDirPath") Path indexPath, final Blockchain blockchain) {
        this.ntpTime = ntpTime;
        this.indexDirPath = indexPath;
        this.blockchain = blockchain;
        if (!Files.exists(indexPath)) {
            try {
                Files.createDirectories(indexPath);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create index directory", e);
            }
        }
    }

    @Override
    public boolean isIndexFolderEmpty() throws IOException {
        boolean indexFolderExists = Files.exists(this.indexDirPath);
        if (indexFolderExists) {
            try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(this.indexDirPath)) {
                return !dirStream.iterator().hasNext();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void indexRow(FullTextOperationData row, TableData tableData) throws SQLException {
        Objects.requireNonNull(row, "row is NULL");
        Objects.requireNonNull(tableData, "tableData is NULL");
        indexLock.readLock().lock();
        try {
            List<String> columnNames = tableData.getColumnNames();
            List<Integer> indexColumns = tableData.getIndexColumns();
            String schemaTableName = tableData.getSchema().toLowerCase() + "." + tableData.getTable().toLowerCase();
            String query = row.getTableKey();
            Document document = new Document();
            document.add(new StringField("_QUERY", query, Field.Store.YES));
            long now = ntpTime.getTime();
            document.add(new TextField("_MODIFIED", DateTools.timeToString(now, DateTools.Resolution.SECOND), Field.Store.NO));
            document.add(new TextField("_TABLE", schemaTableName, Field.Store.NO));
            StringJoiner sj = new StringJoiner(" ");
            for (int i = 0; i < row.getColumnsWithData().size(); i++) {
                String data = row.getColumnsWithData().get(i) != null ?
                    (String) row.getColumnsWithData().get(i) : "NULL";
                String columnName = columnNames.get(indexColumns.get(i)); // get indexed column name by i
                document.add(new TextField(columnName, data, Field.Store.NO));
                sj.add(data);
            }
            document.add(new TextField("_DATA", sj.toString(), Field.Store.NO));
            log.debug("INDEX query={}\n{}", query, document);
            if (document.getFields().size() > 4) {
                // put/update Index when there are real data
                indexWriter.updateDocument(new Term("_QUERY", query), document);
                log.debug("INSERT/UPDATE = {}\n{}", query, document);
            } else {
                log.warn("SKIPPED FTS indexing for incomplete data set = {}", row);
            }
        } catch (IOException exc) {
            log.error("Unable to index row", exc);
            throw new SQLException("Unable to index row", exc);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitRow(FullTextOperationData newRow, TableData tableData) throws SQLException {
        Objects.requireNonNull(newRow, "newRow data is NULL");
        log.debug("COMMIT row at {}: \n{}\n{}\n{}", (this.blockchain != null ? this.blockchain.getHeight() : -1), newRow, tableData);
        if (newRow.getOperationType() == FullTextOperationData.OperationType.INSERT_UPDATE) {
//            log.debug("INSERT/UPDATE: tableData = {}, newRow={}", tableData, newRow);
            indexRow(newRow, tableData);
        } else {
//            log.debug("DELETE: tableData = {}, newRow={}", tableData, newRow);
            deleteRow(newRow, tableData);
        }
    }

    private void deleteRow(FullTextOperationData row, TableData tableData) throws SQLException {
        String query = row.getTableKey();
        indexLock.readLock().lock();
        try {
//            log.debug("DELETE QUERY: query={}\n{}, oldRow={}\n", query, tableData, row);
            indexWriter.deleteDocuments(new Term("_QUERY", query));
            log.debug("DELETE: {}", query);
        } catch (IOException exc) {
            log.error("Unable to delete indexed row", exc);
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
        log.trace("LuceneFullTextSearchEngine init...");
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
            log.debug("LuceneFullTextSearchEngine init DONE");
        } catch (IOException exc) {
            log.error("Unable to access the Lucene index", exc);
            throw new IOException("Unable to access the Lucene index", exc);
        } finally {
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
            if (indexWriter != null && indexWriter.isOpen()) {
                indexWriter.commit();
            }
            DirectoryReader newReader = DirectoryReader.openIfChanged(indexReader);
            if (newReader != null) {
                indexReader.close();
                indexReader = newReader;
                indexSearcher = new IndexSearcher(indexReader);
            }
        } catch (IOException exc) {
            log.error("Unable to commit Lucene index updates", exc);
            throw new SQLException("Unable to commit Lucene index updates", exc);
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    /**
     * Remove the Lucene index files
     *
     * @throws SQLException I/O error occurred
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
                log.info("Lucene search index deleted");
            } catch (IOException exc) {
                log.error("Unable to remove Lucene index files", exc);
                throw new SQLException("Unable to remove Lucene index files", exc);
            }
        } finally {
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
        result.addColumn("schema", Types.VARCHAR, 0, 0);
        result.addColumn("table", Types.VARCHAR, 0, 0);
        result.addColumn("columns", Types.VARCHAR, 0, 0);
        result.addColumn("score", Types.FLOAT, 0, 0);
        result.addColumn("keys", Types.BIGINT, 0, 0);
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
            String queryStr = "_TABLE:" + schema.toUpperCase() + "." + table.toUpperCase() + " AND (" + queryText + ")";
            Query query = parser.parse(queryStr);
            TopDocs documents = indexSearcher.search(query, limit);
            ScoreDoc[] hits = documents.scoreDocs;
            int resultCount = Math.min(hits.length, (limit == 0 ? hits.length : limit));
            int resultOffset = Math.min(offset, resultCount);
            log.debug("HITS length = [{}], resultCount={}, resultOffset={}, query={}", hits.length, resultCount, resultOffset, queryStr);
            for (int i = resultOffset; i < resultCount; i++) {
                Document document = indexSearcher.doc(hits[i].doc); // _QUERY EXAMPLE = public.account_info;DB_ID;1
                String[] indexParts = document.get("_QUERY").split(";"); // split to: public.account_info + DB_ID + 1
                String[] nameParts = indexParts[0].split("\\.");// split to : public + account_info
                log.debug("create ROW indexParts[{}]={}, nameParts[{}]={}", indexParts.length, nameParts.length, indexParts, nameParts);
                result.addRow(nameParts[0], // schema name = public
                    nameParts[1], // table name = account_info
                    new String[]{indexParts[1]}, // columns ?
                    hits[i].score, // score
                    Long.parseLong(indexParts[2]) // DB_ID key value = 1
                );
            }
            log.debug("RESULT by query = {} | {} / {}", queryStr, documents, hits);
        } catch (ParseException exc) {
            log.debug("Lucene parse exception for query: " + queryText + "\n" + exc.getMessage());
            throw new SQLException("Lucene parse exception for query: " + queryText + "\n" + exc.getMessage());
        } catch (IOException exc) {
            log.error("Unable to search Lucene index", exc);
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
        log.trace("LuceneFullTextSearchEngine shutdown start...");
        indexLock.writeLock().lock();
        try {
            commitIndex();
            if (indexReader != null) {
                indexReader.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
            log.debug("LuceneFullTextSearchEngine shutdown DONE...");
        } catch (IOException | SQLException exc) {
            log.error("Unable to remove Lucene index access", exc);
        } finally {
            indexLock.writeLock().unlock();
        }
    }
}
