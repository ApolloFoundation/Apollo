/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.peer.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporter;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.util.Zip;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
@Slf4j
public class ShardImporter {
    private ShardDao shardDao;

    private GenesisImporter genesisImporter;
    private Blockchain blockchain;
    private DerivedTablesRegistry derivedTablesRegistry;
    private BlockchainConfig blockchainConfig;
    private DataTagDao dataTagDao;
    private CsvImporter csvImporter;
    private Zip zipComponent;
    private DownloadableFilesManager downloadableFilesManager;
    private AplAppStatus aplAppStatus;

    @Inject
    public ShardImporter(ShardDao shardDao, BlockchainConfig blockchainConfig, GenesisImporter genesisImporter, Blockchain blockchain, DerivedTablesRegistry derivedTablesRegistry, CsvImporter csvImporter, Zip zipComponent, DataTagDao dataTagDao, DownloadableFilesManager downloadableFilesManager, AplAppStatus aplAppStatus) {
        this.shardDao = shardDao;
        this.genesisImporter = genesisImporter;
        this.blockchain = blockchain;
        this.derivedTablesRegistry = derivedTablesRegistry;
        this.csvImporter = csvImporter;
        this.zipComponent = zipComponent;
        this.downloadableFilesManager = downloadableFilesManager;
        this.dataTagDao = dataTagDao;
        this.aplAppStatus = aplAppStatus;
        this.blockchainConfig = blockchainConfig;

    }

    public void importShardByFileId(String fileId) {
        importShard(fileId, List.of());
        // set to start work block download thread (starting from shard's snapshot block here)
        log.debug("Before updating BlockchainProcessor from Shard data and RESUME block downloading...");
        BlockchainProcessor blockchainProcessor = CDI.current().select(BlockchainProcessor.class).get(); // prevent circular dependency, should be fixed later
        blockchain.update();
        blockchainProcessor.resumeBlockchainDownloading(); // IMPORTANT CALL !!!
    }

    public void importLastShard(int height) {
        if (height == 0) {
            genesisImporter.importGenesisJson(false);
        } else {
            Shard completedShard = shardDao.getLastCompletedOrArchivedShard();
            Long shardId = completedShard.getShardId();
            ShardNameHelper nameHelper = new ShardNameHelper();
            String fileId = nameHelper.getFullShardId(shardId, blockchainConfig.getChain().getChainId());
            importShard(fileId, List.of("block", "transaction"));
        }
    }

    public boolean canImport(int height) {
        if (height == 0) {
            return true;
        } else if (blockchainConfig.getCurrentConfig().isShardingEnabled()) {
            Shard shard = shardDao.getLastShard();
            if (shard != null) {
                return shard.getShardState() == ShardState.FULL || shard.getShardState() == ShardState.CREATED_BY_ARCHIVE;
            } else {
                return true;
            }
        }
        return true;
    }

    protected void importGenesis(boolean onlyKeys) {
        genesisImporter.importGenesisJson(onlyKeys);
    }

