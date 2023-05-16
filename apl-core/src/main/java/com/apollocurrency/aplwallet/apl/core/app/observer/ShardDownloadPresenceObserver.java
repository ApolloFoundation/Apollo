/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app.observer;

import com.apollocurrency.aplwallet.apl.core.app.GenesisImportException;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardPresentData;
import com.apollocurrency.aplwallet.apl.core.model.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.ShardImporter;
import com.apollocurrency.aplwallet.apl.util.db.DbTransactionHelper;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Interface for shard data downloading management. It does following:
 * In case empty database (node is started from the scratch) it will do:
 * - connect to peers and check if sharding is present in current network
 * - gather statistics info about shard hashes/data from peers
 * <p>
 * If shard is not present it starts 'old' process importing Genesis data
 */
@Slf4j
@Singleton
public class ShardDownloadPresenceObserver {

    private final DatabaseManager databaseManager;
    private final Blockchain blockchain;
    private final BlockchainProcessor blockchainProcessor;
    private final DerivedTablesRegistry derivedTablesRegistry;
    private final ShardImporter shardImporter;
    private final BlockchainConfigUpdater blockchainConfigUpdater;
    private final GenesisImporter genesisImporter;
    private final FullTextSearchService fullTextSearchService;
    private final Event<ShardPresentData> shardImportEvent;

    @Inject
    public ShardDownloadPresenceObserver(DatabaseManager databaseManager, BlockchainProcessor blockchainProcessor,
                                         Blockchain blockchain, DerivedTablesRegistry derivedTablesRegistry,
                                         ShardImporter shardImporter, BlockchainConfigUpdater blockchainConfigUpdater,
                                         GenesisImporter genesisImporter,
                                         FullTextSearchService fullTextSearchService,
                                         Event<ShardPresentData> shardImportEvent) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchainProcessor is NULL");
        this.derivedTablesRegistry = Objects.requireNonNull(derivedTablesRegistry, "derivedTablesRegistry is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.shardImporter = Objects.requireNonNull(shardImporter, "shardImporter is NULL");
        this.blockchainConfigUpdater = Objects.requireNonNull(blockchainConfigUpdater, "blockchainConfigUpdater is NULL");
        this.genesisImporter = Objects.requireNonNull(genesisImporter, "genesisImporter is NULL");
        this.fullTextSearchService = Objects.requireNonNull(fullTextSearchService, "fullTextSearchService is NULL");
        this.shardImportEvent = shardImportEvent;
    }

    /**
     * Method is called when node knows it's int sharded network and shard ZIP information has been downloaded by transport subsystem.
     *
     * @param shardPresentData shard present data contains downloaded ZIP name
     */
    public void onShardPresent(@ObservesAsync @ShardPresentEvent(ShardPresentEventType.SHARD_PRESENT) ShardPresentData shardPresentData) {
        log.debug("Catching fired 'SHARD_PRESENT' event for {}", shardPresentData);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        DbTransactionHelper.executeInTransaction(dataSource, () -> {
            try (Connection con = dataSource.getConnection()) {
                // create Lucene search indexes first
                createLuceneSearchIndexes(con);
                // import data so it gets into search indexes as well
                shardImporter.importShardByFileId(shardPresentData);
                fullTextSearchService.reindexAll(con);
            } catch (Exception e) {
                log.error("Error on Shard IMPORTING # " + shardPresentData, e);
                log.error("Node has encountered serious error and import CSV shard data. " +
                    "Somethings wrong with processing fileId =\n'{}'\n >>> FALL BACK to Genesis importing....", shardPresentData);
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
            shardImportEvent.fire(shardPresentData);
        });
    }

    /**
     * Travers Derived tables and create Lucene search indexes
     *
     * @param con connection should be opened
     * @throws SQLException possible error
     */
    private void createLuceneSearchIndexes(Connection con) throws SQLException {
        for (DerivedTableInterface table : derivedTablesRegistry.getDerivedTables()) {
            // only searchable table will be processed inside createSearchIndex(...)
            fullTextSearchService.createSearchIndex(con, table.getName(), table.getFullTextSearchColumns());
        }
    }

    /**
     * Remove all previously imported data from db
     */
    private void cleanUpPreviouslyImportedData() {
        log.debug("start CleanUp after UNSUCCESSFUL zip import...");
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        DbTransactionHelper.executeInTransaction(dataSource, () -> {
            try {
                blockchain.deleteAll();
                derivedTablesRegistry.getDerivedTables().forEach(DerivedTableInterface::truncate);
                log.debug("Finished CleanUp after UNSUCCESSFUL zip import");
            } catch (Exception e) {
                log.error("Error cleanUp after UNSUCCESSFUL zip import", e);
                log.error("Please delete database files and try to run with command line option : --no-shards-import true");
            }
        });
    }

    /**
     * Process event when no sharding is present in network and we should go 'old scenario'
     *
     * @param shardPresentData not used actually
     */
    public void onNoShardPresent(@Observes @ShardPresentEvent(ShardPresentEventType.NO_SHARD) ShardPresentData shardPresentData) {
        // start adding old Genesis Data
        log.trace("Catch event NO_SHARD {}", shardPresentData);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        log.info("Genesis block not in database, starting from scratch");
        try {
            DbTransactionHelper.executeInTransaction(databaseManager.getDataSource(), () -> {
                try (Connection con = dataSource.getConnection()) {
                    // create first genesis block, but do not save it to db here
                    Block genesisBlock = genesisImporter.newGenesisBlock();
                    long initialBlockId = genesisBlock.getId();
                    log.debug("Generated Genesis block with Id = {}", initialBlockId);
                    // import other genesis data
                    genesisImporter.importGenesisJson(false);
                    // first genesis block should be saved only after all genesis data has been imported before
                    blockchain.saveBlock(genesisBlock);// save first genesis block here
                    // create Lucene search indexes first
                    createLuceneSearchIndexes(con);
                    blockchain.commit(genesisBlock);
                    log.debug("Saved Genesis block = {}", genesisBlock);
                    blockchain.update();
                } catch (SQLException | GenesisImportException e) {
                    log.info(e.getMessage());
                    throw new RuntimeException(e.toString(), e);
                }
                // set to start work block download thread (starting from Genesis block here)
                log.debug("Before updating BlockchainProcessor from Genesis and RESUME block downloading...");
                blockchainProcessor.resumeBlockchainDownloading(); // IMPORTANT CALL !!!
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
}
