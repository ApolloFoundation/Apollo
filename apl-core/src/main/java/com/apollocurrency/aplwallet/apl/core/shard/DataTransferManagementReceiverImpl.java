/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.COMPLETED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPIED_TO_SHARD;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_COPY_TO_SHARD_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_RELINKED_IN_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_RELINK_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVED_FROM_MAIN;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.DATA_REMOVE_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_STARTED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SECONDARY_INDEX_UPDATED;
import static com.apollocurrency.aplwallet.apl.core.shard.MigrateState.SHARD_SCHEMA_FULL;
import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.PUBLIC_KEY_TABLE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.AbstractHelper;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedPaginationOperation;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@inheritDoc}
 */
@Singleton
public class DataTransferManagementReceiverImpl implements DataTransferManagementReceiver {
    private static final Logger log = getLogger(DataTransferManagementReceiverImpl.class);

    private MigrateState state = MigrateState.INIT;
    private DatabaseManager databaseManager;
    private TrimService trimService;
    private HelperFactory<BatchedPaginationOperation> helperFactory = new HelperFactoryImpl();
    private Optional<Long> createdShardId;
    private TransactionalDataSource createdShardSource;
    private ShardRecoveryDaoJdbc shardRecoveryDao;

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager, TrimService trimService,
                                              ShardRecoveryDaoJdbc shardRecoveryDao) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.shardRecoveryDao = Objects.requireNonNull(shardRecoveryDao, "shardRecoveryDao is NULL");
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
        } catch (Exception e) {
            log.error("Error creation Shard Db with Schema script:" + dbVersion.getClass().getSimpleName(), e);
            state = MigrateState.FAILED;
        }
        log.debug("INIT shard db={} by schema={} ({}) in {} secs",
                createdShardSource.getDbIdentity(), dbVersion.getClass().getSimpleName(), state.name(),
                (System.currentTimeMillis() - start)/1000);
        return state;
    }

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
                && recovery.getState().getValue() > DATA_COPIED_TO_SHARD.getValue()) {
            // skip to next step
            return state = DATA_COPIED_TO_SHARD;
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
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), targetDataSource.getDbIdentity(), Optional.ofNullable(dbIdExclusionSet));

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
            state = DATA_COPIED_TO_SHARD;
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
    @Override
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
            trimService.doTrimDerivedTables(paramInfo.getSnapshotBlockHeight().intValue(), sourceDataSource); // TRIM 'PUBLIC_KEY' table before processing
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

    private void processOneTableByHelper(CommandParamInfo paramInfo, Connection sourceConnect,
                                         String tableName, long start,
                                         Optional<BatchedPaginationOperation> paginationOperationHelper) throws Exception {
        TableOperationParams operationParams = new TableOperationParams(
                tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), createdShardId, Optional.ofNullable(paramInfo.getDbIdExclusionSet()));

        if (!paginationOperationHelper.isPresent()) { // should never happen from outside code, but better to play safe
            String error = "OperationHelper is NOT PRESENT... Fatal error in sharding code...";
            log.error(error);
            throw new IllegalStateException(error);
        }
        BatchedPaginationOperation batchedPaginationOperation = paginationOperationHelper.get();
        batchedPaginationOperation.setShardRecoveryDao(shardRecoveryDao); // mandatory assignment

        long totalCount = batchedPaginationOperation.processOperation(
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
                && recovery.getState().getValue() > SECONDARY_INDEX_UPDATED.getValue()) {
            // skip to next step
            return state = SECONDARY_INDEX_UPDATED;
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
                    processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper);
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
                recovery = updateShardRecoveryProcessedTableList(sourceConnect, currentTable, SECONDARY_INDEX_STARTED);
            }
            state = SECONDARY_INDEX_UPDATED;
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
                    processOneTableByHelper(paramInfo, sourceConnect, tableName, start, paginationOperationHelper);
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
                    "insert into SHARD (SHARD_ID, SHARD_HASH, SHARD_STATE) values (?, ?, ?)")) {
            preparedInsertStatement.setLong(1, createdShardId.get());
            preparedInsertStatement.setBytes(2, paramInfo.getShardHash());
            preparedInsertStatement.setInt(3, 100); // 100% full shard is present on current node
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
        recovery.setState(finalStepState);
        recovery.setObjectName(null); // remove currently processed table
        recovery.setColumnName(null); // main column used for pagination on data in table
        recovery.setLastColumnValue(null); // latest processed column value
        recovery.setProcessedObject(null); // list of previously processed table names within one step
        shardRecoveryDao.updateShardRecovery(connection, recovery); // update info for next step
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
