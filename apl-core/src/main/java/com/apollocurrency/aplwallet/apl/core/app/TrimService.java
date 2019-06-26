/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimService {
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    private final int maxRollback;
    private final DatabaseManager dbManager;
    private final DerivedTablesRegistry dbTablesRegistry;
    private ScheduledExecutorService executor;
    private final GlobalSync globalSync;
    private volatile int lastTrimHeight;
    private final List<Integer> trimHeights = new ArrayList<>();

    @Inject
    public TrimService(DatabaseManager databaseManager,
                       DerivedTablesRegistry derivedDbTablesRegistry,
                       GlobalSync globalSync,
                       @Property(value = "apl.maxRollback", defaultValue = "720") int maxRollback
    ) {
        this.maxRollback = maxRollback;
        this.dbManager = Objects.requireNonNull(databaseManager, "Database manager cannot be null");
        this.dbTablesRegistry = Objects.requireNonNull(derivedDbTablesRegistry, "Db tables registry cannot be null");
        this.globalSync = Objects.requireNonNull(globalSync, "Synchronization service cannot be null");
    }
    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::processTrimEvent, 0, 500, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public void scheduleTrim(int height) {
        synchronized (trimHeights) {
            trimHeights.add(height);
        }
    }

    private void processTrimEvent() {
        synchronized (trimHeights) {
            if (!trimHeights.isEmpty()) {
                Integer height = trimHeights.remove(0);
                log.debug("Perform trim on height " + height);
                trimDerivedTables(height);
            }
        }
    }

    public int getLastTrimHeight() {
        return lastTrimHeight;
    }

    public void trimDerivedTables(int height) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        try {
            if (!inTransaction) {
                dataSource.begin();
            }
            long startTime = System.currentTimeMillis();
            doTrimDerivedTablesOnBlockchainHeight(height);
            log.debug("Total trim time: " + (System.currentTimeMillis() - startTime));
            dataSource.commit(!inTransaction);
        }
        catch (Exception e) {
            log.info(e.toString(), e);
            dataSource.rollback(!inTransaction);
            throw e;
        }
    }

    public void doTrimDerivedTablesOnBlockchainHeight(int blockchainHeight) {
        lastTrimHeight = Math.max(blockchainHeight - maxRollback, 0);
        if (lastTrimHeight > 0) {
            doTrimDerivedTablesOnHeight(lastTrimHeight);
        }
    }

    public void doTrimDerivedTablesOnHeight(int height) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        long onlyTrimTime = 0;
        for (DerivedTableInterface table : dbTablesRegistry.getDerivedTables()) {
            globalSync.readLock();
            try {
                long startTime = System.currentTimeMillis();
                table.trim(height);
                dataSource.commit(false);
                onlyTrimTime += (System.currentTimeMillis() - startTime);
            }
            finally {
                globalSync.readUnlock();
            }
        }
        log.debug("Only trim time: " + onlyTrimTime);
    }
}
