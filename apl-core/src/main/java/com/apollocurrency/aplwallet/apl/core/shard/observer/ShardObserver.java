/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimConfigUpdated;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.peer.PeerHttpServer;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardMigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Singleton
public class ShardObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

    private final BlockchainProcessor blockchainProcessor;
    private final BlockchainConfig blockchainConfig;
    private final ShardMigrationExecutor shardMigrationExecutor;
    private final ShardRecoveryDao shardRecoveryDao;
    private final ShardDao shardDao;
    private final Event<Boolean> trimEvent;
    private volatile boolean isSharding;
    private PeerHttpServer peerHttpServer;
    private final Blockchain blockchain;
    private final PropertiesHolder propertiesHolder;
    public final static long LOWER_SHARDING_MEMORY_LIMIT=1536*1024*1024; //1.5GB
    
    @Inject
    public ShardObserver(BlockchainProcessor blockchainProcessor, BlockchainConfig blockchainConfig,
                         ShardMigrationExecutor shardMigrationExecutor,
                         ShardDao shardDao, ShardRecoveryDao recoveryDao,
                         PropertiesHolder propertiesHolder,
                         Blockchain blockchain,
                         Event<Boolean> trimEvent) {
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchain processor is NULL");
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardMigrationExecutor = Objects.requireNonNull(shardMigrationExecutor, "shard migration executor is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(recoveryDao, "shard recovery dao cannot be null");
        this.shardDao = Objects.requireNonNull(shardDao, "shardDao is NULL");
        this.trimEvent = Objects.requireNonNull(trimEvent, "TrimEvent should not be null");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "TrimEvent should not be null");
        this.peerHttpServer=peerHttpServer;
        this.blockchain = Objects.requireNonNull(blockchain, "Blockchain is NULL");
    }


    public void onTrimDoneAsync(@Observes @Async TrimData trimData) {
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
    }

    public void onTrimDone(@Observes @Sync TrimData trimData) {
        try {
            CompletableFuture<Boolean> future = tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
            if (future != null) {
                future.get();
            }
        }
        catch (InterruptedException ex){
            log.debug("Interrupted");
            Thread.currentThread().interrupt();
        }catch(ExecutionException e){
            log.error("Execution failed", e);
        }
    }

    public CompletableFuture<Boolean> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<Boolean> completableFuture = null;
        boolean doSharding = !propertiesHolder.getBooleanProperty("apl.noshardcreate",false);
        if(!doSharding) {
            log.warn("Sharding is prohibited by commad line or properties");
            return completableFuture;
        }
        if(!isEnoughMemory()){
            log.warn("Not enough system memory for Shard creation. Sharding will work in client mode only");
            return completableFuture;            
        }
        
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();

        if (currentConfig.isShardingEnabled()) {
            log.debug("LastTrimHeight-{}, isSharding={}", lastTrimBlockHeight, isSharding);
            if (lastTrimBlockHeight != 0 && lastTrimBlockHeight % currentConfig.getShardingFrequency() == 0) {
                if (!blockchainProcessor.isScanning()) {
                    if (!isSharding) {
                        Shard lastShard = shardDao.getLastShard();
                        if (lastShard == null || lastTrimBlockHeight > lastShard.getShardHeight()) {
                            isSharding = true;
                            updateTrimConfig(false);
                            // quick create records for new Shard and Recovery process for later use
                            shardRecoveryDao.saveShardRecovery(
                                    ShardRecovery.builder().state(MigrateState.INIT.toString()).height(blockchainHeight).build());
                            long nextShardId = shardDao.getNextShardId();
                            Shard newShard = new Shard(nextShardId, lastTrimBlockHeight);
                            shardDao.saveShard(newShard); // store shard with HEIGHT AND ID ONLY

                            completableFuture = CompletableFuture.supplyAsync(() -> performSharding(lastTrimBlockHeight, nextShardId, blockchainHeight))
                                    .thenApply((result) -> {
                                        blockchain.setShardInitialBlock(blockchain.findFirstBlock());
                                        return result;
                                    })
                                    .handle((result, ex) -> {
                                        updateTrimConfig(true);
                                        isSharding = false;
                                        return result;
                                    });
                        } else {
                            log.warn("Last trim height {} less than last shard height {}", lastTrimBlockHeight, lastShard.getShardHeight());
                        }
                    } else {
                        log.warn("Unable to start sharding at height {}, previous sharding process was not finished", lastTrimBlockHeight);
                    }
                } else {
                    log.warn("Will skip sharding at height {} due to blokchain scan ", lastTrimBlockHeight);
                }
            }
        }
        return completableFuture;
    }

    private void updateTrimConfig(boolean enableTrim) {
         trimEvent.select(new AnnotationLiteral<TrimConfigUpdated>() {}).fire(enableTrim);
    }


    private boolean isEnoughMemory(){
        long memoryTotal = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            Object attribute = mBeanServer.getAttribute(new ObjectName("java.lang","type","OperatingSystem"), "TotalPhysicalMemorySize");
            memoryTotal = (Long)attribute;
        } catch (Exception ex) {
        }
        boolean res = false;
        if(memoryTotal==0){
            log.warn("Can not calculate physical RAM size, continuing sharding though it is risky");
            res = true;
        }else{
            res = (memoryTotal >= LOWER_SHARDING_MEMORY_LIMIT);
        }
        if(!res){
            log.warn("Not enough system memory for Shard creation. Sharding will work in client mode only");
            log.debug("Required memory: {}, Available: {} ", LOWER_SHARDING_MEMORY_LIMIT, memoryTotal);
        }
        return res;
    }

    public boolean performSharding(int minRollbackHeight, long shardId, int blockchainHeight) {
        boolean doSharding = !propertiesHolder.getBooleanProperty("apl.noshardcreate",false);
        if(!doSharding){
            log.warn("Sharding is prohibited by commad line or properties");
            return false;
        } 
        if(!isEnoughMemory()){
            return false;            
        }
        boolean result = false;
        MigrateState state = MigrateState.INIT;
        long start = System.currentTimeMillis();
        log.info("Start sharding....");

        try {
            log.debug("Clean commands....");
            shardMigrationExecutor.cleanCommands();
            log.debug("Create all commands....");
            shardMigrationExecutor.createAllCommands(minRollbackHeight, shardId, MigrateState.INIT);
            log.debug("Start all commands....");
            state = shardMigrationExecutor.executeAllOperations();
            result = true;
        }
        catch (Throwable t) {
            log.error("Error occurred while trying create shard at height " + minRollbackHeight, t);
        } finally {
            isSharding = false;
        }
        if (state != MigrateState.FAILED && state != MigrateState.INIT) {
            log.info("Finished sharding successfully in {} secs", (System.currentTimeMillis() - start) / 1000);
        } else {
            log.info("FAILED sharding in {} secs", (System.currentTimeMillis() - start) / 1000);
        }
        return result;
    }
}
