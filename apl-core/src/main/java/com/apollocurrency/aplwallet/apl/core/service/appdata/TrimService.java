/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.config.TrimEventCommand;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.TrimDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TrimEntry;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.apollocurrency.aplwallet.apl.util.Constants.DEFAULT_PRUNABLE_UPDATE_PERIOD;

@Slf4j
@Singleton
public class TrimService {
    @Getter
    private final int maxRollback;
    private final DatabaseManager dbManager;
    private final DerivedTablesRegistry dbTablesRegistry;
    private final TrimDao trimDao;
    private final TimeService timeService;
    private final ReentrantLock lock = new ReentrantLock();

    private final Event<TrimEventCommand> trimConfigEvent;


    @Inject
    public TrimService(DatabaseManager databaseManager,
                       DerivedTablesRegistry derivedDbTablesRegistry,
                       TimeService timeService,
                       Event<TrimEventCommand> trimConfigEvent,
                       TrimDao trimDao,
                       @Property(value = "apl.maxRollback", defaultValue = "" + Constants.MAX_AUTO_ROLLBACK) int maxRollback
    ) {
        this.maxRollback = maxRollback;
        this.trimDao = Objects.requireNonNull(trimDao, "trimDao is NULL");
        this.dbManager = Objects.requireNonNull(databaseManager, "Database manager cannot be null");
        this.dbTablesRegistry = Objects.requireNonNull(derivedDbTablesRegistry, "Db tables registry cannot be null");
        this.timeService = Objects.requireNonNull(timeService, "EpochTime should not be null");
        this.trimConfigEvent = Objects.requireNonNull(trimConfigEvent, "TrimConfig event should not be null");
    }

    public int getLastTrimHeight() {
        TrimEntry trimEntry = trimDao.get();
        return trimEntry == null ? 0 : trimEntry.getHeight();
    }


    public void trimDerivedTables(int height) {
        inLock(()-> {
            log.debug("TRIM: trimDerivedTables on height={}", height);
            TransactionalDataSource dataSource = dbManager.getDataSource();
            boolean inTransaction = dataSource.isInTransaction();
            try {
                if (!inTransaction) {
                    dataSource.begin();
                }
                long startTime = System.currentTimeMillis();
                doTrimDerivedTablesOnBlockchainHeight(height);
                dataSource.commit(!inTransaction);
                log.info("Total trim time: {} ms on height '{}', InTr?=('{}')",
                    (System.currentTimeMillis() - startTime), height, inTransaction);
            } catch (Exception e) {
                log.warn(e.toString(), e);
                dataSource.rollback(!inTransaction);
                throw e;
            }
        });
    }

    public void doTrimDerivedTablesOnBlockchainHeight(int blockchainHeight) {
        inLock(() -> {
            log.debug("TRIM: doTrimDerivedTablesOnBlockchainHeight on height {}", blockchainHeight);
            int trimHeight = Math.max(blockchainHeight - maxRollback, 0);
            if (trimHeight > 0) {
                doAccountableTrimDerivedTables(trimHeight);
            }
        });
    }


    public int doAccountableTrimDerivedTables(int height) {
        return inLock(()-> {
            TrimEntry trimEntry = trimDao.get();
            if (trimEntry == null || !trimEntry.isDone() || trimEntry.getHeight() < height) {
                if (trimEntry == null || trimEntry.getHeight() < height) {
                    trimEntry = new TrimEntry(null, height, false);
                }
                trimDao.clear();
                trimEntry = trimDao.save(trimEntry);
                dbManager.getDataSource().commit(false);
                int pruningTime = doTrimDerivedTablesOnHeight(height);
                trimEntry.setDone(true);
                trimDao.save(trimEntry);
                log.debug("doTrimDerived saved {} at height '{}', pruningTime={}", trimEntry, height, pruningTime);
                return pruningTime;
            } else {
                log.debug("doTrimDerived skipped at blockchain height={} and trim height={}", height, height);
                return 0;
            }
        });
    }

    public void resetTrim() {
        resetTrim(0);
    }

    private void inLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
    private <T> T inLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void resetTrim(int height) {
        inLock(()-> {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
            try {
                if (!inTransaction) {
                    dataSource.begin();
                }
                trimDao.clear();
                if (height > 0) {
                    trimDao.save(new TrimEntry(null, height, true));
                    log.debug("Reset Trim to height={}", height);
                } else {
                    log.debug("Reset Trim.");
                }
                dataSource.commit(!inTransaction);
            } catch (Exception e) {
                log.warn(e.toString(), e);
                dataSource.rollback(!inTransaction);
                throw e;
            }
        });
    }

    @Transactional
    int doTrimDerivedTablesOnHeight(int height) {
        log.debug("TRIM: doTrimDerivedTablesOnHeight on height={}", height);
        long start = System.currentTimeMillis();

        TransactionalDataSource dataSource = dbManager.getDataSource();
        long onlyTrimTime = 0;
        int epochTime = timeService.getEpochTime();
        int pruningTime = epochTime - epochTime % DEFAULT_PRUNABLE_UPDATE_PERIOD;

        for (DerivedTableInterface<?> table : dbTablesRegistry.getDerivedTables()) {
            long startTime = System.currentTimeMillis();
            table.prune(pruningTime);
            table.trim(height);
            dataSource.commit(false);
            long duration = System.currentTimeMillis() - startTime;
            // do not log trim duration here, instead go to the logback config and enable trace logs for BasicDbTable class
            log.trace("Trim of {} took {} ms", table.getName(), duration);
            onlyTrimTime += duration;
        }
        log.info("Trim time onlyTrim/full: {} / {} ms, pruning='{}' on height='{}'",
            onlyTrimTime, System.currentTimeMillis() - start, pruningTime, height);
        return pruningTime;
    }

    public void updateTrimConfig(boolean enableTrim, boolean clearQueue) {
        log.debug("Send event to {} trim thread", enableTrim ? "enable" : "disable");
        trimConfigEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimEventCommand(enableTrim, clearQueue));
    }

    public boolean isTrimming() {
        return lock.isLocked();
    }

    public void waitTrimming() {
        log.debug("Waiting for the end of the latest trim");
        int count = 0;
        while (isTrimming()) {
            ThreadUtils.sleep(100);
            if (count % 10 == 0) {
                log.debug("--- WaitingTrim [{}]. . . Lock: isLocked={}, isFair={}, isHeldByCurrentThread={}, holdCount={}",
                    count, lock.isLocked(), lock.isFair(), lock.isHeldByCurrentThread(), lock.getHoldCount());
            }
            count++;
        }
    }
}
