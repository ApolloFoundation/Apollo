/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
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

    private volatile boolean isSharding;

    public boolean tryCreateShard() {
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        boolean res = false;
        if (currentConfig.isShardingEnabled()) {
            int minRollbackHeight = blockchainProcessor.getMinRollbackHeight();
            if (minRollbackHeight != 0 && minRollbackHeight % currentConfig.getShardingFrequency() == 0) {
                if (isSharding) {
                    log.warn("Previous shard was no finished! Will skip next shard at height: " + minRollbackHeight);
                } else {
                    isSharding = true;
                    log.info("Start sharding....");
                    try {
                        log.debug("Clean commands....");
                        shardMigrationExecutor.cleanCommands();
                        log.debug("Create all commands....");
                        shardMigrationExecutor.createAllCommands(minRollbackHeight);
                        log.debug("Start all commands....");
                        shardMigrationExecutor.executeAllOperations();
                    }
                    catch (Throwable t) {
                        log.error("Error occurred while trying create shard at height " + minRollbackHeight, t);
                    }
                    log.info("Finished sharding successfully!");
                    res = true;
                    isSharding = false;
                }
            }
        }
        return res;
    }
}
