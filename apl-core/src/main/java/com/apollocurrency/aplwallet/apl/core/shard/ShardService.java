/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.utils.RuntimeUtils;
import com.apollocurrency.aplwallet.apl.core.utils.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class ShardService {
    private ShardDao shardDao;
    private BlockchainProcessor blockchainProcessor;
    private Blockchain blockchain;
    private DirProvider dirProvider;
    private Zip zip;
    private DatabaseManager databaseManager;
    private BlockchainConfig blockchainConfig;
    private ShardRecoveryDao shardRecoveryDao;
    private ShardMigrationExecutor shardMigrationExecutor;
    private AplAppStatus aplAppStatus;
    private PropertiesHolder propertiesHolder;
    private Event<TrimConfig> trimEvent;
    private Event<DbHotSwapConfig> dbEvent;
    private TrimService trimService;
    private GlobalSync globalSync;


    private volatile boolean isSharding;
    private volatile CompletableFuture<MigrateState> shardingProcess = null;

    public final static long LOWER_SHARDING_MEMORY_LIMIT = 1536 * 1024 * 1024; //1.5GB

    @Inject
    public ShardService(ShardDao shardDao, BlockchainProcessor blockchainProcessor, Blockchain blockchain,
                        DirProvider dirProvider, Zip zip, DatabaseManager databaseManager,
                        BlockchainConfig blockchainConfig, ShardRecoveryDao shardRecoveryDao,
                        ShardMigrationExecutor shardMigrationExecutor, AplAppStatus aplAppStatus,
                        PropertiesHolder propertiesHolder, Event<TrimConfig> trimEvent, GlobalSync globalSync,
                        TrimService trimService, Event<DbHotSwapConfig> dbEvent) {
        this.shardDao = shardDao;
        this.blockchainProcessor = blockchainProcessor;
        this.blockchain = blockchain;
        this.dirProvider = dirProvider;
        this.zip = zip;
        this.databaseManager = databaseManager;
        this.blockchainConfig = blockchainConfig;
        this.shardRecoveryDao = shardRecoveryDao;
        this.shardMigrationExecutor = shardMigrationExecutor;
        this.aplAppStatus = aplAppStatus;
        this.propertiesHolder = propertiesHolder;
        this.trimEvent = trimEvent;
        this.trimService = trimService;
        this.dbEvent = dbEvent;
        this.globalSync = globalSync;
    }

    public List<Shard> getAllCompletedShards() {
        return shardDao.getAllCompletedShards();
    }

    public List<Shard> getAllCompletedOrArchivedShards() {
        return shardDao.getAllCompletedOrArchivedShards();
    }

    public List<Shard> getAllShards() {
        return shardDao.getAllShard();
    }

    private void updateTrimConfig(boolean enableTrim, boolean clearQueue) {
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(new TrimConfig(enableTrim, clearQueue));

    }

    public boolean isSharding() {
        return isSharding;
    }

    public boolean reset(long shardId) {

        Path dbDir = dirProvider.getDbDir();
        Path backupZip = dbDir.resolve(String.format(ShardConstants.DB_BACKUP_FORMAT, new ShardNameHelper().getShardNameByShardId(shardId, blockchainConfig.getChain().getChainId())));
        boolean backupExists = Files.exists(backupZip);
        if (backupExists) {
            if (isSharding) {
                if (shardingProcess != null) {
                    log.info("Stopping sharding process...");
                    shardingProcess.cancel(true);
                } else {
                    log.info("Unable to stop sharding process. Try again later");
                    return false;
                }
            }

            updateTrimConfig(false, true);
            blockchainProcessor.suspendBlockchainDownloading();
            try {
                log.debug("Waiting finish of last trim");
                while (trimService.isTrimming()) {
                    ThreadUtils.sleep(100);
                }
                globalSync.writeLock();
                try {

                    databaseManager.setAvailable(false);
                    dbEvent.fire(new DbHotSwapConfig(shardId));
                    databaseManager.shutdown();
                    FileUtils.deleteFilesByFilter(dirProvider.getDbDir(), (p) -> {
                        Path fileName = p.getFileName();
                        int shardIndex = fileName.toString().indexOf("-shard-");
                        if ( (fileName.toString().endsWith("h2.db") || fileName.toString().endsWith("trace.db")
                                || fileName.toString().endsWith("lock.db"))
                                && shardIndex != -1) {
                            String idString = fileName.toString().substring(shardIndex + 7);
                            String id = idString.substring(0, idString.indexOf("-"));
                            long fileShardId = Long.parseLong(id);
                            return fileShardId >= shardId;
                        } else {
                            return false;
                        }
                    });
                    zip.extract(backupZip.toAbsolutePath().toString(), dbDir.toAbsolutePath().toString());
                    databaseManager.setAvailable(true);
                    databaseManager.getDataSource(); // force init
                    blockchain.update();
                    recoverSharding();
                    return true;
                }
                finally {
                    globalSync.writeUnlock();
                }
            }
            finally {
                blockchainProcessor.resumeBlockchainDownloading();
                updateTrimConfig(true, false);
            }
        } else {
            log.debug("Backup before shard {} does not exist", shardId);
            return false;
        }
    }

    public void setSharding(boolean sharding) {
        isSharding = sharding;
    }

    public void recoverSharding() {
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery();
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = blockchainConfig.getCurrentConfig().isShardingEnabled();
        boolean shardingEnabledRecoveryExists = shardingEnabled && recovery != null;
        boolean doRecovery = !isShardingOff && shardingEnabledRecoveryExists && recovery.getState() != MigrateState.COMPLETED;
        boolean deleteRecovery = isShardingOff && shardingEnabledRecoveryExists && recovery.getState() == MigrateState.INIT;
        log.debug("do recovery = '{}' parts : (!{} && {} && {} && {})",
                doRecovery,
                isShardingOff,
                shardingEnabled,
                recovery != null,
                recovery != null ? recovery.getState() != MigrateState.COMPLETED : "false"
        );
        if (doRecovery) {
            isSharding = true;
            try {
                // here we are able to recover from stored record
                aplAppStatus.durableTaskStart("sharding", "Blockchain db sharding process takes some time, pls be patient...", true);
                Shard lastShard = shardDao.getLastShard();
                shardMigrationExecutor.cleanCommands();
                shardMigrationExecutor.createAllCommands(lastShard.getShardHeight(), lastShard.getShardId(), recovery.getState());
                shardMigrationExecutor.executeAllOperations();
                aplAppStatus.durableTaskFinished("sharding", false, "Shard process finished");
            }
            finally {
                isSharding = false;
            }
            // when sharding was disabled but recovery records was stored before
            // let's remove records for sharding recovery + shard
        } else if (deleteRecovery) {
            deleteRecovery(recovery);
        }
    }

    @Transactional
    private void deleteRecovery(ShardRecovery recovery) {
        // remove previous recover record if it's in INIT state
        int shardRecoveryDeleted = shardRecoveryDao.hardDeleteShardRecovery(recovery.getShardRecoveryId());
        Shard shard = shardDao.getLastShard();
        Objects.requireNonNull(shard, "Shard record should exist!"); // should exist in current implementation
        shardDao.hardDeleteShard(shard.getShardId());
        log.debug("Cleared shard records : shardRecovery - {}, shard id = {}",
                shardRecoveryDeleted, shard.getShardId());
    }

    public MigrateState performSharding(int minRollbackHeight, long shardId, MigrateState initialState) {
        MigrateState resultState = MigrateState.FAILED;
        if (!shouldPerformSharding()) {
            log.debug("Will skip sharding due to lack of memory or cmd/config properties");
        } else {
            long start = System.currentTimeMillis();
            log.info("Start sharding....");

            try {
                shardMigrationExecutor.cleanCommands();
                shardMigrationExecutor.createAllCommands(minRollbackHeight, shardId, initialState);
                resultState = shardMigrationExecutor.executeAllOperations();
            }
            catch (Exception t) {
                log.error("Error occurred while trying create shard at height " + minRollbackHeight, t);
            }
            if (resultState != MigrateState.FAILED) {
                log.info("Finished sharding successfully in {} secs", (System.currentTimeMillis() - start) / 1000);
            } else {
                log.info("FAILED sharding in {} secs", (System.currentTimeMillis() - start) / 1000);
            }
        }
        return resultState;
    }


    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> newShardingProcess = null;
        if (!blockchainProcessor.isScanning()) {
            if (!isSharding) {
                Shard lastShard = shardDao.getLastShard();
                if (lastShard == null || lastTrimBlockHeight > lastShard.getShardHeight()) {
                    isSharding = true;
                    updateTrimConfig(false, false);
                    // quick create records for new Shard and Recovery process for later use
                    long nextShardId = shardDao.getNextShardId();
                    log.debug("Prepare for next sharding = '{}' at height = '{}', lastTrimHeight = '{}'",
                            nextShardId, blockchainHeight, lastTrimBlockHeight);
                    saveShardRecoveryAndShard(nextShardId, lastTrimBlockHeight, blockchainHeight);

                    this.shardingProcess = CompletableFuture.supplyAsync(() -> performSharding(lastTrimBlockHeight, nextShardId, MigrateState.INIT));
                    this.shardingProcess.handle((result, ex) -> {
                        blockchain.setShardInitialBlock(blockchain.findFirstBlock());
                        updateTrimConfig(true, false);
                        isSharding = false;
                        return result;
                    });
                    newShardingProcess = this.shardingProcess;
                } else {
                    log.warn("Last trim height {} less than last shard height {}", lastTrimBlockHeight, lastShard.getShardHeight());
                }
            } else {
                log.warn("Unable to start sharding at height {}, previous sharding process was not finished", lastTrimBlockHeight);
            }
        } else {
            log.warn("Will skip sharding at height {} due to blockchain scan ", lastTrimBlockHeight);
        }
        return newShardingProcess;
    }

    @Transactional
    public void saveShardRecoveryAndShard(long nextShardId, int lastTrimBlockHeight, int blockchainHeight) {
        Shard newShard = new Shard(nextShardId, lastTrimBlockHeight); // shard starts on specified 'Trim height'
        // shard recovery is stored on specified 'Trim height'
        ShardRecovery recovery = ShardRecovery.builder().state(MigrateState.INIT.toString()).height(lastTrimBlockHeight).build();
        shardRecoveryDao.saveShardRecovery(recovery);
        shardDao.saveShard(newShard); // store shard with HEIGHT AND ID ONLY
        log.debug("Saved initial:\n{}\n{}\nfor trimHeight='{}' at current bch height='{}'",
                newShard, recovery, lastTrimBlockHeight, blockchainHeight);
    }

    private boolean shouldPerformSharding() {
        boolean performSharding = !propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        if (!performSharding) {
            log.warn("Sharding is prohibited by command line or properties");
        } else {
            performSharding = RuntimeUtils.isEnoughMemory(LOWER_SHARDING_MEMORY_LIMIT);
        }
        return performSharding;
    }

}
