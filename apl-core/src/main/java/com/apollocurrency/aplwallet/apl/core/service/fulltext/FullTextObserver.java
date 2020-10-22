/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WARNING!!! Trigger instances will be created while construction of DatabaseManager, so that -> do NOT inject DatabaseManager directly into field
 */
@Slf4j
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
@Singleton
public class FullTextObserver implements TransactionCallback {

    /**
     * Pending table updates
     * We collect index row updates and then commit or rollback it when db transaction was finished or rollbacked
     */
    private Map<String, FullTextOperationData> tableUpdates = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;
    private final FullTextSearchEngine fullTextSearchEngine;
    private final FullTextSearchService fullTextSearchService;
    private final FullTextConfig fullTextConfig;
    private Map<String, TableData> tableDataMap = new ConcurrentHashMap<>();

    @Inject
    public FullTextObserver(DatabaseManager databaseManager,
                            FullTextSearchEngine fullTextSearchEngine,
                            FullTextSearchService fullTextSearchService,
                            FullTextConfig fullTextConfig) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.fullTextSearchEngine = Objects.requireNonNull(fullTextSearchEngine, "ftl is NULL");
        this.fullTextSearchService = Objects.requireNonNull(fullTextSearchService, "ftl is NULL");
        this.fullTextConfig = Objects.requireNonNull(fullTextConfig, "ft config is NULL");
    }

    /**
     * Initialize some data.
     */
    public void init() {
        fullTextSearchService.init();
        if (this.fullTextConfig.getTableNames().size() > 0) {
            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try( Connection con = dataSource.getConnection();) {
                while (fullTextConfig.getTableNames().iterator().hasNext()) {
                    String tableName =  fullTextConfig.getTableNames().iterator().next();
                    TableData tableData = readTableData(con, "public", tableName);
                    this.tableDataMap.put(tableName, tableData);
                }
            } catch (SQLException e) {
                log.error("Connection error", e);
            }
        }
        log.debug("Fetched info for = [{}] = {}", this.tableDataMap.size(), this.tableDataMap);
    }

    private TableData readTableData(Connection connection, String schema, String tableName) throws SQLException {
        return DbUtils.getTableData(connection, tableName, schema);
    }

    /**
     * Observes change event
     */
    public void onFullTextOperationData(@ObservesAsync @TrimEvent() FullTextOperationData operationData) {
        if (tableDataMap.size() == 0) {
            init(); // lazy init
        }
        //
        // Commit the change immediately if we are not in a transaction
        //
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
        synchronized (tableUpdates) {
            log.debug("BATCH tableData = {}, operationData = {}", tableDataMap.get(operationData.getTableName()), operationData);
            tableUpdates.put(operationData.getTableKey(), operationData);
        }
        //
        // Register our transaction callback
        //
        dataSource.registerCallback(this);
    }

/*    private DatabaseManager lookupDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager;
    }*/

/*    private FullTextSearchEngine lookupFullTextSearchEngine() {
        if (ftl == null) {
            ftl = CDI.current().select(FullTextSearchEngine.class).get();
        }
        return ftl;
    }*/

    /**
     * Commit the table changes for the current transaction (TransactionCallback interface)
     */
    @Override
    public void commit() {
//        Thread thread = Thread.currentThread();
        try {
            //
            // Update the Lucene index.  Note that a database transaction is associated
            // with a single thread.  So we will commit just those updates generated
            // by the current thread.
            //
            boolean commit = false;
            log.debug("COMMIT BATCH tableData = {}", tableUpdates.size());
            synchronized (tableUpdates) {
                Iterator<FullTextOperationData> updateIt = tableUpdates.values().iterator();
                while (updateIt.hasNext()) {
                    FullTextOperationData operationData = updateIt.next();
//                    if (update.getThread() == thread) {
                        fullTextSearchEngine.commitRow(operationData, tableDataMap.get(operationData.getTableName()));
                        updateIt.remove();
                        commit = true;
//                    }
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
//        Thread thread = Thread.currentThread();
        synchronized (tableUpdates) {
            tableUpdates.clear();
        }
    }

}
