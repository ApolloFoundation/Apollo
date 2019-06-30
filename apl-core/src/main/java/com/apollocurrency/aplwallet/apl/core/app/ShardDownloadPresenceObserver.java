/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEvent;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.ShardPresentEventType;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.core.shard.ShardPresentData;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporter;
import com.apollocurrency.aplwallet.apl.util.Zip;
import lombok.extern.slf4j.Slf4j;

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

    @Inject
    public ShardDownloadPresenceObserver(DatabaseManager databaseManager, BlockchainProcessor blockchainProcessor,
                                         Blockchain blockchain, DerivedTablesRegistry derivedTablesRegistry,
                                         Zip zipComponent, CsvImporter csvImporter,
                                         DownloadableFilesManager downloadableFilesManager,
                                         AplAppStatus aplAppStatus) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.blockchainProcessor = Objects.requireNonNull(blockchainProcessor, "blockchainProcessor is NULL");
        this.derivedTablesRegistry = Objects.requireNonNull(derivedTablesRegistry, "derivedTablesRegistry is NULL");
        this.blockchain = Objects.requireNonNull(blockchain, "blockchain is NULL");
        this.zipComponent = Objects.requireNonNull(zipComponent, "zipComponent is NULL");
        this.csvImporter = Objects.requireNonNull(csvImporter, "csvImporter is NULL");
        this.downloadableFilesManager = Objects.requireNonNull(downloadableFilesManager, "downloadableFilesManager is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
    }

    /**
     * Method is called when node knows it's int sharded network and shard ZIP information has been downloaded by transport subsystem.
     *
     * @param shardPresentData shard present data contains downloaded ZIP name
     */
    public void onShardPresent(@ObservesAsync @ShardPresentEvent(ShardPresentEventType.SHARD_PRESENT) ShardPresentData shardPresentData) {
        // shard archive data has been downloaded at that point and stored (unpacked?) in configured folder
        String genesisTaskId = aplAppStatus.durableTaskStart("Shard data import", "Loading Genesis public accounts",true);
        log.debug("genesisTaskId = {}", genesisTaskId);
        String shardFileId = shardPresentData.getFileIdValue(); // "shard::" + shardPresentData.getFileIdValue() + ";chainid::3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6";
        log.debug("Received shardFileId = '{}', lets map it to Location...", shardFileId);
        Path zipInFolder = downloadableFilesManager.mapFileIdToLocalPath(shardFileId).toAbsolutePath();
        log.debug("Try unpack file name '{}'", zipInFolder);
        boolean unpackResult = zipComponent.extract(zipInFolder.toString(), csvImporter.getDataExportPath().toString());
        log.debug("Zip is unpacked = {}", unpackResult);

        Genesis.apply(true); // import genesis public Keys ONLY (NO balances) - 049,842%
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50.0, "Public keys were imported");
        // import additional tables
        List<String> tables = List.of(ShardConstants.SHARD_TABLE_NAME,
                ShardConstants.BLOCK_TABLE_NAME, ShardConstants.TRANSACTION_TABLE_NAME,
                ShardConstants.TRANSACTION_INDEX_TABLE_NAME, ShardConstants.BLOCK_INDEX_TABLE_NAME);
        log.debug("1. Will be imported [{}] tables...", tables.size());
        for (String table : tables) {
            try {
                log.debug("start importing '{}'...", table);
                aplAppStatus.durableTaskUpdate(genesisTaskId, "Loading '" + table + "'", 0.6);
                long rowsImported = csvImporter.importCsv(table, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, true);
                log.debug("Imported '{}' rows = {}", table, rowsImported);
            } catch (Exception e) {
                log.error("CSV import error for '{}', RETURN.......", table, e);
                return;
            }
        }
        // import derived tables
        Collection<String> tableNames = derivedTablesRegistry.getDerivedTables().stream().map(Object::toString).collect(Collectors.toList());
        log.debug("2. Will be imported [{}] tables...", tables.size());
        for (String table : tableNames) {
            try {
                log.debug("start importing '{}'...", table);
                aplAppStatus.durableTaskUpdate(genesisTaskId, "Loading '" + table + "'", 0.6);
                long rowsImported = csvImporter.importCsv(table, 100, true);
                log.debug("Imported '{}' rows = {}", table, rowsImported);
            } catch (Exception e) {
                log.error("CSV import error for '{}', RETURN.......", table, e);
                return;
            }
        }
        // set to start work block download thread (starting from shard's snapshot block here)
        log.debug("Before updating BlockchainProcessor from Shard data and RESUME block downloading...");
        blockchainProcessor.updateInitialSnapshotBlock();
        blockchainProcessor.resumeBlockchainDownloading(); // IMPORTANT CALL !!!
        aplAppStatus.durableTaskFinished(genesisTaskId, false, "Shard data import");
    }

    /**
     * Process event when no sharding is present in network and we should go 'old scenario'
     *
     * @param shardPresentData not used actually
     */
    public void onNoShardPresent(@ObservesAsync @ShardPresentEvent(ShardPresentEventType.NO_SHARD) ShardPresentData shardPresentData) {
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
            blockchainProcessor.updateInitialBlockId();
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
