/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.files.DownloadableFilesManager;
import com.apollocurrency.aplwallet.apl.core.files.shards.ShardPresentData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvImporter;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ChunkedFileOps;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
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
    static ObjectMapper mapper = new ObjectMapper();

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

    public void importShardByFileId(ShardPresentData shardPresentData) {
        importShard(shardPresentData, List.of());
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
            log.debug("Latest competed shard = {} for height = {}", completedShard, height);
            Long shardId = completedShard.getShardId();
            ShardNameHelper nameHelper = new ShardNameHelper();
            String shardFileId = nameHelper.getFullShardId(shardId, blockchainConfig.getChain().getChainId());
            log.debug("Latest competed shard shardFileId = {}", shardFileId);
            ShardPresentData shardPresentData = new ShardPresentData(shardId, shardFileId, List.of());
            importShard(shardPresentData, List.of("block", "transaction"));
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

    public void importShard(ShardPresentData shardPresentData, List<String> excludedTables) {
        Objects.requireNonNull(shardPresentData, "shardPresentData is NULL");
        Objects.requireNonNull(excludedTables, "excludedTables is NULL");
        // shard archive data has been downloaded at that point and stored (unpacked?) in configured folder
        String genesisTaskId = aplAppStatus.durableTaskStart("Shard data import", "Loading Genesis public accounts", true);
        log.debug("genesisTaskId = {}", genesisTaskId);
        log.debug("Received shardPresentData = '{}', lets map all data to Location(s)...", shardPresentData);

        // try to unzip data and throw ShardArchiveProcessingException in any kind of error or zip inconsistency
        Path zipInFolder = unzipMainOptionalArchives(shardPresentData, genesisTaskId);
        if (zipInFolder == null) {
            // ShardArchiveProcessingException was thrown before due to incorrect Zip processing
            return;
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
            ChunkedFileOps ops = new ChunkedFileOps(zipInFolder.toAbsolutePath().toString());
            lastShard.setCoreZipHash(ops.getFileHash());
            log.debug("Update shard info: coreZipHash={}", Convert.toHexString(lastShard.getCoreZipHash()));

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
                if (ShardConstants.ACCOUNT_TABLE_NAME.equalsIgnoreCase(table)
                    || ShardConstants.ACCOUNT_ASSET_TABLE_NAME.equalsIgnoreCase(table)
                    || ShardConstants.ACCOUNT_CURRENCY_TABLE_NAME.equalsIgnoreCase(table)) {
                    rowsImported = csvImporter.importCsvWithDefaultParams(table, 100, true,
                        Map.of("height", blockchain.findFirstBlock().getHeight()));
                } else if (ShardConstants.TAGGED_DATA_TABLE_NAME.equalsIgnoreCase(table)) {
                    rowsImported = csvImporter.importCsvWithRowHook(table, 100, true, (row) -> {
                        Object parsedTags = row.get("parsed_tags");
                        Object height = row.get("height");
                        if (parsedTags != null) {
                            String[] tagArray = new String[0];
                            try {
                                tagArray = mapper.readValue((String) parsedTags, new TypeReference<>() {
                                });
                            } catch (JsonProcessingException e) {
                                log.error("Parsing 'parsed_tags' error during CSV importing", e);
                                throw new RuntimeException(e);
                            }
                            dataTagDao.add(tagArray, Integer.parseInt((String) height));
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
        // remove all extracted *.csv files after successful importing from zip shard archive(s)
        log.debug("Start deleting imported CSV files from folder: {}", csvImporter.getDataExportPath());
        FileUtils.deleteFilesByPattern(csvImporter.getDataExportPath(), new String[]{"csv"}, null);
        aplAppStatus.durableTaskFinished(genesisTaskId, false, "Shard data import");
    }

    private Path unzipMainOptionalArchives(ShardPresentData shardPresentData, String genesisTaskId) {
        Path zipInFolder = downloadableFilesManager.mapFileIdToLocalPath(shardPresentData.getShardFileId()).toAbsolutePath();
        log.debug("Try unpack main shard file name '{}'", zipInFolder);
        boolean unpackResult = zipComponent.extract(zipInFolder.toString(), csvImporter.getDataExportPath().toString(), true);
        log.debug("Main shard Zip is unpacked = {}", unpackResult);
        if (!unpackResult) {
            logErrorAndThrowException(shardPresentData, genesisTaskId, zipInFolder, unpackResult);
            return null;
        }
        // unzip additional files
        if (shardPresentData.getAdditionalFileIDs() != null && shardPresentData.getAdditionalFileIDs().size() > 0) {
            log.debug("Try unpack Optional files(s)=[{}]", shardPresentData.getAdditionalFileIDs().size());
            Path extZipInFolder = null;
            for (String optionalFileId : shardPresentData.getAdditionalFileIDs()) {
                log.debug("Try unpack Optional file by fileId '{}'", optionalFileId);
                extZipInFolder = downloadableFilesManager.mapFileIdToLocalPath(optionalFileId).toAbsolutePath();//!!!! right path was re-wrote
                unpackResult = zipComponent.extract(extZipInFolder.toString(), csvImporter.getDataExportPath().toString(), true);
                log.debug("Zip for '{}' is unpacked = {}", optionalFileId, unpackResult);
                if (!unpackResult) {
                    logErrorAndThrowException(shardPresentData, genesisTaskId, extZipInFolder, unpackResult);

                    return null;
                }
            }
        }
        return zipInFolder;
    }

    private void logErrorAndThrowException(ShardPresentData shardPresentData, String genesisTaskId, Path zipInFolder, boolean unpackResult) {
        log.error("Node has encountered serious error and can't extract/import ZIP data by shard data = " + shardPresentData +
            "\nSomethings wrong with zipped file =\n'{}'\n >>> STOPPING node process....", zipInFolder);
        aplAppStatus.durableTaskFinished(genesisTaskId, true, "Shard data import");
        throw new ShardArchiveProcessingException("Zip file can't be extracted, result = '" + unpackResult + "' : " + zipInFolder.toString());
    }
}
