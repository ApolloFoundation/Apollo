/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.CSV_EXPORT_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.CSV_EXPORT_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVED_FROM_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVE_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.FAILED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_CREATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_STARTED;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.AbstractHelper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedPaginationOperation;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;

import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@inheritDoc}
 */
@Singleton
public class ShardEngineImpl implements ShardEngine {
    private static final Logger log = getLogger(ShardEngineImpl.class);

    private MigrateState state = MigrateState.INIT;
    private DatabaseManager databaseManager;
    private TrimService trimService;
    private HelperFactory<BatchedPaginationOperation> helperFactory = new HelperFactoryImpl();
    private ShardRecoveryDaoJdbc shardRecoveryDao;
    private CsvExporter csvExporter;
    private DerivedTablesRegistry registry;
    private DirProvider dirProvider;
    private Zip zipComponent;
    private AplAppStatus aplAppStatus;
    private String durableStatusTaskId;


    @Inject
    public ShardEngineImpl(DirProvider dirProvider,
                           DatabaseManager databaseManager,
                           TrimService trimService,
                           ShardRecoveryDaoJdbc shardRecoveryDao,
                           CsvExporter csvExporter,
                           DerivedTablesRegistry registry,
                           Zip zipComponent, AplAppStatus aplAppStatus) {
        this.dirProvider = Objects.requireNonNull(dirProvider, "dirProvider is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(shardRecoveryDao, "shardRecoveryDao is NULL");
        this.csvExporter = Objects.requireNonNull(csvExporter, "csvExporter is NULL");
        this.registry = Objects.requireNonNull(registry, "registry is NULL");
        this.zipComponent = Objects.requireNonNull(zipComponent, "zipComponent is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState getCurrentState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState createBackup() {
        long start = System.currentTimeMillis();
        durableTaskUpdateByState(state, 0.0, "Backup main database...");
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                new ShardDataSourceCreateHelper(databaseManager);
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        String nextShardName = shardDataSourceCreateHelper.createUninitializedDataSource().checkGenerateShardName();
        Path dbDir = dirProvider.getDbDir();
        String backupName = String.format("BACKUP-BEFORE-%s.zip", nextShardName);
        Path backupPath = dbDir.resolve(backupName);
        String sql = String.format("BACKUP TO '%s'", backupPath.toAbsolutePath().toString());
        try (Connection con = sourceDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            if (!Files.exists(backupPath)) {
                state = FAILED;
                durableTaskUpdateByState(state, null, null);
            }
            log.debug("BACKUP by SQL={} was successful, shard = {}", sql, shardDataSourceCreateHelper.getShardId());
            state = MigrateState.MAIN_DB_BACKUPED;
            durableTaskUpdateByState(state, 3.0, "Backed up");
            loadAndRefreshRecovery(sourceDataSource);
        } catch (SQLException e) {
            log.error("ERROR on backup db before sharding, sql = " + sql, e);
            state = FAILED;
            durableTaskUpdateByState(state, null, null);
        }
        log.debug("BACKUP db before shard ({}) in {} sec", state.name(),
                (System.currentTimeMillis() - start)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState addOrCreateShard(DbVersion dbVersion, CommandParamInfo commandParamInfo) {
        long start = System.currentTimeMillis();
        Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        log.debug("INIT shard db file by schema={}", dbVersion.getClass().getSimpleName());
        try {
            boolean isConstraintSchema = dbVersion instanceof ShardAddConstraintsSchemaVersion || dbVersion instanceof AplDbVersion;
            ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(databaseManager.getDataSource());
            if (recovery != null) {
                if (recovery.getState().getValue() >= SHARD_SCHEMA_FULL.getValue()) {
                    // skip to next step
                    return state = SHARD_SCHEMA_FULL;
                }
                if (!isConstraintSchema && recovery.getState().getValue() >= SHARD_SCHEMA_CREATED.getValue()) {
                    return state = SHARD_SCHEMA_CREATED;
                }
            } else {
                if (isConstraintSchema) {
                    throw new IllegalStateException("Unable to apply constrains: Shard db was not created (recovery is null)");
                }
            }

            // we ALWAYS need to do that STEP to attach to new/existing shard db !!
            TransactionalDataSource createdShardSource = ((ShardManagement)databaseManager).createAndAddShard(commandParamInfo.getShardId(), dbVersion);


            if (isConstraintSchema) {
                // that code is called when 'shard index/constraints' sql class is applied to shard db
                state = SHARD_SCHEMA_FULL;
                byte[] shardHash = commandParamInfo.getShardHash();
                if (shardHash != null && shardHash.length > 0) {
                    // update shard record by merkle tree hash value
                    // save prev generator ids to shard
                    // TODO: find better place
                    savePrevBlockData(commandParamInfo.getPrevBlockData(), commandParamInfo.getShardId());

                    // main goal is store merkle tree hash
                    updateShardRecord(commandParamInfo, databaseManager.getDataSource(), state, ShardState.IN_PROGRESS);
                }
                durableTaskUpdateByState(state, 13.0, "Shard is completed");
            } else {
                state = SHARD_SCHEMA_CREATED;
                durableTaskUpdateByState(state, 3.0, "Shard is created");
            }
            TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
            loadAndRefreshRecovery(sourceDataSource);
            log.debug("INIT shard db={} by schema={} ({}) in {} sec",
                    createdShardSource.getDbIdentity(), dbVersion.getClass().getSimpleName(), state.name(),
                    (System.currentTimeMillis() - start)/1000);
        } catch (Exception e) {
            log.error("Error creation Shard Db with Schema script:" + dbVersion.getClass().getSimpleName(), e);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
        }
        return state;
    }



    /**
     * Create new Recovery info record (if it's missing in specified dataSource) or load existing one.
     *
     * @param sourceDataSource usually main db (but can be different)
     */
    private void loadAndRefreshRecovery(TransactionalDataSource sourceDataSource) {
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        log.trace("Latest = {}", recovery);
        if (recovery == null) {
            // store new value or continue to next step
            recovery = new ShardRecovery(state);
            shardRecoveryDao.saveShardRecovery(sourceDataSource, recovery);
        } else {
            if (recovery.getState().getValue() < state.getValue()) {
                recovery.setState(state); // do not change previous state
                recovery.setObjectName(null);
                recovery.setColumnName(null);
                recovery.setProcessedObject(null);
                recovery.setLastColumnValue(null);
                shardRecoveryDao.updateShardRecovery(sourceDataSource, recovery);
            }
        }
    }

    /**
     * Check mandatory parameters
     * @param paramInfo common parameters
     */
    private void checkRequiredParameters(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableNameList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState copyDataToShard(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        log.debug("Starting shard data transfer from [{}] tables...", paramInfo.getTableNameList().size());
        long startAllTables = System.currentTimeMillis();
        String lastTableName = null;

        TransactionalDataSource targetDataSource = ((ShardManagement) databaseManager).getOrCreateShardDataSourceById(paramInfo.getShardId());
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        // check previous state
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() > DATA_COPY_TO_SHARD_FINISHED.getValue()) {
            // skip to next step
            return state = DATA_COPY_TO_SHARD_FINISHED;
        } else { // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(DATA_COPY_TO_SHARD_STARTED);
        }

        // insert data as not processed previously
        Connection targetConnect = null;

        String currentTable = null;
        durableTaskUpdateByState(state, 4.0, "Data copying...");
        try (Connection sourceConnect = sourceDataSource.getConnection() ) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                if (!targetDataSource.isInTransaction()) {
                    targetConnect = targetDataSource.begin();
                }
                currentTable = tableName;
                BatchedPaginationOperation paginationOperationHelper = helperFactory.createSelectInsertHelper(tableName);
                ExcludeInfo excludeInfo = paramInfo.getExcludeInfo();
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(),
                            paramInfo.getShardId(), excludeInfo);

                paginationOperationHelper.setShardRecoveryDao(shardRecoveryDao);// mandatory

                    long totalCount = paginationOperationHelper.processOperation(
                            sourceConnect, targetConnect, operationParams);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                paginationOperationHelper.reset();
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, DATA_COPY_TO_SHARD_STARTED);
                targetDataSource.commit(false);
                incrementDurableTaskUpdateByPercent(1.5);
            }
            state = DATA_COPY_TO_SHARD_FINISHED;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
            durableTaskUpdateByState(state, 7.0, "Data is copied");
        } catch (Exception e) {
            log.error("Error COPY processing table = '" + currentTable + "'", e);
            targetDataSource.rollback(false);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
            return state;
        } finally {
            if (targetConnect != null) {
                targetDataSource.commit();
            }
        }
        log.debug("COPY Processed table(s)=[{}] in {} sec", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    private void processOneTableByHelper(CommandParamInfo paramInfo, Connection sourceConnect,
                                         String tableName, long start,
                                         BatchedPaginationOperation paginationOperationHelper) throws Exception {
        TableOperationParams operationParams = new TableOperationParams(
                tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(),null,  paramInfo.getExcludeInfo());

        if (paginationOperationHelper == null) { // should never happen from outside code, but better to play safe
            String error = "OperationHelper is NOT PRESENT... Fatal error in sharding code...";
            log.error(error);
            throw new IllegalStateException(error);
        }
        paginationOperationHelper.setShardRecoveryDao(shardRecoveryDao); // mandatory assignment

        long totalCount = paginationOperationHelper.processOperation(
                sourceConnect, null, operationParams);

        sourceConnect.commit();
        log.debug("Totally processed '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState updateSecondaryIndex(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting SECONDARY INDEX data update from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() > SECONDARY_INDEX_FINISHED.getValue()) {
            // skip to next step
            return state = SECONDARY_INDEX_FINISHED;
        } else {
            // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(SECONDARY_INDEX_STARTED);
        }
        durableTaskUpdateByState(state, 13.0, "Secondary indexes creation...");
        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;

                BatchedPaginationOperation paginationOperationHelper = helperFactory.createSelectInsertHelper(tableName);
                processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper);
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, SECONDARY_INDEX_STARTED);
                incrementDurableTaskUpdateByPercent(1.8);
            }
            state = SECONDARY_INDEX_FINISHED;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
            durableTaskUpdateByState(state, 17.0, "Secondary indexes created...");
        } catch (Exception e) {
            log.error("Error UPDATE S/Index processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("UPDATE Processed table(s)=[{}] in {} sec", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState exportCsv(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startTime = System.currentTimeMillis();
        List<String> allTables = paramInfo.getTableNameList();
        log.debug("Starting EXPORT data from 'derived tables' + [{}] tables...", allTables.size());

        ShardRecovery recovery = getOrCreateRecovery(CSV_EXPORT_STARTED);
        if (recovery.getState().getValue() >= CSV_EXPORT_FINISHED.getValue()) {
            // skip to next step
            return state = CSV_EXPORT_FINISHED;
        }
        durableTaskUpdateByState(state, 17.0, "CSV exporting...");
        try {
            trimDerivedTables(paramInfo.getSnapshotBlockHeight() + 1);
            if (StringUtils.isBlank(recovery.getProcessedObject())) {
                Files.list(csvExporter.getDataExportPath())
                        .filter(p-> !Files.isDirectory(p) && p.toString().endsWith(CsvAbstractBase.CSV_FILE_EXTENSION))
                        .forEach(FileUtils::deleteFileIfExistsQuietly);
            }
            for (String tableName : allTables) {
                exportTableWithRecovery(recovery, tableName, () -> {
                    switch (tableName.toLowerCase()) {
                        case ShardConstants.SHARD_TABLE_NAME:
                            return csvExporter.exportShardTable(paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
                        case ShardConstants.BLOCK_INDEX_TABLE_NAME:
                            return csvExporter.exportBlockIndex(paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
                        case ShardConstants.TRANSACTION_INDEX_TABLE_NAME:
                            return csvExporter.exportTransactionIndex(paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
                        case ShardConstants.TRANSACTION_TABLE_NAME:
                            return csvExporter.exportTransactions(paramInfo.getExcludeInfo().getExportDbIds());
                        case ShardConstants.BLOCK_TABLE_NAME:
                            return csvExporter.exportBlock(paramInfo.getSnapshotBlockHeight());
                        case ShardConstants.ACCOUNT_TABLE_NAME:
                            return exportDerivedTable(tableName, paramInfo, Set.of("DB_ID","LATEST","HEIGHT"), "id");
                        default:
                            return exportDerivedTable(tableName, paramInfo);
                    }
                });
                incrementDurableTaskUpdateByPercent(0.7);
            }
            state = CSV_EXPORT_FINISHED;
            updateToFinalStepState(recovery, state);
            log.debug("Export finished in {} secs", (System.currentTimeMillis() - startTime)/1000);
            durableTaskUpdateByState(state, 58.0, "CSV exported");
        } catch (Exception e) {
            log.error("Exception during export", e);
            state = FAILED;
            durableTaskUpdateByState(state, null, null);
        }
        return state;
    }

    private void trimDerivedTables(int height) {
        databaseManager.getDataSource().begin();
        try {
            trimService.doTrimDerivedTablesOnHeight(height);
        } catch (Exception e) {
            databaseManager.getDataSource().rollback(false);
        } finally {
            databaseManager.getDataSource().commit();
        }
    }

    private ShardRecovery getOrCreateRecovery(MigrateState commandStartState) {
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery == null) {
            // init new recovery
            recovery = new ShardRecovery(commandStartState);
            long key = shardRecoveryDao.saveShardRecovery(databaseManager.getDataSource(), recovery);
            recovery.setShardRecoveryId(key);
        }
        return recovery;
    }

    private Long exportDerivedTable(String tableName, CommandParamInfo paramInfo, Set<String> excludedColumns, String sort) {
        DerivedTableInterface derivedTable = registry.getDerivedTable(tableName);
        if (derivedTable != null) {
            if (excludedColumns == null && sort == null) {
                return csvExporter.exportDerivedTable(derivedTable, paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
            } else {
                return csvExporter.exportDerivedTableCustomSort(derivedTable, paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize(), excludedColumns, sort);
            }
        } else {
            durableTaskUpdateByState(FAILED, null, null);
            throw new IllegalArgumentException("Unable to find derived table " + tableName + " in derived table registry");
        }
    }

    private Long exportDerivedTable(String tableName, CommandParamInfo paramInfo) {
        return exportDerivedTable(tableName, paramInfo, null, null);
    }

    private void exportTableWithRecovery(ShardRecovery recovery, String tableName, Supplier<Long> exportPerformer) {
        if (AbstractHelper.isContain(recovery.getProcessedObject(), tableName)) {
            log.debug("Skip already exported table: " + tableName);
        } else {
            Path tableCsvPath = csvExporter.getDataExportPath().resolve(tableName + CsvAbstractBase.CSV_FILE_EXTENSION);
            log.trace("Exporting '{}'...", tableCsvPath);
            FileUtils.deleteFileIfExistsAndHandleException(tableCsvPath, (e)-> {
                durableTaskUpdateByState(state, null, null);
                throw new RuntimeException("Unable to remove not finished csv file: " + tableCsvPath.toAbsolutePath().toString());
            });
            long startTableExportTime = System.currentTimeMillis();
            Long exported = exportPerformer.get();
            log.debug("Exported - {}, from {} to {} in {} secs", exported, tableName, tableCsvPath,
                    (System.currentTimeMillis() - startTableExportTime)/1000);

            if (StringUtils.isBlank(recovery.getProcessedObject())) {
                recovery.setProcessedObject(tableName);
            } else {
                recovery.setProcessedObject(recovery.getProcessedObject() + "," + tableName);
            }
            shardRecoveryDao.updateShardRecovery(databaseManager.getDataSource(), recovery);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState archiveCsv(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo);
        long startAllTables = System.currentTimeMillis();

        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() >= ZIP_ARCHIVE_FINISHED.getValue()) {
            // skip to next step
            return state = ZIP_ARCHIVE_FINISHED;
        }

        UUID chainId = databaseManager.getChainId();
        String shardFileName = new ShardNameHelper().getShardArchiveNameByShardId(paramInfo.getShardId(),chainId);
        String currentTable = shardFileName;

        Path shardZipFilePath = dirProvider.getDataExportDir().resolve(shardFileName);
        log.debug("Zip file name = '{}' will be searched/stored in '{}'", shardFileName, shardZipFilePath);
        // delete if something left in previous run
        boolean isRemoved = FileUtils.deleteFileIfExistsAndHandleException(shardZipFilePath, (e) -> {
            durableTaskUpdateByState(FAILED, null, null);
            throw new RuntimeException("Unable to remove previous ZIP file: " + shardZipFilePath.toAbsolutePath().toString());
        });
        log.debug("Previous Zip in '{}' was '{}'", shardFileName, isRemoved ? "REMOVED" : "NOT FOUND");

//        try (Connection sourceConnect = sourceDataSource.begin()) {
        try (Connection sourceConnect = sourceDataSource.getConnection()) {
            state = ZIP_ARCHIVE_STARTED;
            updateShardRecoveryProcessedTableList(sourceConnect, shardFileName, state);
            durableTaskUpdateByState(state, 58.0, "CSV archiving...");
            // compute ZIP crc hash
            FilenameFilter CSV_FILE_FILTER = new SuffixFileFilter(".csv"); // CSV files only
            byte[] zipCrcHash = zipComponent.compressAndHash(
                    shardZipFilePath.toAbsolutePath().toString(),
                    
                    dirProvider.getDataExportDir().toAbsolutePath().toString(), null, CSV_FILE_FILTER, false);

            // prepare real CRC data for shard record update
            paramInfo = CommandParamInfo.builder()
                    .shardHash(zipCrcHash)
                    .isZipCrcStored(true)
                    .shardId(paramInfo.getShardId())
                    .build();
            updateShardRecord(paramInfo, sourceDataSource, state, ShardState.IN_PROGRESS); //update shard record by ZIP crc value

            // update recovery
            state = ZIP_ARCHIVE_FINISHED;
            updateToFinalStepState(recovery, state);
            durableTaskUpdateByState(state, 58.5, "CSV archived");
        } catch (Exception e) {
            log.error("Error ZIP ARCHIVE creation = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
            return state;
        }
        log.debug("ZIP ARCHIVE Processed in {} sec", (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState deleteCopiedData(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting Deleting data from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() > DATA_REMOVED_FROM_MAIN.getValue()) {
            // skip to next step
            return state = DATA_REMOVED_FROM_MAIN;
        } else {
            // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(DATA_REMOVE_STARTED);
        }
        durableTaskUpdateByState(state, 59.0, "Data deleting...");
        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;

                BatchedPaginationOperation paginationOperationHelper = helperFactory.createDeleteHelper(tableName);
                processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper);
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, DATA_REMOVE_STARTED);
                incrementDurableTaskUpdateByPercent(18.0);
            }
            state = DATA_REMOVED_FROM_MAIN;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
            durableTaskUpdateByState(state, 95.0, "Data deleted");
        } catch (Exception e) {
            log.error("Error DELETE processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("DELETE Processed table(s)=[{}] in {} sec", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState finishShardProcess(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting create SHARD record in main db...");

        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() >= COMPLETED.getValue()) {
            // skip to next step
            return state = COMPLETED;
        }
        state = COMPLETED;
        // complete sharding
        updateShardRecord(paramInfo, sourceDataSource, state, ShardState.FULL);
        // call ANALYZE to optimize main db performance after massive copy/delete/update actions in it
        sourceDataSource.analyzeTables();
        log.debug("Shard record '{}' is created with Hash in {} ms", paramInfo.getShardId(),
                System.currentTimeMillis() - startAllTables);
        durableTaskUpdateByState(state, null, null);
        return state;
    }

    private void savePrevBlockData(PrevBlockData prevBlockData, long shardId) {
        try(Connection con = databaseManager.getDataSource().getConnection();
        PreparedStatement pstmt = con.prepareStatement("UPDATE shard SET generator_ids = ?, block_timestamps = ?, block_timeouts = ? WHERE shard_id = ?")) {
            DbUtils.setArray(pstmt, 1, prevBlockData.getGeneratorIds());
            DbUtils.setArray(pstmt, 2, prevBlockData.getPrevBlockTimestamps());
            DbUtils.setArray(pstmt, 3, prevBlockData.getPrevBlockTimeouts());
            pstmt.setLong(4, shardId);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean updateShardRecord(CommandParamInfo paramInfo,
                                      TransactionalDataSource sourceDataSource,
                                      MigrateState recoveryStateUpdateInto,
                                      ShardState shardState) {
        ShardRecovery recovery;
        String sqlUpdate = null;
        // we want update SHARD either 'merkle tree hash' or 'zip CRC'
        if (paramInfo.getShardHash() != null) {
            if (!paramInfo.isZipCrcStored()) {
                // merkle tree hash
                sqlUpdate = "UPDATE SHARD SET SHARD_HASH = ?, SHARD_STATE = ? WHERE SHARD_ID = ?";
            } else {
                // zip crc hash
                sqlUpdate = "UPDATE SHARD SET ZIP_HASH_CRC = ?, SHARD_STATE = ? WHERE SHARD_ID = ?";
            }
        }
        if (sqlUpdate == null) {
            // update only state
            sqlUpdate = "UPDATE SHARD SET SHARD_STATE = ? WHERE SHARD_ID = ?";
        }
//        try (Connection sourceConnect = sourceDataSource.begin();
        try (Connection sourceConnect = sourceDataSource.getConnection();
             PreparedStatement preparedInsertStatement = sourceConnect.prepareStatement(sqlUpdate)) {
            int result = 0;
            // skip updating SHARD record on latest step
            // assign either 'merkle tree hash' OR 'zip CRC'
            int i = 1;
            if (paramInfo.getShardHash() != null) {
                preparedInsertStatement.setBytes(i++, paramInfo.getShardHash()); // merkle or zip crc}
            }
            preparedInsertStatement.setLong(i++, shardState.getValue()); // 100% full shard is present on current node
            preparedInsertStatement.setLong(i, paramInfo.getShardId());
            result = preparedInsertStatement.executeUpdate();
            log.debug("Shard record is updated result = '{}'", result);
            if (recoveryStateUpdateInto == COMPLETED) {
                // remove recovery data when process is completed
                recovery = shardRecoveryDao.getLatestShardRecovery(sourceConnect);
                result = shardRecoveryDao.hardDeleteShardRecovery(sourceConnect, recovery.getShardRecoveryId());
                log.debug("Shard Recovery is deleted = '{}'", result);
            }
            sourceConnect.commit();
        }
        catch (Exception e) {
            log.error("Error creating Shard record in main db", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return true;
        }
        finally {
/*
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
*/
        }
        return false;
    }

    /**
     * Update previous info with table name just processed in loop
     * @param currentTable table name to be put into table
     * @param inProgressState progress state value
     * @return updated and stored instance
     */
    private ShardRecovery updateShardRecoveryProcessedTableList(
            Connection connection, String currentTable, MigrateState inProgressState) {
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(connection);
        // add processed table name into optional column for later use
        recovery.setState(inProgressState);
        // we want :
        // - add new table name into empty column
        // - preserve column value if the same table name exist inside
        // - add table name if it not exists in list separated by space
        recovery.setProcessedObject(recovery.getProcessedObject() != null ?
                (!AbstractHelper.isContain(recovery.getProcessedObject(), currentTable) ?
                        recovery.getProcessedObject() + " " + currentTable.toLowerCase() : recovery.getProcessedObject())
                : currentTable.toLowerCase()); // add processed table name into list if NOT exists
        shardRecoveryDao.updateShardRecovery(connection, recovery); // update info for next step
        return recovery;
    }

    /**
     * Store final step state, when all tables were processed
     * @param recovery current recovery
     * @param finalStepState final state
     */
    private void updateToFinalStepState(Connection connection, ShardRecovery recovery, MigrateState finalStepState) {
       resetShardRecovery(recovery, finalStepState);
        shardRecoveryDao.updateShardRecovery(connection, recovery); // update info for next step
    }

    private void updateToFinalStepState(ShardRecovery recovery, MigrateState finalStepState) {
        resetShardRecovery(recovery, finalStepState);
        shardRecoveryDao.updateShardRecovery(databaseManager.getDataSource(), recovery); // update info for next step
    }

    private void resetShardRecovery(ShardRecovery recovery, MigrateState migrateState) {
        recovery.setState(migrateState);
        recovery.setObjectName(null); // remove currently processed table
        recovery.setColumnName(null); // main column used for pagination on data in table
        recovery.setLastColumnValue(null); // latest processed column value
        recovery.setProcessedObject(null); // list of previously processed table names within one step
    }


    private void durableTaskUpdateByState(MigrateState state, Double percentComplete, String message) {
        checkOrInitAppStatus();
        switch (state) {
            case FAILED:
                aplAppStatus.durableTaskFinished(durableStatusTaskId, true, "Sharding process has " + state.name());
                break;
            case COMPLETED:
                aplAppStatus.durableTaskFinished(durableStatusTaskId, false, "Sharding process completed successfully !");
                log.info("Sharding process COMPLETED successfully !");
                break;
            default:
                aplAppStatus.durableTaskUpdate(durableStatusTaskId, percentComplete, message);
        }
    }

    private void incrementDurableTaskUpdateByPercent(Double percentIncreaseValue) {
        checkOrInitAppStatus();
        aplAppStatus.durableTaskUpdateAddPercents(durableStatusTaskId, percentIncreaseValue);
    }

    private void checkOrInitAppStatus() {
        if (durableStatusTaskId == null) {
            Optional<DurableTaskInfo> taskInfo = aplAppStatus.findTaskByName("sharding");
            if (taskInfo.isEmpty()) {
                durableStatusTaskId = aplAppStatus.durableTaskStart(
                        "sharding",
                        "Blockchain db sharding process takes some time, pls be patient...",
                        true, 0.0);
            } else {
                durableStatusTaskId = taskInfo.get().getId();
            }
        } else {
            aplAppStatus.findTaskByName("sharding");
        }
    }

}
