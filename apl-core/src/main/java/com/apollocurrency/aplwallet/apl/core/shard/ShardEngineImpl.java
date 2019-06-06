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
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_FINISHED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.ZIP_ARCHIVE_STARTED;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.AbstractHelper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedPaginationOperation;
import com.apollocurrency.aplwallet.apl.core.shard.helper.CsvExporter;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import com.apollocurrency.aplwallet.apl.core.shard.helper.csv.CsvAbstractBase;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private Optional<Long> createdShardId;
    private TransactionalDataSource createdShardSource;
    private ShardRecoveryDaoJdbc shardRecoveryDao;
    private CsvExporter csvExporter;
    private DerivedTablesRegistry registry;
    private DirProvider dirProvider;
    
    public ShardEngineImpl() {
    }

    @Inject
    public ShardEngineImpl(DirProvider dirProvider,DatabaseManager databaseManager, TrimService trimService,
                           ShardRecoveryDaoJdbc shardRecoveryDao, CsvExporter csvExporter,
                           DerivedTablesRegistry registry) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(shardRecoveryDao, "shardRecoveryDao is NULL");
        this.csvExporter = Objects.requireNonNull(csvExporter, "csvExporter is NULL");
        this.registry = Objects.requireNonNull(registry, "registry is NULL");
        this.dirProvider = dirProvider;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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
            }
            log.debug("BACKUP by SQL={} was successful", sql);
            state = MigrateState.MAIN_DB_BACKUPED;
            loadAndRefreshRecovery(sourceDataSource);
        } catch (SQLException e) {
            log.error("ERROR on backup db before sharding, sql = " + sql, e);
            state = FAILED;
        }
        log.debug("BACKUP db before shard ({}) in {} secs", state.name(),
                (System.currentTimeMillis() - start)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState addOrCreateShard(DbVersion dbVersion) {
        long start = System.currentTimeMillis();
        Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        log.debug("INIT shard db file by schema={}", dbVersion.getClass().getSimpleName());
        try {
            // we ALWAYS need to do that STEP to attach to new/existing shard db !!
            createdShardSource = ((ShardManagement)databaseManager).createAndAddShard(null, dbVersion);
            createdShardId = createdShardSource.getDbIdentity(); // MANDATORY ACTION FOR SUCCESS completion !!
            if (dbVersion instanceof ShardAddConstraintsSchemaVersion
                    || dbVersion instanceof AplDbVersion) {
                state = SHARD_SCHEMA_FULL;
            } else {
                state = MigrateState.SHARD_SCHEMA_CREATED;
            }
            TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
            loadAndRefreshRecovery(sourceDataSource);
        } catch (Exception e) {
            log.error("Error creation Shard Db with Schema script:" + dbVersion.getClass().getSimpleName(), e);
            state = MigrateState.FAILED;
        }
        log.debug("INIT shard db={} by schema={} ({}) in {} secs",
                createdShardSource.getDbIdentity(), dbVersion.getClass().getSimpleName(), state.name(),
                (System.currentTimeMillis() - start)/1000);
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

        TransactionalDataSource targetDataSource = initializeAssignShardDataSource(new ShardInitTableSchemaVersion());
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
        try (Connection sourceConnect = sourceDataSource.getConnection() ) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                if (!targetDataSource.isInTransaction()) {
                    targetConnect = targetDataSource.begin();
                }
                currentTable = tableName;

                Optional<BatchedPaginationOperation> paginationOperationHelper = helperFactory.createSelectInsertHelper(tableName);
                if (paginationOperationHelper.isPresent()) {
                    Set<Long> dbIdExclusionSet = paramInfo.getDbIdExclusionSet();
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(),
                            targetDataSource.getDbIdentity(), Optional.ofNullable(dbIdExclusionSet));

                    BatchedPaginationOperation batchedPaginationOperation = paginationOperationHelper.get();
                    batchedPaginationOperation.setShardRecoveryDao(shardRecoveryDao);// mandatory

                    long totalCount = batchedPaginationOperation.processOperation(
                            sourceConnect, targetConnect, operationParams);
                    targetDataSource.commit(false);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                    batchedPaginationOperation.reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, DATA_COPY_TO_SHARD_STARTED);
            }
            state = DATA_COPY_TO_SHARD_FINISHED;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
        } catch (Exception e) {
            log.error("Error COPY processing table = '" + currentTable + "'", e);
            targetDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (targetConnect != null) {
                targetDataSource.commit();
            }
        }
        log.debug("COPY Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
/*
    @Override
    @Deprecated
    public MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo) {
        checkRequiredParameters(paramInfo);
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting LINKED data update from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() > DATA_RELINKED_IN_MAIN.getValue()) {
            // skip to next step
            return state = DATA_RELINKED_IN_MAIN;
        } else { // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(DATA_RELINK_STARTED);
        }

        try ( Connection sourceConnect = sourceDataSource.begin() ) {

            long startTrim = System.currentTimeMillis();
            currentTable = PUBLIC_KEY_TABLE_NAME; // assign name for trim
            log.debug("Start trimming '{}' to HEIGHT '{}'", "PUBLIC_KEY", paramInfo.getSnapshotBlockHeight());
//            trimService.doTrimDerivedTables(paramInfo.getSnapshotBlockHeight().intValue(), sourceDataSource); // TRIM 'PUBLIC_KEY' table before processing
            log.debug("Trimmed '{}' to HEIGHT '{}' within {} sec", "PUBLIC_KEY", paramInfo.getSnapshotBlockHeight(), (System.currentTimeMillis() - startTrim)/1000);

            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;

                Optional<BatchedPaginationOperation> paginationOperationHelper = helperFactory.createSelectInsertHelper(tableName, true);
                if (paginationOperationHelper.isPresent()) {
                    processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper);
                    paginationOperationHelper.get().reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, DATA_RELINK_STARTED);
            }
            state = DATA_RELINKED_IN_MAIN;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
        } catch (Exception e) {
            log.error("Error RELINK processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("RELINK Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }
*/

    private void processOneTableByHelper(CommandParamInfo paramInfo, Connection sourceConnect,
                                         String tableName, long start,
                                         BatchedPaginationOperation paginationOperationHelper) throws Exception {
        TableOperationParams operationParams = new TableOperationParams(
                tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), createdShardId, Optional.ofNullable(paramInfo.getDbIdExclusionSet()));

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

        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;

                Optional<BatchedPaginationOperation> paginationOperationHelper = helperFactory.createSelectInsertHelper(tableName);
                if (paginationOperationHelper.isPresent() && createdShardId.isPresent()) {
                    processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper.get());
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, SECONDARY_INDEX_STARTED);
            }
            state = SECONDARY_INDEX_FINISHED;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
        } catch (Exception e) {
            log.error("Error UPDATE S/Index processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("UPDATE Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);

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

        trimDerivedTables(paramInfo.getSnapshotBlockHeight());

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
                        return csvExporter.exportTransactions(paramInfo.getDbIdExclusionSet());
                    case "block":
                        return csvExporter.exportBlock(paramInfo.getSnapshotBlockHeight());
                    default:
                        return exportDerivedTable(tableName, paramInfo);
                }
            });
        }
        state = CSV_EXPORT_FINISHED;
        updateToFinalStepState(recovery, state);
        log.debug("Export finished in {} secs", (System.currentTimeMillis() - startTime)/1000);
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

    private Long exportDerivedTable(String tableName, CommandParamInfo paramInfo) {
        DerivedTableInterface derivedTable = registry.getDerivedTable(tableName);
        if (derivedTable != null) {
            return csvExporter.exportDerivedTable(derivedTable, paramInfo.getSnapshotBlockHeight(), paramInfo.getCommitBatchSize());
        } else {
            throw new IllegalArgumentException("Unable to find derived table " + tableName + " in derived table registry");
        }
    }

    private void exportTableWithRecovery(ShardRecovery recovery, String tableName, Supplier<Long> exportPerformer) {
        if (AbstractHelper.isContain(recovery.getProcessedObject(), tableName)) {
            log.debug("Skip already exported table: " + tableName);
        } else {
            Path tableCsvPath = csvExporter.getDataExportPath().resolve(tableName + CsvAbstractBase.CSV_FILE_EXTENSION);
            try {
                Files.deleteIfExists(tableCsvPath);
            }
            catch (IOException e) {
                throw new RuntimeException("Unable to remove not finished csv file: " + tableCsvPath.toAbsolutePath().toString());
            }
            long startTableExportTime = System.currentTimeMillis();
            Long exported = exportPerformer.get();
            log.debug("Exported - {}, from {} to {} in {} secs", exported, tableName, tableCsvPath, (System.currentTimeMillis() - startTableExportTime)/1000);

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
        checkRequiredParameters(paramInfo);
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting ZIP ARCHIVE data update from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() > ZIP_ARCHIVE_FINISHED.getValue()) {
            // skip to next step
            return state = ZIP_ARCHIVE_FINISHED;
        } else {
            // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(ZIP_ARCHIVE_STARTED);
        }

        // TODO: put into archive CSV for all : derived tables, shard, block_index, transaction_shard_index + compute CRC/hash and update column value in SHARD
        // TODO: keep recovery data on export

        state = ZIP_ARCHIVE_FINISHED;
//        updateToFinalStepState(sourceDataSource.begin(), recovery, state);
        log.debug("ZIP ARCHIVE Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
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

        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;

                Optional<BatchedPaginationOperation> paginationOperationHelper = helperFactory.createDeleteHelper(tableName);
                if (paginationOperationHelper.isPresent() && createdShardId.isPresent()) {
                    processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper.get());
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, DATA_REMOVE_STARTED);
            }
            state = DATA_REMOVED_FROM_MAIN;
            updateToFinalStepState(sourceConnect, recovery, state);
            sourceConnect.commit();
        } catch (Exception e) {
            log.error("Error DELETE processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("DELETE Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState addShardInfo(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getShardHash(), "shardHash is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting create SHARD record in main db...");

        if (!createdShardId.isPresent()) {
            String error = "Error. Shard was not initialized previously, " +
                    "missing addOrCreateShard(dbVersion) step during sharding process!";
            log.error(error);
            throw new IllegalStateException(error);
        }

        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(sourceDataSource);
        if (recovery != null && recovery.getState() != null
                && recovery.getState().getValue() >= COMPLETED.getValue()) {
            // skip to next step
            return state = COMPLETED;
        } else {
            // that is needed in case separate step execution, when previous step was missed in code
            recovery = new ShardRecovery(COMPLETED);
        }

        try (Connection sourceConnect = sourceDataSource.begin();
            PreparedStatement preparedInsertStatement = sourceConnect.prepareStatement(
                    "UPDATE SHARD SET SHARD_HASH = ?, SHARD_STATE = ? WHERE SHARD_ID = ?")) {
            preparedInsertStatement.setBytes(1, paramInfo.getShardHash());
//            preparedInsertStatement.setLong(1, paramInfo.getSnapshotBlockHeight());
            preparedInsertStatement.setLong(2, SHARD_PERCENTAGE_FULL); // 100% full shard is present on current node
            preparedInsertStatement.setLong(3, createdShardId.get());
            int result = preparedInsertStatement.executeUpdate();
            log.debug("Shard record is created = '{}'", result);
            state = COMPLETED;
            // remove recovery data when process is completed
            recovery = shardRecoveryDao.getLatestShardRecovery(sourceConnect);
            result = shardRecoveryDao.hardDeleteShardRecovery(sourceConnect, recovery.getShardRecoveryId());
            sourceConnect.commit();
            log.debug("Shard Recovery is deleted = '{}'", result);
        } catch (Exception e) {
            log.error("Error creating Shard record in main db", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        log.debug("Shard record is created with Hash in {} msec", System.currentTimeMillis() - startAllTables);
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
        ShardRecovery recovery = shardRecoveryDao.getLatestShardRecovery(connection);
        // add processed table name into optional column for later use
        recovery.setState(inProgressState);
        // we want :
        // - add new table name into empty column
        // - preserve column value if the same table name exist inside
        // - add table name if it not exists in list separated by space
        recovery.setProcessedObject(recovery.getProcessedObject() != null ?
                (!AbstractHelper.isContain(recovery.getProcessedObject(), currentTable) ?
                        recovery.getProcessedObject() + " " + currentTable.toUpperCase() : recovery.getProcessedObject())
                : currentTable.toUpperCase()); // add processed table name into list if NOT exists
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

    /**
     * Method is need for separate steps execution to extract 'createdShardId' value
     * @param dbVersion shard schema class
     * @return shard data source
     */
    private TransactionalDataSource initializeAssignShardDataSource(DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        if (createdShardSource == null) {
            createdShardSource = ((ShardManagement)databaseManager).getOrCreateShardDataSourceById(null, dbVersion);
            createdShardId = createdShardSource.getDbIdentity();
            return createdShardSource;
        }
        return createdShardSource;
    }

}
