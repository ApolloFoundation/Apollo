/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.Db;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
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
import org.apache.lucene.index.IndexFormatTooOldException;
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
import org.h2.api.Trigger;
import org.h2.tools.SimpleResultSet;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.enterprise.inject.spi.CDI;

/**
 * FullText provides Lucene search support.  Each searchable database has
 * a database trigger defined.  The Lucene index is updated whenever a row is
 * inserted, updated or deleted.  The DB_ID column is used to identify each row
 * and will be returned as the COLUMNS and KEYS values in the search results.
 *
 * Schema, table and column names are converted to uppercase to match the
 * way H2 stores the information.  Function aliases and triggers are created in
 * the default schema (PUBLIC).
 *
 * The database aliases are defined as follows:
 *   CREATE ALIAS FTL_CREATE_INDEX FOR "com.apollocurrency.aplwallet.apl.db.FullText.createIndex"
 *       CALL FTL_CREATE(schema, table, columnList)
 *   CREATE ALIAS FTL_DROP_INDEX FOR "com.apollocurrency.aplwallet.apl.db.FullText.dropIndex"
 *       CALL FTL_DROP(schema, table)
 *   CREATE ALIAS FTL_SEARCH FOR "com.apollocurrency.aplwallet.apl.db.FullText.search"
 *       CALL FTL_SEARCH(schema, table, query, limit, offset)
 *
 * FTL_CREATE_INDEX is called to create a fulltext index for a table.  It is
 * provided as a convenience for use in AplDbVersion when creating a new index
 * after the database has been created.
 *
 * FTL_DROP_INDEX is called to drop a fulltext index for a table.  It is
 * provided as a convenience for use in AplDbVersion when dropping a table
 * after the database has been created.
 *
 * FTL_SEARCH is used to return the search result set as part of a SELECT statement.
 * The result set columns are the following:
 *   SCHEMA  - the schema name (String)
 *   TABLE   - the table name (String)
 *   COLUMNS - the primary key columns (String[]) - this is always DB_ID for APL
 *   KEYS    - the primary key values (Long[]) - DB_ID value for the row
 *   SCORE   - the search hit score (Float)
 *
 * The table index trigger is defined as follows:
 *   CREATE TRIGGER trigger_name AFTER INSERT,UPDATE,DELETE ON table_name FOR EACH ROW CALL "com.apollocurrency.aplwallet.apl.db.FullText"
 */
public class FullTextTrigger implements Trigger, TransactionalDb.TransactionCallback {
    private static final Logger LOG = getLogger(FullTextTrigger.class);




    /** Index trigger is enabled */
    private volatile boolean isEnabled = false;

    /** Table name (schema.table) */
    private String tableName;

    /** Column names */
    private final List<String> columnNames = new ArrayList<>();

    /** Column types */
    private final List<String> columnTypes = new ArrayList<>();

    /** Database identifier column ordinal */
    private int dbColumn = -1;

    /** Indexed column ordinals */
    private final List<Integer>indexColumns = new ArrayList<>();

    /** Pending table updates */
    private final List<TableUpdate> tableUpdates = new ArrayList<>();


