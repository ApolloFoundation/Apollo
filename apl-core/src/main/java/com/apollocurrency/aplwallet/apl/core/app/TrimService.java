/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimService {
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    private final int maxRollback;
    private final DatabaseManager dbManager;
    private final DerivedTablesRegistry dbTablesRegistry;
    private final GlobalSync globalSync;
    private volatile int lastTrimHeight;


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

    public int getLastTrimHeight() {
        return lastTrimHeight;
    }

    public void trimDerivedTables(int height) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        try {
            if (!dataSource.isInTransaction()) {
                dataSource.begin();
            }
            long startTime = System.currentTimeMillis();
            doTrimDerivedTables(height, dataSource);
            log.debug("Total trim time: " + (System.currentTimeMillis() - startTime));
            dataSource.commit();

        }
        catch (Exception e) {
            log.info(e.toString(), e);
            dataSource.rollback();
            throw e;
        }
    }

    public void doTrimDerivedTables(int height, TransactionalDataSource dataSource) {
        lastTrimHeight = Math.max(height - maxRollback, 0);
        long onlyTrimTime = 0;
        if (lastTrimHeight > 0) {
            for (DerivedDbTable table : dbTablesRegistry.getDerivedTables()) {
                globalSync.readLock();
                try {
                    if (dataSource == null) {
                        dataSource = dbManager.getDataSource();
                    }
                    long startTime = System.currentTimeMillis();
                    table.trim(lastTrimHeight, dataSource);
                    dataSource.commit(false);
                    onlyTrimTime += (System.currentTimeMillis() - startTime);
                }
                finally {
                    globalSync.readUnlock();
                }
            }
        }
        log.debug("Only trim time: " + onlyTrimTime);
    }


}
