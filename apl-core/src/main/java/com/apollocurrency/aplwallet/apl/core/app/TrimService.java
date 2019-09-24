/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimService {
    private static final int DEFAULT_PRUNABLE_UPDATE_PERIOD = 3600;
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    private final int maxRollback;
    private final int trimFrequency;
    private final DatabaseManager dbManager;
    private final DerivedTablesRegistry dbTablesRegistry;
    private final TrimDao trimDao;
    private final GlobalSync globalSync;
    private final TimeService timeService;
    private final ReentrantLock lock = new ReentrantLock();

    private Event<TrimData> trimEvent;


    @Inject
    public TrimService(DatabaseManager databaseManager,
                       DerivedTablesRegistry derivedDbTablesRegistry,
                       GlobalSync globalSync,
                       TimeService timeService,
                       Event<TrimData> trimEvent,
                       TrimDao trimDao,
                       @Property(value = "apl.maxRollback", defaultValue = "720") int maxRollback
    ) {
        this.maxRollback = maxRollback;
        this.trimDao = Objects.requireNonNull(trimDao, "trimDao is NULL");
        this.dbManager = Objects.requireNonNull(databaseManager, "Database manager cannot be null");
        this.dbTablesRegistry = Objects.requireNonNull(derivedDbTablesRegistry, "Db tables registry cannot be null");
        this.globalSync = Objects.requireNonNull(globalSync, "Synchronization service cannot be null");
        this.timeService = Objects.requireNonNull(timeService, "EpochTime should not be null");
        this.trimFrequency = Constants.DEFAULT_TRIM_FREQUENCY;
        this.trimEvent = Objects.requireNonNull(trimEvent, "Trim event should not be null");
    }

    public int getLastTrimHeight() {
        TrimEntry trimEntry = trimDao.get();
        return trimEntry == null ? 0 : Math.max(trimEntry.getHeight() - maxRollback, 0);
    }


    public void init(int height) {
        log.debug("init() at height = {}", height);
        lock.lock();
        try {
            TrimEntry trimEntry = trimDao.get();
            if (trimEntry == null) {
                log.info("Trim was not saved previously (existing database on new code). Skip trim");
                trimDao.save(new TrimEntry(null, height, true));
                return;
            }
            int lastTrimHeight = trimEntry.getHeight();
            log.info("Last trim height '{}' was done? ='{}', supplied height {}",
                    lastTrimHeight, trimEntry.isDone(), height);
            if (!trimEntry.isDone()) {
                log.info("Finish trim at height {}", lastTrimHeight);
                trimDerivedTables(lastTrimHeight, false);
            }
            for (int i = lastTrimHeight + trimFrequency; i <= height; i += trimFrequency) {
                log.info("Perform trim on height {}", i);
                trimDerivedTables(i, false);
            }
        } finally {
            lock.unlock();
        }
    }

    public void trimDerivedTables(int height, boolean async) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        lock.lock();
        try {
            try {
                if (!inTransaction) {
                    dataSource.begin();
                }
                long startTime = System.currentTimeMillis();
                doTrimDerivedTablesOnBlockchainHeight(height, async);
                dataSource.commit(!inTransaction);
                log.info("Total trim time: {} ms on height '{}', InTr?=('{}')",
                        (System.currentTimeMillis() - startTime), height, inTransaction);
            } catch (Exception e) {
                log.warn(e.toString(), e);
                dataSource.rollback(!inTransaction);
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    public void doTrimDerivedTablesOnBlockchainHeight(int blockchainHeight, boolean async) {
        log.debug("doTrimDerived on height {} as async operation (? = {})", blockchainHeight, async);
        lock.lock();
        try {
            int trimHeight = Math.max(blockchainHeight - maxRollback, 0);
            if (trimHeight > 0) {

                TrimEntry trimEntry = new TrimEntry(null, blockchainHeight, false);
                trimDao.clear();
                trimEntry = trimDao.save(trimEntry);
                dbManager.getDataSource().commit(false);
                int pruningTime = doTrimDerivedTablesOnHeight(trimHeight, false);
                if (async) {
                    log.debug("Fire doTrimDerived event height '{}' Async, trimHeight={}", blockchainHeight, trimHeight);
                    trimEvent.select(new AnnotationLiteral<Async>() {}).fire(new TrimData(trimHeight, blockchainHeight, pruningTime));
                } else {
                    log.debug("Fire doTrimDerived event height '{}' Sync, trimHeight={}", blockchainHeight, trimHeight);
                    trimEvent.select(new AnnotationLiteral<Sync>() {}).fire(new TrimData(trimHeight, blockchainHeight, pruningTime));
                }
                trimEntry.setDone(true);
                trimDao.save(trimEntry);
                log.debug("doTrimDerived saved {} at height '{}'", trimEntry, blockchainHeight);
            }
        } finally {
            lock.unlock();
        }
    }

    public void resetTrim() {
        trimDao.clear();
    }

    @Transactional
    public int doTrimDerivedTablesOnHeight(int height, boolean oneLock) {
        long start = System.currentTimeMillis();
        lock.lock();
        try {
            if (oneLock) {
                globalSync.readLock();
            }
            TransactionalDataSource dataSource = dbManager.getDataSource();
            boolean inTransaction = dataSource.isInTransaction();
            log.debug("doTrimDerivedTablesOnHeight height = '{}', inTransaction = '{}'",
                    height, inTransaction);
            if (!inTransaction) {
                dataSource.begin();
            }
            long onlyTrimTime = 0;
            int epochTime = timeService.getEpochTime();
            int pruningTime = epochTime - epochTime % DEFAULT_PRUNABLE_UPDATE_PERIOD;
            try {
                for (DerivedTableInterface table : dbTablesRegistry.getDerivedTables()) {
                    if (!oneLock) {
                        globalSync.readLock();
                    }
                    try {
                        long startTime = System.currentTimeMillis();
                        table.prune(pruningTime);
                        table.trim(height);
                        dataSource.commit(false);
                        long duration = System.currentTimeMillis() - startTime;
                        log.debug("Trim of {} took {} ms",table.getName(), duration);
                        onlyTrimTime += duration;
                    } finally {
                        if (!oneLock) {
                            globalSync.readUnlock();
                        }
                    }
                }
            } finally {
                if (oneLock) {
                    globalSync.readUnlock();
                }
            }
            log.info("Trim time onlyTrim/full: {} / {} ms, pruning='{}' on height='{}'",
                    onlyTrimTime, System.currentTimeMillis() - start, pruningTime, height);
            return pruningTime;

        } finally {
            lock.unlock();
        }
//        return pruningTime;
    }

    public boolean isTrimming() {
        return lock.isLocked();
    }
}
