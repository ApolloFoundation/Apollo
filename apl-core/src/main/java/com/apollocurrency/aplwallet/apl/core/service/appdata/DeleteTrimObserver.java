/*
 *  Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.service.appdata;

import static com.apollocurrency.aplwallet.apl.util.Constants.LONG_TIME_FIVE_SECONDS;
import static com.apollocurrency.aplwallet.apl.util.Constants.LONG_TIME_TWO_SECONDS;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteTrimObserver {

    private final Object lock = new Object();
    private final Queue<DeleteOnTrimData> deleteOnTrimDataQueue = new ConcurrentLinkedQueue<>();
    protected final DatabaseManager databaseManager;
    private final PropertiesHolder propertiesHolder;
    private volatile boolean trimDerivedTablesEnabled = true;
    private final ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("apl-delete-on-trim"));
    private final long batchSize = ShardConstants.DEFAULT_COMMIT_BATCH_SIZE;

    @Inject
    public DeleteTrimObserver(DatabaseManager databaseManager,
                              PropertiesHolder propertiesHolder) {
        this.databaseManager = databaseManager;
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
    }

    @PostConstruct
    void init() {
        // schedule first run with random delay in 2 sec range
        executorService.schedule(taskToCall, ThreadLocalRandom.current().nextLong(
            LONG_TIME_TWO_SECONDS - 1L) + 1L, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }

    public void onDeleteTrimDataAsync(@ObservesAsync @TrimEvent DeleteOnTrimData deleteOnTrimData) {
        log.debug("onDeleteTrimDataAsync = {}", deleteOnTrimData);
        if (deleteOnTrimData.isResetEvent()) {
            deleteOnTrimDataQueue.clear();
            log.debug("onDeleteTrimDataAsync : clean up = {}", deleteOnTrimData.isResetEvent());
        } else {
            deleteOnTrimDataQueue.add(deleteOnTrimData);
            log.debug("onDeleteTrimDataAsync : queue size = [{}]", deleteOnTrimDataQueue.size());
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
            boolean performDeleteTrimData = false;
            DeleteOnTrimData deleteOnTrimData = null;
            synchronized (lock) {
                if (trimDerivedTablesEnabled) {
                    deleteOnTrimData = deleteOnTrimDataQueue.peek();
                    performDeleteTrimData = deleteOnTrimData != null;
                    if (performDeleteTrimData) {
                        deleteOnTrimDataQueue.remove();
                        performOneTableDelete(deleteOnTrimData);
                        log.debug("Perform trim on blockchain height={}", deleteOnTrimData);
                    } else {
                        log.trace("NO trim data to delete...");
                    }
                }
            }
        }
    }

    public void performOneTableDelete(DeleteOnTrimData deleteOnTrimData) {
        long startDeleteTime = System.currentTimeMillis();
        long deleted = 0L;
        if (deleteOnTrimData != null
            && deleteOnTrimData.getDbIdSet() != null
            && deleteOnTrimData.getDbIdSet().size() > 0) {

            TransactionalDataSource dataSource = databaseManager.getDataSource();
            try (Connection con = dataSource.getConnection();
                 PreparedStatement pstmtDeleteById =
                     con.prepareStatement("DELETE FROM " + deleteOnTrimData.getTableName() + " WHERE db_id = ?");
            ) {
                long index = 0;
                for (Long id : deleteOnTrimData.getDbIdSet()) {
//                    deleted += addDeleteToBatch(pstmtDeleteById, id);
                    addDeleteToBatch(pstmtDeleteById, id);
                    index++;
                    if (index % batchSize == 0) {
                        int[] result = pstmtDeleteById.executeBatch();
                        dataSource.commit(false);
                        deleted += Arrays.stream(result).asLongStream().sum();
                    }
                }
                int[] result = pstmtDeleteById.executeBatch();
                dataSource.commit(false);
                deleted += Arrays.stream(result).asLongStream().sum();
                log.trace("Delete table '{}' in {} ms: deleted=[{}]",
                    System.currentTimeMillis() - startDeleteTime, deleteOnTrimData.getTableName(), deleted);
            } catch (Exception e) {
                log.error("Batch delete error on table {}", deleteOnTrimData.getTableName(), e);
            }
//            log.debug("Delete for table {} took {} ms", table, System.currentTimeMillis() - startDeleteTime);
        }
    }

    private void addDeleteToBatch(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        pstmtDeleteByDbId.addBatch(); // <-- batching
    }

    private int deleteByDbId(PreparedStatement pstmtDeleteByDbId, long dbId) throws SQLException {
        pstmtDeleteByDbId.setLong(1, dbId);
        return pstmtDeleteByDbId.executeUpdate();
    }
}
