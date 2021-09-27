/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountInfoTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.asset.AssetTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.dgs.DGSGoodsTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.poll.PollTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;
import static com.apollocurrency.aplwallet.apl.util.Constants.LONG_TIME_FIVE_SECONDS;

@Slf4j
@Deprecated
public class DeleteTrimObserver {

    private final Object lock = new Object();
    private final Queue<DeleteOnTrimData> deleteOnTrimDataQueue = new ConcurrentLinkedQueue<>();
    protected final DatabaseManager databaseManager;
    protected final PropertiesHolder propertiesHolder;
    private volatile boolean trimDerivedTablesEnabled = true;
    private final int COMMIT_BATCH_SIZE;
    private final ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-delete-on-trim"));

    private final Event<FullTextOperationData> fullTextOperationDataEvent;
    /**
     * Tables to processed for FTS updates
     */
    private static final Set<String> fullTextSearchTableNames = new HashSet<>(6){{
        add(AccountInfoTable.TABLE_NAME);
        add(AssetTable.TABLE_NAME);
        add(CurrencyTable.TABLE_NAME);
        add(DGSGoodsTable.TABLE_NAME);
        add(PollTable.TABLE_NAME);
        add(TaggedDataTable.TABLE_NAME);
    }};

    @Inject
    public DeleteTrimObserver(DatabaseManager databaseManager,
                              PropertiesHolder propertiesHolder,
                              Event<FullTextOperationData> fullTextOperationDataEvent) {
        this.databaseManager = databaseManager;
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder);
        this.COMMIT_BATCH_SIZE = propertiesHolder.BATCH_COMMIT_SIZE();
        this.fullTextOperationDataEvent = Objects.requireNonNull(fullTextOperationDataEvent);
    }

    @PostConstruct
    void init() {
        // schedule first run with random delay in 5 sec range
        executorService.schedule(taskToCall, ThreadLocalRandom.current().nextLong(
            LONG_TIME_FIVE_SECONDS - 1L) + 1L, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    public void onDeleteTrimDataAsync(@ObservesAsync @TrimEvent DeleteOnTrimData deleteOnTrimData) {
        log.trace("onDeleteTrimDataAsync = {}", deleteOnTrimData);
        if (deleteOnTrimData.isResetEvent()) {
            log.trace("onDeleteTrimDataAsync : clean up = {}, size[{}]",
                deleteOnTrimData.isResetEvent(), deleteOnTrimDataQueue.size());
            this.deleteOnTrimDataQueue.clear();
        } else {
            log.trace("onDeleteTrimDataAsync : queue size = [{}]", deleteOnTrimDataQueue.size());
            this.deleteOnTrimDataQueue.add(deleteOnTrimData);
        }
    }

    private Callable<Void> taskToCall = new Callable<>() {
        public Void call() {
            try {
                // Do work.
                processScheduledDeleteTrimEvent();
            } finally {
                // Reschedule next new Callable with next random delay within 5 sec range
                executorService.schedule(this,
                    ThreadLocalRandom.current().nextLong(
                        LONG_TIME_FIVE_SECONDS - 1L) + 1L, TimeUnit.MILLISECONDS);
            }
            return null;
        }
    };

    private void processScheduledDeleteTrimEvent() {
        log.trace("processScheduledDeleteTrimEvent() scheduled on previous run...");
        if (trimDerivedTablesEnabled) {
            boolean performDeleteTrimData;
            DeleteOnTrimData deleteOnTrimData;
            synchronized (lock) {
                if (trimDerivedTablesEnabled) {
                    deleteOnTrimData = deleteOnTrimDataQueue.peek();
                    performDeleteTrimData = deleteOnTrimData != null;
                    if (performDeleteTrimData) {
                        performOneTableDelete(deleteOnTrimData);
                        deleteOnTrimDataQueue.remove();
                        log.debug("Performed trim on = {}", deleteOnTrimData);
                    } else {
                        log.trace("NO trim data to delete...");
                    }
                }
            }
        } else {
            log.trace("DISABLED: processScheduledDeleteTrimEvent()");
        }
    }

    public long performOneTableDelete(DeleteOnTrimData deleteOnTrimData) {
        log.trace("start performOneTableDelete(): {}", deleteOnTrimData);
        long startDeleteTime = System.currentTimeMillis();
        long deleted = 0L;
        if (deleteOnTrimData != null
            && deleteOnTrimData.getDbIdSet() != null
            && deleteOnTrimData.getDbIdSet().size() > 0) {

            TransactionalDataSource dataSource = databaseManager.getDataSource();
            boolean inTransaction = dataSource.isInTransaction();
            if (!inTransaction) {
                dataSource.begin();
            }
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmtDeleteById =
                     con.prepareStatement("DELETE FROM " + deleteOnTrimData.getTableName() + " WHERE db_id = ?");
            ) {
                long index = 0;
                for (Long id : deleteOnTrimData.getDbIdSet()) {
                    int deleteResult = deleteByDbId(pstmtDeleteById, id);
                    if (deleteResult > 0 && fullTextSearchTableNames.contains(deleteOnTrimData.getTableName())) {
                        // fire Async event to FTS
                        FullTextOperationData operationData = new FullTextOperationData(
                            DEFAULT_SCHEMA, deleteOnTrimData.getTableName(), Thread.currentThread().getName());
                        operationData.setDbIdValue(id);
                        operationData.setOperationType(FullTextOperationData.OperationType.DELETE);
                        log.trace("Put lucene index update data = {}", operationData);
                        this.fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {}).fireAsync(operationData);
                    }
                    deleted += deleteResult;
//                    addDeleteToBatch(pstmtDeleteById, id);
                    index++;
                    if (index % COMMIT_BATCH_SIZE == 0) {
//                        int[] result = pstmtDeleteById.executeBatch();
                        dataSource.commit(false);
//                        deleted += Arrays.stream(result).asLongStream().sum();
                    }
                }
//                int[] result = pstmtDeleteById.executeBatch();
                dataSource.commit(!inTransaction);
//                deleted += Arrays.stream(result).asLongStream().sum();
            } catch (Exception e) {
                log.error("Batch delete error on table {}", deleteOnTrimData.getTableName(), e);
            }
            log.debug("performOneTableDelete(): Delete table '{}' in {} ms: deleted=[{}]",
                deleteOnTrimData.getTableName(), System.currentTimeMillis() - startDeleteTime, deleted);
        }
        return deleted;
    }

    private void addDeleteToBatch(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        pstmtDeleteByDbId.addBatch(); // <-- batching
    }

    private int deleteByDbId(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        return pstmtDeleteByDbId.executeUpdate();
    }

    /**
     * For unit tests mostly
     * @return internal queue with data
     */
    public Queue<DeleteOnTrimData> getDeleteOnTrimDataQueue() {
        return this.deleteOnTrimDataQueue;
    }

    public int getDeleteOnTrimDataQueueSize() {
        return this.deleteOnTrimDataQueue.size();
    }

}
