/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.shard.ShardImporter;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporter;
import com.apollocurrency.aplwallet.apl.util.Zip;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Interface for shard data downloading management. It does following:
 * In case empty database (node is started from the scratch) it will do:
 * - connect to peers and check if sharding is present in current network
 * - gather statistics info about shard hashes/data from peers
 *
 * If shard is not present it starts 'old' process importing Genesis data
 */
@Slf4j
@Singleton
public class ShardDownloadPresenceObserver {

    private DatabaseManager databaseManager;
    private Blockchain blockchain;
    private BlockchainProcessor blockchainProcessor;
    private DerivedTablesRegistry derivedTablesRegistry;
    private CsvImporter csvImporter;
    private Zip zipComponent;
    private DownloadableFilesManager downloadableFilesManager;
    private AplAppStatus aplAppStatus;
    private GlobalSync globalSync;
    private ShardImporter shardImporter;
    private BlockchainConfigUpdater blockchainConfigUpdater;

    @Inject
    public ShardDownloadPresenceObserver(DatabaseManager databaseManager, BlockchainProcessor blockchainProcessor,
                                         Blockchain blockchain, DerivedTablesRegistry derivedTablesRegistry,
                                         Zip zipComponent, CsvImporter csvImporter,
                                         DownloadableFilesManager downloadableFilesManager,
                                         AplAppStatus aplAppStatus, GlobalSync globalSync,
                                         ShardImporter shardImporter, BlockchainConfigUpdater blockchainConfigUpdater) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchainProcessor is NULL");
        this.derivedTablesRegistry = Objects.requireNonNull(derivedTablesRegistry, "derivedTablesRegistry is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.zipComponent = Objects.requireNonNull(zipComponent, "zipComponent is NULL");
        this.csvImporter = Objects.requireNonNull(csvImporter, "csvImporter is NULL");
        this.downloadableFilesManager = Objects.requireNonNull(downloadableFilesManager, "downloadableFilesManager is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        this.globalSync = Objects.requireNonNull(globalSync, "globalSync is NULL");
        this.shardImporter = Objects.requireNonNull(shardImporter, "shardImporter is NULL");
        this.blockchainConfigUpdater = Objects.requireNonNull(blockchainConfigUpdater, "blockchainConfigUpdater is NULL");
    }

    /**
     * Method is called when node knows it's int sharded network and shard ZIP information has been downloaded by transport subsystem.
     *
     * @param shardPresentData shard present data contains downloaded ZIP name
     */
    public void onShardPresent(@Observes @ShardPresentEvent(ShardPresentEventType.SHARD_PRESENT) ShardPresentData shardPresentData) {
        shardImporter.importShard(shardPresentData.getFileIdValue(), List.of());
        log.info("SNAPSHOT block should be READY in database...");
        blockchainProcessor.updateInitialSnapshotBlock();
        Block lastBlock = blockchain.findLastBlock();
        log.debug("SNAPSHOT Last block height: " + lastBlock.getHeight());
        blockchainConfigUpdater.updateToLatestConfig();
        blockchainProcessor.setGetMoreBlocks(true); // turn ON blockchain downloading
        log.info("onShardPresent() finished Last block height: " + lastBlock.getHeight());
    }

    /**
     * Process event when no sharding is present in network and we should go 'old scenario'
     *
     * @param shardPresentData not used actually
     */
    public void onNoShardPresent(@Observes @ShardPresentEvent(ShardPresentEventType.NO_SHARD) ShardPresentData shardPresentData) {
        // start adding old Genesis Data
            try {
                log.info("Genesis block not in database, starting from scratch");
                TransactionalDataSource dataSource = databaseManager.getDataSource();
//        Connection con = dataSource.begin();
                try (Connection con = dataSource.begin()) {
                    Block genesisBlock = Genesis.newGenesisBlock();
                    addBlock(dataSource, genesisBlock);
                    long initialBlockId = genesisBlock.getId();
                    log.debug("Generated Genesis block with Id = {}", initialBlockId);
                    Genesis.apply(false);
                    for (DerivedTableInterface table : derivedTablesRegistry.getDerivedTables()) {
                        table.createSearchIndex(con);
                    }
                    blockchain.commit(genesisBlock);
                    dataSource.commit();
                    log.debug("Saved Genesis block = {}", genesisBlock);
                } catch (SQLException e) {
                    dataSource.rollback();
                    log.info(e.getMessage());
                    throw new RuntimeException(e.toString(), e);
                }
                // set to start work block download thread (starting from Genesis block here)
                log.debug("Before updating BlockchainProcessor from Genesis and RESUME block downloading...");
                blockchainProcessor.updateInitialBlock();
                blockchainProcessor.resumeBlockchainDownloading(); // IMPORTANT CALL !!!

            } catch (Exception e) {
                log.error(e.toString(), e);
            }
    }

    private void addBlock(TransactionalDataSource dataSource, Block block) {
        try (Connection con = dataSource.getConnection()) {
            blockchain.saveBlock(con, block);
            blockchain.setLastBlock(block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


}
