/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShardObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

    private BlockchainProcessor blockchainProcessor;
    private BlockchainConfig blockchainConfig;
    private DatabaseManager databaseManager;
    private ShardMigrationExecutor shardMigrationExecutor;

    private volatile boolean isSharding = false;

    @Inject
    public ShardObserver(BlockchainProcessor blockchainProcessor, BlockchainConfig blockchainConfig, DatabaseManager databaseManager, ShardMigrationExecutor shardMigrationExecutor) {
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchain processor is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.shardMigrationExecutor = Objects.requireNonNull(shardMigrationExecutor, "shard migration executor is NULL");
    }

    public void onBlockPushed(@ObservesAsync @BlockEvent(BlockEventType.BLOCK_PUSHED) Block block) {
        tryCreateShard();
    }
    
    public boolean isInSharding(){
        return isSharding;
    }
    
    public synchronized boolean tryCreateShard() {
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        boolean res = false;
        if (currentConfig.isShardingEnabled()) {
            int minRollbackHeight = blockchainProcessor.getMinRollbackHeight();
            if (minRollbackHeight != 0 && minRollbackHeight % currentConfig.getShardingFrequency() == 0) {
                if (isSharding) {
                    log.warn("Previous shard was no finished! Will skip next shard at height: " + minRollbackHeight);
                    log.error("!!! --- SHARD SKIPPING CASE, IT SHOULD NEVER HAPPEN ON PRODUCTION --- !!! You can skip it at YOUR OWN RISK !!!");
                } else {
                    isSharding = true;
                    MigrateState state = MigrateState.INIT;
                    long start = System.currentTimeMillis();
                    log.info("Start sharding....");
                    try {
                        log.debug("Clean commands....");
                        shardMigrationExecutor.cleanCommands();
                        log.debug("Create all commands....");
                        shardMigrationExecutor.createAllCommands(minRollbackHeight);
                        log.debug("Start all commands....");
                        state = shardMigrationExecutor.executeAllOperations();
                    }
                    catch (Throwable t) {
                        log.error("Error occurred while trying create shard at height " + minRollbackHeight, t);
                        res = false;
                    }
                    if (state != MigrateState.FAILED && state != MigrateState.INIT ) {
                        log.info("Finished sharding successfully in {} secs", (System.currentTimeMillis() - start) / 1000);
                        res = true;
                    } else {
                        log.info("FAILED sharding in {} secs", (System.currentTimeMillis() - start) / 1000);
                        res = false;
                    }
                    isSharding = false;
                }
            }
        }
        return res;
    }
}
