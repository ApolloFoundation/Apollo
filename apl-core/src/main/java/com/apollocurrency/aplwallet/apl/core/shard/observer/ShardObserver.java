/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.BlockEventType;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShardObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

    private BlockchainProcessor blockchainProcessor;
    private BlockchainConfig blockchainConfig;
    private ShardMigrationExecutor shardMigrationExecutor;
    private ShardRecoveryDao shardRecoveryDao;
    private ShardDao shardDao;
    private Event<Boolean> trimEvent;
    private boolean trimDerivedTables;

    @Inject
    public ShardObserver(BlockchainProcessor blockchainProcessor, BlockchainConfig blockchainConfig,
                         ShardMigrationExecutor shardMigrationExecutor,
                         ShardDao shardDao, ShardRecoveryDao recoveryDao,
                         @Property("apl.trimDerivedTables") boolean trimDerivedTables,
                         Event<Boolean> trimEvent) {
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchain processor is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardMigrationExecutor = Objects.requireNonNull(shardMigrationExecutor, "shard migration executor is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(recoveryDao, "shard recovery dao cannot be null");
        this.shardDao = Objects.requireNonNull(shardDao, "shardDao is NULL");
        this.trimEvent = Objects.requireNonNull(trimEvent, "TrimEvent should not be null");
        this.trimDerivedTables = trimDerivedTables;
    }

    public void onBlockAccepted(@Observes @BlockEvent(BlockEventType.AFTER_BLOCK_ACCEPT) Block block) {
        tryCreateShardAsync();
    }

    public CompletableFuture<Boolean> tryCreateShardAsync() {
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        CompletableFuture<Boolean> completableFuture = null;
        if (currentConfig.isShardingEnabled()) {
            int minRollbackHeight = blockchainProcessor.getMinRollbackHeight();
            if (minRollbackHeight != 0 && minRollbackHeight % currentConfig.getShardingFrequency() == 0) {
                if (!blockchainProcessor.isScanning()) {
                    updateTrimConfig(false);
                    // quick create records for new Shard and Recovery process for later use
                    shardRecoveryDao.saveShardRecovery(new ShardRecovery(MigrateState.INIT));
                    long nextShardId = shardDao.getNextShardId();
                    Shard newShard = new Shard(nextShardId, minRollbackHeight);
                    shardDao.saveShard(newShard); // store shard with HEIGHT AND ID ONLY
                    completableFuture = CompletableFuture.supplyAsync(() -> performSharding(minRollbackHeight))
                            .thenApply((result) -> {
                                blockchainProcessor.updateInitialBlockId();
                                return result;
                            })
                            .handle((result, ex) -> {
                                updateTrimConfig(true);
                                return result;
                            });
                } else {
                    log.warn("Will skip sharding at height {} due to blokchain scan ", minRollbackHeight);
                }
            }
        }
        return completableFuture;
    }

    private void updateTrimConfig(boolean enableTrim) {
        if (trimDerivedTables) {
            trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(enableTrim);
        }
    }

    public boolean performSharding(int minRollbackHeight) {
        boolean result = false;
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
                    result = true;
        } catch (Throwable t) {
            log.error("Error occurred while trying create shard at height " + minRollbackHeight, t);
        }
        if (state != MigrateState.FAILED && state != MigrateState.INIT) {
            log.info("Finished sharding successfully in {} secs", (System.currentTimeMillis() - start) / 1000);
        } else {
            log.info("FAILED sharding in {} secs", (System.currentTimeMillis() - start) / 1000);
        }
        return result;
    }
}
