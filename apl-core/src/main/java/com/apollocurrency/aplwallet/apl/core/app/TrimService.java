/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.config.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.config.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

public class TrimService {
    private static final Logger log = LoggerFactory.getLogger(TrimService.class);
    private final boolean trimDerivedTables;
    private final int trimFrequency;
    private final int maxRollback;
    private final DatabaseManager dbManager;
    private final DerivedDbTablesRegistry dbTablesRegistry;
    private final SynchronizationService synchronizationService;
    private volatile boolean isTrimming;
    private volatile int lastTrimHeight;


    @Inject
    public TrimService(@Property("apl.trimDerivedTables") boolean trimDerivedTables,
                       @Property("apl.trimFrequency") int trimFrequency,
                       @Property(value = "apl.maxRollback", defaultValue = "720") int maxRollback,
                       DatabaseManager databaseManager,
                       DerivedDbTablesRegistry derivedDbTablesRegistry,
                       SynchronizationService synchronizationService
    ) {
        this.trimDerivedTables = trimDerivedTables;
        this.trimFrequency = trimFrequency;
        this.maxRollback = maxRollback;
        this.dbManager = Objects.requireNonNull(databaseManager, "Database manager cannot be null");
        this.dbTablesRegistry = Objects.requireNonNull(derivedDbTablesRegistry, "Db tables registry cannot be null");
        this.synchronizationService = Objects.requireNonNull(synchronizationService, "Synchronization service cannot be null");
    }

    private BlockchainProcessor blockchainProcessor;

    //async
    public void onBlockScanned(@Observes @BlockEvent(BlockEventType.BLOCK_SCANNED) Block block) {
        if (block.getHeight() % 5000 == 0) {
            log.info("processed block " + block.getHeight());
        }
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
            doTrimDerivedTables(block.getHeight());
        }
    }

    // async
    public void onBlockPushed(@Observes @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        if (trimDerivedTables && block.getHeight() % trimFrequency == 0 && !isTrimming) {
            isTrimming = true;
            trimDerivedTables(block.getHeight());
            isTrimming = false;
        }
    }

    private BlockchainProcessor lookupBlockchainProcessor() {
        if (blockchainProcessor == null) {
            blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get();
        }
        return blockchainProcessor;
    }

    public void trimDerivedTables(int height) {
        TransactionalDataSource dataSource = dbManager.getDataSource();
        try {
            dataSource.begin();
            long startTime = System.currentTimeMillis();
            doTrimDerivedTables(height);
            log.debug("Total trim time: " + (System.currentTimeMillis() - startTime));
            dataSource.commit();

        }
        catch (Exception e) {
            log.info(e.toString(), e);
            dataSource.rollback();
            throw e;
        }
    }

    private void doTrimDerivedTables(int height) {
        lastTrimHeight = Math.max(height - maxRollback, 0);
        long onlyTrimTime = 0;
        if (lastTrimHeight > 0) {
            for (DerivedDbTable table : dbTablesRegistry.getDerivedTables()) {
                synchronizationService.readLock();
                try {
                    TransactionalDataSource dataSource = dbManager.getDataSource();
                    long startTime = System.currentTimeMillis();
                    table.trim(lastTrimHeight);
                    dataSource.commit(false);
                    onlyTrimTime += (System.currentTimeMillis() - startTime);
                }
                finally {
                    synchronizationService.readUnlock();
                }
            }
        }
        log.debug("Only trim time: " + onlyTrimTime);
    }


}