    public void importShard(String fileId, List<String> excludedTables) {
        Objects.requireNonNull(fileId, "fileId is NULL");
        Objects.requireNonNull(excludedTables, "excludedTables is NULL");
        // shard archive data has been downloaded at that point and stored (unpacked?) in configured folder
        String genesisTaskId = aplAppStatus.durableTaskStart("Shard data import", "Loading Genesis public accounts", true);
        log.debug("genesisTaskId = {}", genesisTaskId);
        log.debug("Received shardFileId = '{}', lets map it to Location...", fileId);
        Path zipInFolder = downloadableFilesManager.mapFileIdToLocalPath(fileId).toAbsolutePath();
        log.debug("Try unpack file name '{}'", zipInFolder);
        boolean unpackResult = zipComponent.extract(zipInFolder.toString(), csvImporter.getDataExportPath().toString());
        log.debug("Zip is unpacked = {}", unpackResult);
        if (!unpackResult) {
            log.error("Node has encountered serious error and can't import ZIP with shard data. " +
                    "Somethings wrong with zipped file =\n'{}'\n >>> STOPPING node process....", zipInFolder);
            aplAppStatus.durableTaskFinished(genesisTaskId, true, "Shard data import");
            throw new ShardArchiveProcessingException("Zip file can't be extracted, result = '" + unpackResult + "' : " + zipInFolder.toString());
        }

        genesisImporter.importGenesisJson(true); // import genesis public Keys ONLY (NO balances) - 049,842%
        aplAppStatus.durableTaskUpdate(genesisTaskId, 50.0, "Public keys were imported");
        // import additional tables
        List<String> tables = List.of(ShardConstants.SHARD_TABLE_NAME,
                ShardConstants.BLOCK_TABLE_NAME, ShardConstants.TRANSACTION_TABLE_NAME,
                ShardConstants.TRANSACTION_INDEX_TABLE_NAME, ShardConstants.BLOCK_INDEX_TABLE_NAME);
        log.debug("1. Will be imported [{}] tables...", tables.size());
        for (String table : tables) {
            if (excludedTables.contains(table)) {
                log.warn("Skip import {}", table);
                continue;
            }
            try {
                log.debug("start importing '{}'...", table);
                aplAppStatus.durableTaskUpdate(genesisTaskId, "Loading '" + table + "'", 0.6);
                long rowsImported = csvImporter.importCsv(table, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, true);
                log.debug("Imported '{}' rows = {}", table, rowsImported);
            } catch (Exception e) {
                log.error("CSV import error for '{}', RETURN.......", table, e);
                aplAppStatus.durableTaskFinished(genesisTaskId, true, "Shard data import");
                throw new RuntimeException(e);
            }
        }
        Shard lastShard = shardDao.getLastShard();
        if (lastShard == null) {
            if (!excludedTables.contains(ShardConstants.SHARD_TABLE_NAME)) {
                aplAppStatus.durableTaskFinished(genesisTaskId, true, "Shard data import");
                throw new IllegalStateException("Unable to import shard without records in shard table");
            }
        } else {
            lastShard.setShardState(ShardState.CREATED_BY_ARCHIVE);
            lastShard.setCoreZipHash(zipComponent.calculateHash(zipInFolder.toAbsolutePath().toString()));
            shardDao.updateShard(lastShard);
        }


        // import derived tables
        Collection<String> tableNames = derivedTablesRegistry.getDerivedTableNames();
        log.debug("2. Will be imported [{}] tables...", tables.size());
        for (String table : tableNames) {
            try {
                log.debug("start importing '{}'...", table);
                aplAppStatus.durableTaskUpdate(genesisTaskId, "Loading '" + table + "'", 0.6);
                long rowsImported;
                if (ShardConstants.ACCOUNT_TABLE_NAME.equalsIgnoreCase(table)) {
                    rowsImported = csvImporter.importCsvWithDefaultParams(table, 100, true,
                            Map.of("height", blockchain.findFirstBlock().getHeight()));
                } else if (ShardConstants.TAGGED_DATA_TABLE_NAME.equalsIgnoreCase(table)) {
                    rowsImported = csvImporter.importCsvWithRowHook(table, 100, true, (row)-> {
                        Object parsedTags = row.get("parsed_tags");
                        Object height = row.get("height");
                        if (parsedTags != null) {
                            Object[] tagArray = (Object[]) parsedTags;
                            dataTagDao.add(Arrays.copyOf(tagArray, tagArray.length, String[].class), Integer.parseInt((String) height));
                        }
                    });
                } else {
                    rowsImported = csvImporter.importCsv(table, 100, true);
                }
                log.debug("Imported '{}' rows = {}", table, rowsImported);
            } catch (Exception e) {
                log.error("CSV import error for '{}', RETURN.......", table, e);
                aplAppStatus.durableTaskFinished(genesisTaskId, true, "Shard data import");
                throw new RuntimeException(e);
            }
        }
        aplAppStatus.durableTaskFinished(genesisTaskId, false, "Shard data import");
    }
}
