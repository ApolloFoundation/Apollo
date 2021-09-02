/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.TransactionCallback;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Componend store pending Lucene index updates/changes and commits them when main transaction is committed by using
 * DataSource transaction callback registered for data accepting.
 */
@Slf4j
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
public class FullTextSearchUpdaterImpl implements TransactionCallback, FullTextSearchUpdater {

    /**
     * Pending table updates
     * We collect index row updates and then commit or rollback it when db transaction was finished or rollbacked
     */
    private Map<String, FullTextOperationData> pendingFTSIndexUpdates = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;
    private final FullTextSearchEngine fullTextSearchEngine;
    private final FullTextSearchService fullTextSearchService;
    private final FullTextConfig fullTextConfig;
    /**
     * Here we keep searchable table characteristics
     */
    private Map<String, TableData> tableDataMap = new ConcurrentHashMap<>();

    @Inject
    public FullTextSearchUpdaterImpl(DatabaseManager databaseManager,
                                     FullTextSearchEngine fullTextSearchEngine,
                                     FullTextSearchService fullTextSearchService,
                                     FullTextConfig fullTextConfig) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.fullTextSearchEngine = Objects.requireNonNull(fullTextSearchEngine, "fullTextSearchEngine is NULL");
        this.fullTextSearchService = Objects.requireNonNull(fullTextSearchService, "fullTextSearchService is NULL");
        this.fullTextConfig = Objects.requireNonNull(fullTextConfig, "ft config is NULL");
        log.debug("Constructor FullTextObserver");
    }

    /**
     * Initialize some data.
     */
    @PostConstruct
    public void init() {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        log.debug("init = (isInTransaction = {})", dataSource.isInTransaction());
        dataSource.registerCallback(this);

        fullTextSearchService.init(); // tables and their indexed columns should be in DB after init() call
        try (Connection con = dataSource.getConnection();
             ResultSet rs = con.createStatement().executeQuery("SELECT `table` FROM ftl_indexes")) {
            while (rs.next()) {
                String tableName = rs.getString("table");
                try { // initialize map structure for future use
                    TableData tableData = this.readTableData(con, fullTextConfig.getSchema(), tableName);
                    this.tableDataMap.put(tableName.toLowerCase(), tableData);
                } catch (SQLException throwables) {
                    log.error("Reading table '{}'", tableName);
                }
            }
        } catch (SQLException e) {
            log.error("Cannot initialize index table", e);
        }
        log.debug("Fetched info for = [{}] = {}", this.tableDataMap.size(), this.tableDataMap);
    }

    private TableData readTableData(Connection connection, String schema, String tableName) throws SQLException {
        return DbUtils.getTableData(connection, tableName, schema);
    }

    public void onPutFullTextOperationData(@ObservesAsync @TrimEvent FullTextOperationData operationData) {
        log.debug("Received ASYNC operationData = {}", operationData);
        this.putFullTextOperationData(operationData);
    }

    /**
     * Observes change event
     */
    @Override
    public void putFullTextOperationData(FullTextOperationData operationData) {
        //
        // Commit the change immediately if we are not in a transaction
        //
        if (!fullTextSearchService.enabled()) {
            return;
        }
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        log.debug("operationData = {}, (isInTransaction = {})", operationData, dataSource.isInTransaction());
        if (!dataSource.isInTransaction()) {
            try {
                fullTextSearchEngine.commitRow(operationData, tableDataMap.get(operationData.getTableName()));
                fullTextSearchEngine.commitIndex();
            } catch (SQLException exc) {
                log.error("Unable to update the Lucene index", exc);
            }
            return;
        }
        //
        // Save the table update until the update is committed or rolled back.  Note
        // that the current thread is the application thread performing the update operation.
        //
        synchronized (pendingFTSIndexUpdates) {
            log.debug("BATCH tableData = {}, operationData = {}", tableDataMap.get(operationData.getTableName()), operationData);
            pendingFTSIndexUpdates.put(operationData.getTableKey(), operationData);
        }
        //
        // Register our transaction callback
        //
        dataSource.registerCallback(this);
    }

    /**
     * Commit the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void commit() {
        try {
            //
            // Update the Lucene index.  Note that a database transaction is associated
            // with a single thread.  So we will commit just those updates generated
            // by the current thread.
            //
            boolean commit = false;
            log.debug("COMMIT BATCH tableData = {}", pendingFTSIndexUpdates.size());
            synchronized (pendingFTSIndexUpdates) {
                Iterator<FullTextOperationData> updateIt = pendingFTSIndexUpdates.values().iterator();
                while (updateIt.hasNext()) {
                    FullTextOperationData operationData = updateIt.next();
                    if (operationData.getThread().equalsIgnoreCase(Thread.currentThread().getName())) {
                        TableData tableDataFromMap = null;
                        String key = operationData.getTableName().toLowerCase();
                        if (tableDataMap.containsKey(key)) {
                            tableDataFromMap = tableDataMap.get(key);
                        } else {
                            try (Connection conn = databaseManager.getDataSource().getConnection();
                                 Statement stmt = conn.createStatement()) {
                                // lazily persist table's data for future use
                                fullTextSearchService.initTableLazyIfNotPresent(conn, stmt, key);
                                // fetch data and store in map
                                tableDataFromMap = this.readTableData(conn, fullTextConfig.getSchema(), key);
                                this.tableDataMap.put(key, tableDataFromMap);
                            } catch (SQLException e) {
                                log.error("Error updating FTS data table", e);
                                throw new RuntimeException(e);
                            }
                        }
                        fullTextSearchEngine.commitRow(operationData, tableDataFromMap);
                        updateIt.remove();
                        commit = true;
                    }
                }
            }
            //
            // Commit the index updates
            //
            if (commit) {
                fullTextSearchEngine.commitIndex();
            }
        } catch (SQLException exc) {
            log.error("Unable to update the Lucene index", exc);
        }
    }

    /**
     * Discard the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void rollback() {
        synchronized (pendingFTSIndexUpdates) {
            pendingFTSIndexUpdates.entrySet().removeIf(entry ->
                entry.getValue().getThread().equalsIgnoreCase(Thread.currentThread().getName()));
        }
    }

}
