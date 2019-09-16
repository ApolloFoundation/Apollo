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
import static com.apollocurrency.aplwallet.apl.core.shard.ShardConstants.DB_BACKUP_FORMAT;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.api.dto.DurableTaskInfo;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardRecoveryDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.AbstractHelper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedPaginationOperation;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.core.shard.model.ExcludeInfo;
import com.apollocurrency.aplwallet.apl.core.shard.model.PrevBlockData;
import com.apollocurrency.aplwallet.apl.core.shard.model.TableInfo;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.Zip;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
    private ShardRecoveryDaoJdbc shardRecoveryDaoJdbc;
    private ShardRecoveryDao shardRecoveryDao;
    private CsvExporter csvExporter;
    private DerivedTablesRegistry registry;
    private DirProvider dirProvider;
    private ShardDao shardDao;
    private Zip zipComponent;
    private AplAppStatus aplAppStatus;
    private String durableStatusTaskId;


    @Inject
    public ShardEngineImpl(DirProvider dirProvider,
                           DatabaseManager databaseManager,
                           TrimService trimService,
                           ShardRecoveryDaoJdbc shardRecoveryDaoJdbc,
                           CsvExporter csvExporter,
                           DerivedTablesRegistry registry,
                           ShardRecoveryDao shardRecoveryDao,
                           ShardDao shardDao,
                           Zip zipComponent, AplAppStatus aplAppStatus) {
        this.dirProvider = Objects.requireNonNull(dirProvider, "dirProvider is NULL");
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.shardRecoveryDaoJdbc = Objects.requireNonNull(shardRecoveryDaoJdbc, "shardRecoveryDaoJdbc is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(shardRecoveryDao, "shardRecoveryDao is NULL");
        this.csvExporter = Objects.requireNonNull(csvExporter, "csvExporter is NULL");
        this.registry = Objects.requireNonNull(registry, "registry is NULL");
        this.zipComponent = Objects.requireNonNull(zipComponent, "zipComponent is NULL");
        this.aplAppStatus = Objects.requireNonNull(aplAppStatus, "aplAppStatus is NULL");
        this.shardDao = Objects.requireNonNull(shardDao, "shardDao is NULL");
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
        String backupName = String.format(DB_BACKUP_FORMAT, nextShardName);
        Path backupPath = dbDir.resolve(backupName);
        String sql = String.format("BACKUP TO '%s'", backupPath.toAbsolutePath().toString());
        try (Connection con = sourceDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
            if (!Files.exists(backupPath)) {
                state = FAILED;
                log.error("BACKUP main db has FAILED, SQL={}, shard = {}, backup was not found in path = {}",
                        sql, shardDataSourceCreateHelper.getShardId(), backupPath);
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
            ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(databaseManager.getDataSource());
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
            TransactionalDataSource createdShardSource = ((ShardManagement)databaseManager).createOrUpdateShard(commandParamInfo.getShardId(), dbVersion);


            if (isConstraintSchema) {
                // that code is called when 'shard index/constraints' sql class is applied to shard db
                state = SHARD_SCHEMA_FULL;
                byte[] shardHash = commandParamInfo.getShardHash();
                if (shardHash != null && shardHash.length > 0) {
                    // update shard record by merkle tree hash value
                    // save prev generator ids to shard
                    // TODO: find better place
                    Shard shard = requireLastNotFinishedShard();
                    PrevBlockData prevBlockData = commandParamInfo.getPrevBlockData();
                    shard.setShardHash(shardHash);
                    shard.setBlockTimeouts(Convert.toArrayInt(prevBlockData.getPrevBlockTimeouts()));
                    shard.setBlockTimestamps(Convert.toArrayInt(prevBlockData.getPrevBlockTimestamps()));
                    shard.setGeneratorIds(Convert.toArray(prevBlockData.getGeneratorIds()));
                    shard.setShardState(ShardState.IN_PROGRESS);
                    shardDao.updateShard(shard);
                }
                durableTaskUpdateByState(state, 13.0, "Shard schema is completed");
            } else {
                state = SHARD_SCHEMA_CREATED;
                durableTaskUpdateByState(state, 3.0, "Shard schema is created");
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

    private Shard requireLastNotFinishedShard() {
        Shard lastShard = shardDao.getLastShard();
        if (lastShard == null || lastShard.getShardState() == ShardState.FULL) {
            throw new IllegalStateException("Last shard should be in progress, but got " + lastShard);
        }
        return lastShard;
    }



    /**
     * Create new Recovery info record (if it's missing in specified dataSource) or load existing one.
     *
     * @param sourceDataSource usually main db (but can be different)
     */
    private void loadAndRefreshRecovery(TransactionalDataSource sourceDataSource) {
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(sourceDataSource);
        log.trace("Latest = {}", recovery);
        if (recovery == null) {
            // store new value or continue to next step
            recovery = new ShardRecovery(state);
            shardRecoveryDaoJdbc.saveShardRecovery(sourceDataSource, recovery);
        } else {
            if (recovery.getState().getValue() < state.getValue()) {
                recovery.setState(state); // do not change previous state
                recovery.setObjectName(null);
                recovery.setColumnName(null);
                recovery.setProcessedObject(null);
                recovery.setLastColumnValue(null);
                shardRecoveryDaoJdbc.updateShardRecovery(sourceDataSource, recovery);
            }
        }
    }

    /**
     * Check mandatory parameters
     * @param paramInfo common parameters
     */
    private void checkRequiredParameters(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableInfoList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState copyDataToShard(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        log.debug("Starting shard data transfer from [{}] tables...", paramInfo.getTableInfoList().size());
        long startAllTables = System.currentTimeMillis();
        String lastTableName = null;

        TransactionalDataSource targetDataSource = ((ShardManagement) databaseManager).getOrCreateShardDataSourceById(paramInfo.getShardId());
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        // check previous state
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(sourceDataSource);
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
            for (TableInfo tableInfo : paramInfo.getTableInfoList()) {
                long start = System.currentTimeMillis();
                if (!targetDataSource.isInTransaction()) {
                    targetConnect = targetDataSource.begin();
                }
                currentTable = tableInfo.getName();
                BatchedPaginationOperation paginationOperationHelper = helperFactory.createSelectInsertHelper(currentTable);
                ExcludeInfo excludeInfo = paramInfo.getExcludeInfo();
                    TableOperationParams operationParams = new TableOperationParams(
                            currentTable, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(),
                            paramInfo.getShardId(), excludeInfo);

                paginationOperationHelper.setShardRecoveryDao(shardRecoveryDaoJdbc);// mandatory

                    long totalCount = paginationOperationHelper.processOperation(
                            sourceConnect, targetConnect, operationParams);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, currentTable, (System.currentTimeMillis() - start)/1000);
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
        log.debug("COPY Processed table(s)=[{}] in {} sec", paramInfo.getTableInfoList().size(), (System.currentTimeMillis() - startAllTables)/1000);
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
        paginationOperationHelper.setShardRecoveryDao(shardRecoveryDaoJdbc); // mandatory assignment

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
        log.debug("Starting SECONDARY INDEX data update from [{}] tables...", paramInfo.getTableInfoList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(sourceDataSource);
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
            for (TableInfo tableInfo : paramInfo.getTableInfoList()) {
                long start = System.currentTimeMillis();
                currentTable = tableInfo.getName();

                BatchedPaginationOperation paginationOperationHelper = helperFactory.createSelectInsertHelper(currentTable);
                processOneTableByHelper(paramInfo, sourceConnect, currentTable, start, paginationOperationHelper);
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
        log.debug("UPDATE Processed table(s)=[{}] in {} sec", paramInfo.getTableInfoList().size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState exportCsv(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startTime = System.currentTimeMillis();
        List<TableInfo> allTables = paramInfo.getTableInfoList();
        log.debug("Starting EXPORT data from 'derived tables' + [{}] tables...", allTables.size());

        ShardRecovery recovery = getOrCreateRecovery(CSV_EXPORT_STARTED);
        if (recovery.getState().getValue() >= CSV_EXPORT_FINISHED.getValue()) {
            // skip to next step
            return state = CSV_EXPORT_FINISHED;
        }
        state = CSV_EXPORT_STARTED;
        durableTaskUpdateByState(state, 17.0, "CSV exporting...");
        try {
            int pruningTime = trimDerivedTables(paramInfo.getSnapshotBlockHeight() + 1);
            if (StringUtils.isBlank(recovery.getProcessedObject())) {
                Files.list(csvExporter.getDataExportPath())
                        .filter(p-> !Files.isDirectory(p) && p.toString().endsWith(CsvAbstractBase.CSV_FILE_EXTENSION))
                        .forEach(FileUtils::deleteFileIfExistsQuietly);
            }
            for (TableInfo tableInfo : allTables) {
                exportTableWithRecovery(recovery, tableInfo.getName(), () -> {
                    switch (tableInfo.getName()) {
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
                            return exportDerivedTable(tableInfo, paramInfo, Set.of("DB_ID","LATEST","HEIGHT"), pruningTime);
                        default:
                            return exportDerivedTable(tableInfo, paramInfo, pruningTime);
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

    private int trimDerivedTables(int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        boolean inTransaction = dataSource.isInTransaction();
        log.debug("trimDerivedTables height = '{}', inTransaction = '{}'",
                height, inTransaction);
        if (!inTransaction) {
            dataSource.begin();
        }
        try {
            return trimService.doTrimDerivedTablesOnHeight(height, true);
        } catch (Exception e) {
            databaseManager.getDataSource().rollback(false);
            throw new RuntimeException(e);
        } finally {
            databaseManager.getDataSource().commit();
        }
    }

    private ShardRecovery getOrCreateRecovery(MigrateState commandStartState) {
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(sourceDataSource);
        if (recovery == null) {
            // init new recovery
            recovery = new ShardRecovery(commandStartState);
            long generatedId = shardRecoveryDaoJdbc.saveShardRecovery(sourceDataSource, recovery);
            recovery.setShardRecoveryId(generatedId);
        }
        return recovery;
    }

    private void updateRecovery(ShardRecovery recovery, String processedObject) {
        if (StringUtils.isBlank(recovery.getProcessedObject())) {
            recovery.setProcessedObject(processedObject);
        } else {
            recovery.setProcessedObject(recovery.getProcessedObject() + "," + processedObject);
        }
        shardRecoveryDaoJdbc.updateShardRecovery(databaseManager.getDataSource(), recovery);
    }

    private Long exportDerivedTable(TableInfo info, CommandParamInfo paramInfo, Set<String> excludedColumns, int pruningTime) {
        DerivedTableInterface derivedTable = registry.getDerivedTable(info.getName());
        if (derivedTable != null) {
            if (!info.isPrunable()) {
                if (excludedColumns == null) {
                    return csvExporter.exportDerivedTable(derivedTable, paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
                } else {
                    return csvExporter.exportDerivedTable(derivedTable, paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize(), excludedColumns);
                }
            } else {
                return csvExporter.exportPrunableDerivedTable(((PrunableDbTable) derivedTable), paramInfo.getSnapshotBlockHeight(), pruningTime, paramInfo.getCommitBatchSize());
            }
        } else {
            durableTaskUpdateByState(FAILED, null, null);
            throw new IllegalArgumentException("Unable to find derived table " + info.getName() + " in derived table registry");
        }
    }

    private Long exportDerivedTable(TableInfo tableInfo, CommandParamInfo paramInfo, int pruningTime) {
        return exportDerivedTable(tableInfo, paramInfo, null, pruningTime);
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
            updateRecovery(recovery, tableName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState archiveCsv(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo);
        long startAllTables = System.currentTimeMillis();

        ShardRecovery recovery = getOrCreateRecovery(ZIP_ARCHIVE_STARTED);
        if (recovery.getState().getValue() >= ZIP_ARCHIVE_FINISHED.getValue()) {
            // skip to next step
            return state = ZIP_ARCHIVE_FINISHED;
        }
        durableTaskUpdateByState(state, 58.0, "CSV archiving...");
        try {
            UUID chainId = databaseManager.getChainId();
            ShardNameHelper shardNameHelper = new ShardNameHelper();

            String shardCoreFileName = shardNameHelper.getCoreShardArchiveNameByShardId(paramInfo.getShardId(),chainId);
            Set<String> coreTables = paramInfo.getTableInfoList().stream().filter(t -> !t.isPrunable()).map(TableInfo::getName).collect(Collectors.toSet());
            doZip(recovery, shardCoreFileName,
                    (dir, name) -> name.endsWith(".csv") && coreTables.contains(name.substring(0, name.indexOf(".csv"))),
                    (shard, hash) -> {
                        shard.setCoreZipHash(hash);
                        shardDao.updateShard(shard);
                    });
            String shardPrunableFileName = shardNameHelper.getPrunableShardArchiveNameByShardId(paramInfo.getShardId(),chainId);
            Set<String> prunableTables = paramInfo.getTableInfoList().stream().filter(TableInfo::isPrunable).map(TableInfo::getName).collect(Collectors.toSet());
            doZip(recovery, shardPrunableFileName,
                    (dir, name) -> name.endsWith(".csv") && prunableTables.contains(name.substring(0, name.indexOf(".csv"))),
                    (shard, hash) -> {
                        if (hash != null) {
                            shard.setPrunableZipHash(hash);
                            shardDao.updateShard(shard);
                        }
                    });
                // update recovery
                state = ZIP_ARCHIVE_FINISHED;
                updateToFinalStepState(recovery, state);
                durableTaskUpdateByState(state, 58.5, "CSV archived");
        } catch (Exception e) {
            log.error("Error ZIP ARCHIVE creation", e);
            state = MigrateState.FAILED;
            durableTaskUpdateByState(state, null, null);
            return state;
        }
        log.debug("ZIP ARCHIVE Processed in {} sec", (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    private void doZip(ShardRecovery recovery, String zipName, FilenameFilter fileFilter, BiConsumer<Shard, byte[]> postCompressTask) {
        if (AbstractHelper.isContain(recovery.getProcessedObject(), zipName)) {
            log.debug("Skip already performed compression for {} ", zipName);
        } else {
            Path zipPath = dirProvider.getDataExportDir().resolve(zipName);
            log.debug("Zip file name = '{}' will be searched/stored in '{}'", zipName, zipPath);
            // delete if something left in previous run
            boolean isRemoved = FileUtils.deleteFileIfExists(zipPath);
            log.debug("Previous Zip in '{}' was '{}'", zipName, isRemoved ? "REMOVED" : "NOT FOUND");
            // compute ZIP crc hash
            byte[] zipCrcHash = zipComponent.compressAndHash(
                    zipPath.toAbsolutePath().toString(),
                    dirProvider.getDataExportDir().toAbsolutePath().toString(), null, fileFilter, false);
            postCompressTask.accept(requireLastNotFinishedShard(), zipCrcHash);
            updateRecovery(recovery, zipName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState deleteCopiedData(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting Deleting data from [{}] tables...", paramInfo.getTableInfoList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(sourceDataSource);
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
            for (TableInfo tableInfo : paramInfo.getTableInfoList()) {
                long start = System.currentTimeMillis();
                currentTable = tableInfo.getName();

                BatchedPaginationOperation paginationOperationHelper = helperFactory.createDeleteHelper(currentTable);
                processOneTableByHelper(paramInfo, sourceConnect, currentTable, start, paginationOperationHelper);
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
        log.debug("DELETE Processed table(s)=[{}] in {} sec", paramInfo.getTableInfoList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MigrateState finishShardProcess(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        log.debug("Finish sharding...");

        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery();
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() >= COMPLETED.getValue()) {
            // skip to next step
            return state = COMPLETED;
        }
        state = COMPLETED;
        ((ShardManagement) databaseManager).addFullShard(paramInfo.getShardId());
        // complete sharding
        Shard shard = requireLastNotFinishedShard();
        shard.setShardState(ShardState.FULL);
        shardDao.updateShard(shard);
        shardRecoveryDao.hardDeleteAllShardRecovery();
        // call ANALYZE to optimize main db performance after massive copy/delete/update actions in it
        sourceDataSource.analyzeTables();
        log.debug("Shard process finished successfully, shard id - {}", paramInfo.getShardId());
        durableTaskUpdateByState(state, null, null);
        return state;
    }

    /**
     * Update previous info with table name just processed in loop
     * @param currentTable table name to be put into table
     * @param inProgressState progress state value
     * @return updated and stored instance
     */
    private ShardRecovery updateShardRecoveryProcessedTableList(
            Connection connection, String currentTable, MigrateState inProgressState) {
        ShardRecovery recovery = shardRecoveryDaoJdbc.getLatestShardRecovery(connection);
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
        shardRecoveryDaoJdbc.updateShardRecovery(connection, recovery); // update info for next step
        return recovery;
    }

    /**
     * Store final step state, when all tables were processed
     * @param recovery current recovery
     * @param finalStepState final state
     */
    private void updateToFinalStepState(Connection connection, ShardRecovery recovery, MigrateState finalStepState) {
       resetShardRecovery(recovery, finalStepState);
       shardRecoveryDaoJdbc.updateShardRecovery(connection, recovery); // update info for next step
    }

    private void updateToFinalStepState(ShardRecovery recovery, MigrateState finalStepState) {
        resetShardRecovery(recovery, finalStepState);
        shardRecoveryDaoJdbc.updateShardRecovery(databaseManager.getDataSource(), recovery); // update info for next step
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
                checkOrInitAppStatus(); // sometimes app status task is not created by 'task id', so try to find or create it
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
