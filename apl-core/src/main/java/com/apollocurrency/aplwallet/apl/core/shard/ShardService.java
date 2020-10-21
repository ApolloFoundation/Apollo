/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;


import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.TrimConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TrimService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.utils.RuntimeUtils;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ShardService {
    public final static long LOWER_SHARDING_MEMORY_LIMIT = 1536 * 1024 * 1024; //1.5GB
    private final ShardDao shardDao;
    private final BlockchainProcessor blockchainProcessor;
    private final Blockchain blockchain;
    private final DirProvider dirProvider;
    private final Zip zip;
    private final DatabaseManager databaseManager;
    private final BlockchainConfig blockchainConfig;
    private final ShardRecoveryDao shardRecoveryDao;
    private final ShardMigrationExecutor shardMigrationExecutor;
    private final AplAppStatus aplAppStatus;
    private final PropertiesHolder propertiesHolder;
    private final Event<TrimConfig> trimEvent;
    private final Event<DbHotSwapConfig> dbEvent;
    private final TrimService trimService;
    private final GlobalSync globalSync;
    private volatile boolean isSharding;
    private volatile CompletableFuture<MigrateState> shardingProcess = null;

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
        trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {
        }).fire(new TrimConfig(enableTrim, clearQueue));

    }

    public boolean isSharding() {
        return isSharding;
    }

    public void setSharding(boolean sharding) {
        isSharding = sharding;
    }

    public boolean reset(long shardId) {

        Path dbDir = dirProvider.getDbDir();
        Path backupZip = dbDir.resolve(String.format(ShardConstants.DB_BACKUP_FORMAT, new ShardNameHelper().getShardNameByShardId(shardId, blockchainConfig.getChain().getChainId())));
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
                    int shardIndex = fileName.toString().indexOf("_shard_");
                    if ((fileName.toString().endsWith(DbProperties.DB_EXTENSION) || fileName.toString().endsWith("trace.db"))
                        && shardIndex != -1) {
                        String idString = fileName.toString().substring(shardIndex + 7);
                        String id = idString.substring(0, idString.indexOf("-"));
                        long fileShardId = Long.parseLong(id);
                        return fileShardId >= shardId;
                    } else {
                        return false;
                    }
                });
                zip.extract(backupZip.toAbsolutePath().toString(), dbDir.toAbsolutePath().toString(), true);
                databaseManager.setAvailable(true);
                databaseManager.getDataSource(); // force init
                blockchain.update();
                recoverSharding();
                return true;
            } finally {
                globalSync.writeUnlock();
            }
        } finally {
            blockchainProcessor.resumeBlockchainDownloading();
            updateTrimConfig(true, false);
        }
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
            long start = System.currentTimeMillis();
            try {
                // here we are able to recover from stored record
                aplAppStatus.durableTaskStart("sharding", "Blockchain db sharding process takes some time, pls be patient...", true);
                Shard lastShard = shardDao.getLastShard();
                shardMigrationExecutor.prepare();
                shardMigrationExecutor.createAllCommands(lastShard.getShardHeight(), lastShard.getShardId(), recovery.getState());
                shardMigrationExecutor.executeAllOperations();
                log.info("Finished sharding by recovery, height='{}' in {} ms", recovery.getHeight(), System.currentTimeMillis() - start);
                aplAppStatus.durableTaskFinished("sharding", false, "Shard process finished");
            } finally {
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

    public MigrateState performSharding(int newShardBlockHeight, long shardId, MigrateState initialState) {
        MigrateState resultState = MigrateState.FAILED;
        log.debug(">> performSharding '{}' at newShardBlockHeight={}", shardId, newShardBlockHeight);
        if (!shouldPerformSharding()) {
            log.debug("Will skip sharding due to lack of memory or cmd/config properties, shardId='{}'", shardId);
        } else {
            long start = System.currentTimeMillis();
            log.info("Start sharding '{}'....", shardId);

            try {
                shardMigrationExecutor.prepare();
                shardMigrationExecutor.createAllCommands(newShardBlockHeight, shardId, initialState);
                resultState = shardMigrationExecutor.executeAllOperations();
                log.info("Finished sharding #'{}' at height='{}' in {} ms", shardId, newShardBlockHeight, System.currentTimeMillis() - start);
            } catch (Exception t) {
                log.error("Error occurred while trying create shard " + shardId + " at height " + newShardBlockHeight, t);
            }
            if (resultState != MigrateState.FAILED) {
                log.info("Finished sharding successfully in {} secs '{}' at '{}'",
                    (System.currentTimeMillis() - start) / 1000, shardId, newShardBlockHeight);
            } else {
                log.info("FAILED sharding in {} secs '{}' at height='{}'", (System.currentTimeMillis() - start) / 1000, shardId, newShardBlockHeight);
            }
        }
        return resultState;
    }


    public CompletableFuture<MigrateState> tryCreateShardAsync(int newShardBlockHeight, int currentBlockchainHeight) {
        CompletableFuture<MigrateState> newShardingProcess = null;
        log.debug(">> tryCreateShardAsync, scanning ? = {}, !isSharding={},\nCurrent config = {}",
            !blockchainProcessor.isScanning(), !isSharding, blockchainConfig.getCurrentConfig());
        if (!blockchainProcessor.isScanning()) {
            if (!isSharding) {
                Shard lastShard = shardDao.getLastShard();
                if (lastShard == null || newShardBlockHeight > lastShard.getShardHeight()) {
                    isSharding = true;
                    updateTrimConfig(false, false);
                    // quick create records for new Shard and Recovery process for later use
                    long nextShardId = shardDao.getNextShardId();
                    log.debug("Prepare for next sharding = '{}' at currentBlockchainHeight = '{}', newShardBlockHeight = '{}'",
                        nextShardId, currentBlockchainHeight, newShardBlockHeight);
                    saveShardRecoveryAndShard(nextShardId, newShardBlockHeight, currentBlockchainHeight);

                    this.shardingProcess = CompletableFuture.supplyAsync(() -> performSharding(newShardBlockHeight, nextShardId, MigrateState.INIT));
                    this.shardingProcess.handle((result, ex) -> {
                        blockchain.setShardInitialBlock(blockchain.findFirstBlock());
                        updateTrimConfig(true, false);
                        isSharding = false;
                        return result;
                    });
                    newShardingProcess = this.shardingProcess;
                } else {
                    log.warn("Last trim height {} less than last shard height {}", newShardBlockHeight, lastShard.getShardHeight());
                }
            } else {
                log.warn("Unable to start sharding at height {}, previous sharding process was not finished", newShardBlockHeight);
            }
        } else {
            log.warn("Will skip sharding at height {} due to current blockchain scanning !", newShardBlockHeight);
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
            log.warn("Sharding is prohibited by command line or properties 'apl.noshardcreate' value !");
        } else {
            performSharding = RuntimeUtils.isEnoughMemory(LOWER_SHARDING_MEMORY_LIMIT);
            log.warn("Memory check for sharding: performSharding = {}", performSharding);
        }
        return performSharding;
    }

    public Shard getLastShard() {
        return shardDao.getLastShard();
    }

    public List<Shard> getShardsByBlockHeightRange(int heightFrom, int heightTo) {
        // select possibly - none, one, two shard's records by specified height range
        List<Shard> foundShards = shardDao.getCompletedBetweenBlockHeight(heightFrom, heightTo); // reverse params
        log.debug("getShardsByBlockHeightRange( from={}, to={} ): foundShards=[{}] / shardIds={}",
            heightFrom, heightTo, foundShards.size(), foundShards.stream().map(Shard::getShardId).collect(Collectors.toList()));
        return foundShards;
    }

}
