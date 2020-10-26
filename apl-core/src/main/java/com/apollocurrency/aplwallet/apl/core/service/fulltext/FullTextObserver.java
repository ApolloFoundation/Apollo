/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.fulltext;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.FullTextSearchDataEvent;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
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

        fullTextSearchService.init();
        if (this.fullTextConfig.getTableNames().size() > 0) {
            try( Connection con = dataSource.getConnection();) {
                Iterator<String> iterator = fullTextConfig.getTableNames().keySet().iterator();
                while (iterator.hasNext()) {
                    String tableName =  iterator.next();
                    try {
                        TableData tableData = this.readTableData(con, fullTextConfig.getSchema(), tableName);
                        this.tableDataMap.put(tableName, tableData);
                    } catch (SQLException throwables) {
                        log.error("Reading table '{}'", tableName);
                    }
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
    public void onFullTextOperationData(@ObservesAsync @FullTextSearchDataEvent() FullTextOperationData operationData) {
/*
        if (tableDataMap.size() == 0) {
            init(); // lazy init
        }
*/
        //
        // Commit the change immediately if we are not in a transaction
        //
/*
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
*/
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
//        dataSource.registerCallback(this);
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
                    if (operationData.getThread().equalsIgnoreCase(Thread.currentThread().getName())) {
                        fullTextSearchEngine.commitRow(operationData, tableDataMap.get(operationData.getTableName()));
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
//        Thread thread = Thread.currentThread();
        synchronized (tableUpdates) {
            tableUpdates.clear();
        }
    }

}
