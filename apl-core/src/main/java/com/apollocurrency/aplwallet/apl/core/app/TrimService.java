/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.observer.TrimData;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import static com.apollocurrency.aplwallet.apl.util.Constants.DEFAULT_PRUNABLE_UPDATE_PERIOD;

@Singleton
public class TrimService {
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    @Getter
    private final int maxRollback;
    private final int trimFrequency;
    private final DatabaseManager dbManager;
    private final DerivedTablesRegistry dbTablesRegistry;
    private final TrimDao trimDao;
    private final GlobalSync globalSync;
    private final TimeService timeService;
    private final ReentrantLock lock = new ReentrantLock();

    private Event<TrimData> trimEvent;
    private Event<TrimConfig> trimConfigEvent;


    @Inject
    public TrimService(DatabaseManager databaseManager,
                       DerivedTablesRegistry derivedDbTablesRegistry,
                       GlobalSync globalSync,
                       TimeService timeService,
                       Event<TrimData> trimEvent,
                       Event<TrimConfig> trimConfigEvent,
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
        this.trimConfigEvent = Objects.requireNonNull(trimConfigEvent, "TrimConfig event should not be null");
    }

    public int getLastTrimHeight() {
        TrimEntry trimEntry = trimDao.get();
        return trimEntry == null ? 0 : Math.max(trimEntry.getHeight() - maxRollback, 0);
    }


    public void init(int height, int shardInitialBlockHeight) {
        log.debug("TRIM: init() at height = {}, shard initial height={}", height, shardInitialBlockHeight);
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
            if( lastTrimHeight < shardInitialBlockHeight){
                //we need to change the lastTrimHeight value according to the first block in the latest shard
                lastTrimHeight = shardInitialBlockHeight;
                log.info("Set last trim height to shard initial block height={}", lastTrimHeight);
            }
            if (!trimEntry.isDone()) {
                log.info("Finish trim at height {}", lastTrimHeight);
                trimDerivedTables(lastTrimHeight, false);
            }
            //TODO: Do we really need to do so many iterations or something about two-three heights from tail would be enough?
            for (int i = lastTrimHeight + trimFrequency; i <= height; i += trimFrequency) {
                log.info("Perform trim on height {}", i);
                trimDerivedTables(i, false);
            }
        } finally {
            lock.unlock();
        }
    }

    public void trimDerivedTables(int height, boolean async) {
        log.debug("TRIM: trimDerivedTables on height={}, async={}", height, async);
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
        log.debug("TRIM: doTrimDerivedTablesOnBlockchainHeight on height {} as async operation (? = {})", blockchainHeight, async);
        lock.lock();
        try {
            int trimHeight = Math.max(blockchainHeight - maxRollback, 0);
            if (trimHeight > 0) {
                TrimEntry trimEntry = trimDao.get();
                if (trimEntry == null || !trimEntry.isDone() || trimEntry.getHeight() < blockchainHeight) {
                    if (trimEntry == null || trimEntry.getHeight() < blockchainHeight){
                        trimEntry = new TrimEntry(null, blockchainHeight, false);
                    }
                    trimDao.clear();
                    trimEntry = trimDao.save(trimEntry);
                    dbManager.getDataSource().commit(false);
                    int pruningTime = doTrimDerivedTablesOnHeight(trimHeight);
                    if (async) {
                        log.debug("Fire doTrimDerived event height '{}' Async, trimHeight={}", blockchainHeight, trimHeight);
                        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
                        }).fireAsync(new TrimData(trimHeight, blockchainHeight, pruningTime));
                    } else {
                        log.debug("Fire doTrimDerived event height '{}' Sync, trimHeight={}", blockchainHeight, trimHeight);
                        trimEvent.select(new AnnotationLiteral<TrimEvent>() {
                        }).fire(new TrimData(trimHeight, blockchainHeight, pruningTime));
                    }
                    trimEntry.setDone(true);
                    trimDao.save(trimEntry);
                    log.debug("doTrimDerived saved {} at height '{}'", trimEntry, blockchainHeight);
                }else{
                    log.debug("doTrimDerived skipped at blockchain height={} and trim height={}", blockchainHeight, trimHeight);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void resetTrim() {
        resetTrim(0);
    }

    public void resetTrim(int height) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        lock.lock();
        try {
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
            }catch (Exception e){
                log.warn(e.toString(), e);
                dataSource.rollback(!inTransaction);
                throw e;
            }
        }finally {
            lock.unlock();
        }
    }
    
    public int doTrimDerivedTablesOnHeightLocked(int height) {
        int res = 0;
        lock.lock();
        try {
            res = doTrimDerivedTablesOnHeight(height);
        } finally {
            lock.unlock();
        }
        return res;
    }
    
    @Transactional
    private int doTrimDerivedTablesOnHeight(int height) {
        log.debug("TRIM: doTrimDerivedTablesOnHeight on height={}", height);
        long start = System.currentTimeMillis();

        TransactionalDataSource dataSource = dbManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        log.debug("doTrimDerivedTablesOnHeight height = '{}', inTransaction = '{}'",  height, inTransaction);
        if (!inTransaction) {
            dataSource.begin();
        }
        long onlyTrimTime = 0;
        int epochTime = timeService.getEpochTime();
        int pruningTime = epochTime - epochTime % DEFAULT_PRUNABLE_UPDATE_PERIOD;

            for (DerivedTableInterface table : dbTablesRegistry.getDerivedTables()) {
                log.trace("Try to acquire lock...");
                globalSync.readLock();
                log.trace("Got it.");
                try {
                    long startTime = System.currentTimeMillis();
                    table.prune(pruningTime);
                    table.trim(height);
                    dataSource.commit(false);
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Trim of {} took {} ms", table.getName(), duration);
                    onlyTrimTime += duration;
                } finally {
                    globalSync.readUnlock();
                }
            }

        log.info("Trim time onlyTrim/full: {} / {} ms, pruning='{}' on height='{}'",
                onlyTrimTime, System.currentTimeMillis() - start, pruningTime, height);
        return pruningTime;
    }

    public void updateTrimConfig(boolean enableTrim, boolean clearQueue) {
        log.debug("Send event to {} trim thread", enableTrim?"enable":"disable");
        trimConfigEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(enableTrim, clearQueue));
    }

    public boolean isTrimming() {
        return lock.isLocked();
    }

    public void waitTrimming(){
        log.debug("Waiting for the end of the latest trim");
        int count =0;
        while ( isTrimming() ) {
            ThreadUtils.sleep(100);
            if(count%10==0){
              log.debug("--- Waiting . . . Lock: isLocked={}, isFair={}, isHeldByCurrentThread={}, holdCount={}",
                        lock.isLocked(), lock.isFair(), lock.isHeldByCurrentThread(), lock.getHoldCount());
            }
            count++;
        }
    }
}
