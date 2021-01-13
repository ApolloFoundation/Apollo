package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.app.runnable.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOperationDao;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOperation;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import com.apollocurrency.aplwallet.apl.util.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Singleton
@Slf4j
public class DexOperationService {
    private static final int DEFAULT_ENTRY_LIFETIME = 90 * 24 * 60 * 60; // 90 days in seconds
    private static final int MIN_ENTRY_LIFETIME = 12 * 60 * 60; // 12 hours in seconds
    private static final int INITIAL_DELAY = 5 * 60 * 1000; // 5 minutes in ms
    private static final int DELAY = 60 * 60 * 1000; // 60 minutes in ms
    private static final String SERVICE_NAME = "DexOperationService";
    private static final String TASK_NAME = "DexOperationCleaner";

    private DexOperationDao dao;
    private boolean deleteOldEntries;
    private long entryLifetime; //ms
    private TaskDispatchManager dispatchManager;

    @Inject
    public DexOperationService(@Property(name = "apl.dex.operations.lifetime", defaultValue = "" + DEFAULT_ENTRY_LIFETIME) int entryLifetime,
                               @Property(name = "apl.dex.operations.deleteOld", defaultValue = "true") boolean deleteOldEntries,
                               DexOperationDao dao,
                               TaskDispatchManager dispatchManager) {
        this.dao = Objects.requireNonNull(dao);
        this.dispatchManager = Objects.requireNonNull(dispatchManager);
        this.deleteOldEntries = deleteOldEntries;
        this.entryLifetime = (long) Math.max(entryLifetime, MIN_ENTRY_LIFETIME) * 1000; // do not let set less than specified limit to prevent confusions and complications related to this feature
    }

    @Transactional(readOnly = true)
    public List<DexOperation> getAll(long fromDbId, int limit) {
        return dao.getAll(fromDbId, limit);
    }

    @PostConstruct
    public void init() {
        TaskDispatcher dispatcher = dispatchManager.newScheduledDispatcher(SERVICE_NAME);
        dispatcher.schedule(Task.builder()
            .initialDelay(INITIAL_DELAY)
            .delay(DELAY)
            .name(TASK_NAME)
            .task(this::deleteWithOffsetIfCleanerEnabled)
            .build());
        log.info("{} was initialized", SERVICE_NAME);
    }

    private void deleteWithOffsetIfCleanerEnabled() {
        if (deleteOldEntries) {
            int deleted = deleteWithOffset(entryLifetime);
            log.info("Deleted {} dex_operation entries", deleted);
        } else {
            log.debug("{} is disabled", TASK_NAME);
        }
    }

    @Transactional
    public void save(DexOperation op) {
        DexOperation existing = dao.getBy(op.getAccount(), op.getStage(), op.getEid());
        if (existing == null) {
            long generatedDbId = dao.add(op);// add new
            op.setDbId(generatedDbId);
        } else {
            op.setDbId(existing.getDbId());
            dao.updateByDbId(op);
        }
    }

    @Transactional
    public void finish(DexOperation operation) {
        Objects.requireNonNull(operation.getDbId(), "Db id is not specified, unable to update such record");
        if (operation.isFinished()) {
            throw new IllegalStateException("Operation is already finished");
        }
        operation.setFinished(true);
        dao.updateByDbId(operation);
    }

    @Transactional
    public int deleteWithOffset(long offsetMs) {
        long currentTime = System.currentTimeMillis();
        long targetTime = currentTime - offsetMs;
        Timestamp targetTimestamp = new Timestamp(targetTime);
        log.info("Will delete all dex_operation entries before {}", targetTimestamp);
        return deleteAfterTimestamp(targetTimestamp);
    }


    @Transactional(readOnly = true)
    public DexOperation getBy(String account, DexOperation.Stage stage, String eid) {
        return dao.getBy(account, stage, eid);
    }

    @Transactional
    public int deleteAfterTimestamp(Timestamp toTimestamp) {
        return dao.deleteAfterTimestamp(toTimestamp);
    }
}
