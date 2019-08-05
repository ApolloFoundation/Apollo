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

    private final DatabaseManager databaseManager;
    private final Blockchain blockchain;
    private final BlockchainProcessor blockchainProcessor;
    private final DerivedTablesRegistry derivedTablesRegistry;
    private final CsvImporter csvImporter;
    private final Zip zipComponent;
    private final DownloadableFilesManager downloadableFilesManager;
    private final AplAppStatus aplAppStatus;
    private final GlobalSync globalSync;
    private final ShardImporter shardImporter;
    private final BlockchainConfigUpdater blockchainConfigUpdater;

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
        String fileId = shardPresentData.getFileIdValue();
        try {
            shardImporter.importShardByFileId(fileId);
        } catch (Exception e) {
            log.error("Error on Shard # {}. Zip/CSV importing...", fileId);
            log.error("Node has encountered serious error and import CSV shard data. " +
                    "Somethings wrong with processing fileId =\n'{}'\n >>> FALL BACK to Genesis importing....", fileId);
            // truncate all partial data potentially imported into database
            cleanUpPreviouslyImportedData();
            // fall back to importing Genesis and starting from beginning
            onNoShardPresent(shardPresentData);
            return;
        }
        log.info("SNAPSHOT block should be READY in database...");
        Block lastBlock = blockchain.findLastBlock();
        log.debug("SNAPSHOT Last block height: " + lastBlock.getHeight());
        blockchainConfigUpdater.updateToLatestConfig();
        blockchainProcessor.resumeBlockchainDownloading(); // turn ON blockchain downloading
        log.info("onShardPresent() finished Last block height: " + lastBlock.getHeight());
    }

    /**
     * Remove all previously imported data from db
     */
    private void cleanUpPreviouslyImportedData() {
        log.debug("start CleanUp after UNSUCCESSFUL zip import...");
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            blockchain.deleteAll();
            derivedTablesRegistry.getDerivedTables().forEach(DerivedTableInterface::truncate);
            dataSource.commit();
            log.debug("Finished CleanUp after UNSUCCESSFUL zip import");
        } catch (Exception e) {
            log.error("Error cleanUp after UNSUCCESSFUL zip import", e);
            dataSource.rollback();
            log.error("Please delete database files and try to run with command line option : --no-shards-import true");
        }
    }

    /**
     * Process event when no sharding is present in network and we should go 'old scenario'
     *
     * @param shardPresentData not used actually
     */
    public void onNoShardPresent(@Observes @ShardPresentEvent(ShardPresentEventType.NO_SHARD) ShardPresentData shardPresentData) {
        // start adding old Genesis Data
        log.trace("Catch event NO_SHARD {}", shardPresentData);
        try {
                log.info("Genesis block not in database, starting from scratch");
                TransactionalDataSource dataSource = databaseManager.getDataSource();
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
                    blockchain.update();
                } catch (SQLException e) {
                    dataSource.rollback();
                    log.info(e.getMessage());
                    throw new RuntimeException(e.toString(), e);
                }
                // set to start work block download thread (starting from Genesis block here)
                log.debug("Before updating BlockchainProcessor from Genesis and RESUME block downloading...");
                blockchainProcessor.resumeBlockchainDownloading(); // IMPORTANT CALL !!!

            } catch (Exception e) {
                log.error(e.toString(), e);
            }
    }

    private void addBlock(TransactionalDataSource dataSource, Block block) {
        try (Connection con = dataSource.getConnection()) {
            blockchain.saveBlock(con, block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }


}