    /**
     * Initialize the trigger (Trigger interface)
     *
     * @param   conn                Database connection
     * @param   schema              Database schema name
     * @param   trigger             Database trigger name
     * @param   table               Database table name
     * @param   before              TRUE if trigger is called before database operation
     * @param   type                Trigger type
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void init(Connection conn, String schema, String trigger, String table, boolean before, int type)
                                    throws SQLException {
        //
        // Ignore the trigger if APL is not active or this is a temporary table copy
        //
        if (table.contains("_COPY_")) {
            return;
        }
        //
        // Access the Lucene index
        //
        // We need to get the access just once, either in a trigger or in a function alias
        //
        getIndexAccess(conn);
        //
        // Get table and index information
        //
        tableName = schema + "." + table;
        try (Statement stmt = conn.createStatement()) {
            //
            // Get the table column information
            //
            // APL tables use DB_ID as the primary index
            //
            try (ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + table + " FROM " + schema)) {
                int index = 0;
                while (rs.next()) {
                    String columnName = rs.getString("FIELD");
                    String columnType = rs.getString("TYPE");
                    columnType = columnType.substring(0, columnType.indexOf('('));
                    columnNames.add(columnName);
                    columnTypes.add(columnType);
                    if (columnName.equals("DB_ID")) {
                        dbColumn = index;
                    }
                    index++;
                }
            }
            if (dbColumn < 0) {
                LOG.error("DB_ID column not found for table " + tableName);
                return;
            }
            //
            // Get the indexed columns
            //
            // Indexed columns must be strings (VARCHAR)
            //
            try (ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT COLUMNS FROM FTL.INDEXES WHERE SCHEMA = '%s' AND TABLE = '%s'",
                    schema, table))) {
                if (rs.next()) {
                    String[] columns = rs.getString(1).split(",");
                    for (String column : columns) {
                        int pos = columnNames.indexOf(column);
                        if (pos >= 0) {
                            if (columnTypes.get(pos).equals("VARCHAR")) {
                                indexColumns.add(pos);
                            } else {
                                LOG.error("Indexed column " + column + " in table " + tableName + " is not a string");
                            }
                        } else {
                            LOG.error("Indexed column " + column + " not found in table " + tableName);
                        }
                    }
                }
            }
            if (indexColumns.isEmpty()) {
                LOG.error("No indexed columns found for table " + tableName);
                return;
            }
            //
            // Trigger is enabled
            //
            isEnabled = true;
            indexTriggers.put(tableName, this);
        } catch (SQLException exc) {
            LOG.error("Unable to get table information", exc);
        }
    }

    /**
     * Close the trigger (Trigger interface)
     *
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void close() throws SQLException {
        if (isEnabled) {
            isEnabled = false;
            indexTriggers.remove(tableName);
        }
    }

    /**
     * Remove the trigger (Trigger interface)
     *
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void remove() throws SQLException {
        if (isEnabled) {
            isEnabled = false;
            indexTriggers.remove(tableName);
        }
    }

    /**
     * Trigger has fired (Trigger interface)
     *
     * @param   conn                Database connection
     * @param   oldRow              The old row or null
     * @param   newRow              The new row or null
     * @throws  SQLException        A SQL error occurred
     */
    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        //
        // Ignore the trigger if it is not enabled
        //
        if (!isEnabled) {
            return;
        }
        //
        // Commit the change immediately if we are not in a transaction
        //
        if (!Db.getDb().isInTransaction()) {
            try {
                commitRow(oldRow, newRow);
                commitIndex();
            } catch (SQLException exc) {
                LOG.error("Unable to update the Lucene index", exc);
            }
            return;
        }
        //
        // Save the table update until the update is committed or rolled back.  Note
        // that the current thread is the application thread performing the update operation.
        //
        synchronized(tableUpdates) {
            tableUpdates.add(new TableUpdate(Thread.currentThread(), oldRow, newRow));
        }
        //
        // Register our transaction callback
        //
        Db.getDb().registerCallback(this);
    }

    /**
     * Commit the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void commit() {
        Thread thread = Thread.currentThread();
        try {
            //
            // Update the Lucene index.  Note that a database transaction is associated
            // with a single thread.  So we will commit just those updates generated
            // by the current thread.
            //
            boolean commit = false;
            synchronized(tableUpdates) {
                Iterator<TableUpdate> updateIt = tableUpdates.iterator();
                while (updateIt.hasNext()) {
                    TableUpdate update = updateIt.next();
                    if (update.getThread() == thread) {
                        commitRow(update.getOldRow(), update.getNewRow());
                        updateIt.remove();
                        commit = true;
                    }
                }
            }
            //
            // Commit the index updates
            //
            if (commit) {
                commitIndex();
            }
        } catch (SQLException exc) {
            LOG.error("Unable to update the Lucene index", exc);
        }
    }

    /**
     * Discard the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void rollback() {
        Thread thread = Thread.currentThread();
        synchronized(tableUpdates) {
            tableUpdates.removeIf(update -> update.getThread() == thread);
        }
    }
}
