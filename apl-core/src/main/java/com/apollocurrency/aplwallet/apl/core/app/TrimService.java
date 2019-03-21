/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TrimService {
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    private final boolean trimDerivedTables;
    private final int trimFrequency;
    private final int maxRollback;
    private final DatabaseManager dbManager;
    private final DerivedDbTablesRegistry dbTablesRegistry;
    private final GlobalSync globalSync;
    private volatile boolean isTrimming;
    private volatile int lastTrimHeight;


    @Inject
    public TrimService(@Property("apl.trimDerivedTables") boolean trimDerivedTables,
                       @Property("apl.trimFrequency") int trimFrequency,
                       @Property(value = "apl.maxRollback", defaultValue = "720") int maxRollback,
                       DatabaseManager databaseManager,
                       DerivedDbTablesRegistry derivedDbTablesRegistry,
                       GlobalSync globalSync
    ) {
        this.trimDerivedTables = trimDerivedTables;
        this.trimFrequency = trimFrequency;
        this.maxRollback = maxRollback;
        this.dbManager = Objects.requireNonNull(databaseManager, "Database manager cannot be null");
        this.dbTablesRegistry = Objects.requireNonNull(derivedDbTablesRegistry, "Db tables registry cannot be null");
        this.globalSync = Objects.requireNonNull(globalSync, "Synchronization service cannot be null");
    }

//    private BlockchainProcessor blockchainProcessor;

    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        if (block.getHeight() % 5000 == 0) {
            log.info("processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            doTrimDerivedTables(block.getHeight(), null);
        }
    }

    // async
    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0 && !isTrimming) {
            isTrimming = true;
            trimDerivedTables(block.getHeight());
            isTrimming = false;
        }
    }

/*    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }*/

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
