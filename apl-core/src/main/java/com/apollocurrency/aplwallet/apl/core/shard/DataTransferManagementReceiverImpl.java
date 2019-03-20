/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.Optional;

import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedPaginationOperation;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class DataTransferManagementReceiverImpl implements DataTransferManagementReceiver {
    private static final Logger log = getLogger(DataTransferManagementReceiverImpl.class);

    private MigrateState state = MigrateState.INIT;
    private DatabaseManager databaseManager;
    private TrimService trimService;
    private OptionDAO optionDAO;
    private HelperFactory<BatchedPaginationOperation> helperFactory = new HelperFactoryImpl();
    private Optional<Long> createdShardId;
    private TransactionalDataSource createdShardSource;

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager, TrimService trimService) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager is NULL");
        this.trimService = Objects.requireNonNull(trimService, "trimService is NULL");
        this.optionDAO = new OptionDAO(this.databaseManager);
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
            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()); // store info about started process 'SHARD CREATION'
            // add info about state
            createdShardSource = ((ShardManagement)databaseManager).createAndAddShard(null, dbVersion);
            createdShardId = createdShardSource.getDbIdentity();
            if (dbVersion instanceof ShardAddConstraintsSchemaVersion
                    || dbVersion instanceof AplDbVersion) {
                state = MigrateState.SHARD_SCHEMA_FULL;
            } else {
                state = MigrateState.SHARD_SCHEMA_CREATED;
            }
            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name());
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
     * {@inheritDoc}
     */
    @Override
    public MigrateState copyDataToShard(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableNameList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        log.debug("Starting shard data transfer from [{}] tables...", paramInfo.getTableNameList().size());
        long startAllTables = System.currentTimeMillis();
        String lastTableName = null;

        TransactionalDataSource targetDataSource = initializeAssignMissingDataSources(new ShardInitTableSchemaVersion());
        // insert data as not processed previously
        Connection targetConnect = null;

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        try (Connection sourceConnect = sourceDataSource.getConnection() ) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                if (!targetDataSource.isInTransaction()) {
                    targetConnect = targetDataSource.begin();
                }
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName);
                Optional<BatchedPaginationOperation> sqlSelectAndInsertHelper = helperFactory.createSelectInsertHelper(tableName);
                if (sqlSelectAndInsertHelper.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), targetDataSource.getDbIdentity());
                    long totalCount = sqlSelectAndInsertHelper.get().selectInsertOperation(
                            sourceConnect, targetConnect, operationParams);
                    targetDataSource.commit(false);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                    sqlSelectAndInsertHelper.get().reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            targetDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (targetConnect != null) {
                targetDataSource.commit();
            }
        }
        state = MigrateState.DATA_COPIED_TO_SHARD;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name());
        log.debug("Copied Block/Transaction block, '{}' was saved = {}", state.name(), result);
        log.debug("Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableNameList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting LINKED data update from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        try ( Connection sourceConnect = sourceDataSource.begin() ) {
            long startTrim = System.currentTimeMillis();
            log.debug("Start trimming '{}' to HEIGHT '{}'", "PUBLIC_KEY", paramInfo.getSnapshotBlockHeight());
            trimService.doTrimDerivedTables(paramInfo.getSnapshotBlockHeight().intValue(), sourceDataSource); // TRIM 'PUBLIC_KEY' table before processing
            log.debug("Trimmed '{}' to HEIGHT '{}' within {} sec", "PUBLIC_KEY", paramInfo.getSnapshotBlockHeight(), (System.currentTimeMillis() - startTrim)/1000);

            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName);
                Optional<BatchedPaginationOperation> sqlSelectAndInsertHelper = helperFactory.createSelectInsertHelper(tableName);
                if (sqlSelectAndInsertHelper.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), createdShardId);

                    long totalCount = sqlSelectAndInsertHelper.get().selectInsertOperation(
                            sourceConnect, null, operationParams);
                    log.debug("Totally updated '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                    sqlSelectAndInsertHelper.get().reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        state = MigrateState.DATA_RELINKED_IN_MAIN;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, source.getDataSource()*/);
        log.debug("Updated Linked Data to Snapshot block, '{}' was saved = {}", state.name(), result);
        log.debug("Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState updateSecondaryIndex(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableNameList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting SECONDARY INDEX data update from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();

        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName);
                Optional<BatchedPaginationOperation> updateIndexHelper = helperFactory.createSelectInsertHelper(tableName);
                if (updateIndexHelper.isPresent() && createdShardId.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), createdShardId);

                    long totalCount = updateIndexHelper.get().selectInsertOperation(
                            sourceConnect, null, operationParams);
                    log.debug("Totally updated '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }
        state = MigrateState.SECONDARY_INDEX_UPDATED;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name());
        log.debug("Updated Secondary Index, '{}' was saved = {}", state.name(), result);
        log.debug("Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState deleteCopiedData(CommandParamInfo paramInfo) {
        Objects.requireNonNull(paramInfo, "paramInfo is NULL");
        Objects.requireNonNull(paramInfo.getTableNameList(), "table Name List is NULL");
        Objects.requireNonNull(paramInfo.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting Deleting data from [{}] tables...", paramInfo.getTableNameList().size());

        String currentTable = null;
        TransactionalDataSource sourceDataSource = databaseManager.getDataSource();
        try (Connection sourceConnect = sourceDataSource.begin()) {
            for (String tableName : paramInfo.getTableNameList()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName);
                Optional<BatchedPaginationOperation> updateIndexHelper = helperFactory.createDeleteHelper(tableName);
                if (updateIndexHelper.isPresent() && createdShardId.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, paramInfo.getCommitBatchSize(), paramInfo.getSnapshotBlockHeight(), createdShardId);

                    long totalCount = updateIndexHelper.get().selectInsertOperation(
                            sourceConnect, null, operationParams);
                    log.debug("Totally updated '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit();
            }
        }

        state = MigrateState.DATA_REMOVED_FROM_MAIN;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name());
        log.debug("Deleted block/transaction, '{}' was saved = {}", state.name(), result);
        log.debug("Processed table(s)=[{}] in {} secs", paramInfo.getTableNameList().size(), (System.currentTimeMillis() - startAllTables)/1000);
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
        try (Connection sourceConnect = sourceDataSource.begin();
            PreparedStatement preparedInsertStatement = sourceConnect.prepareStatement(
                    "insert into SHARD (SHARD_ID, SHARD_HASH, SHARD_STATE) values (?, ?, ?)")) {
            preparedInsertStatement.setLong(1, createdShardId.get());
            preparedInsertStatement.setBytes(2, paramInfo.getShardHash());
            preparedInsertStatement.setInt(3, 100); // 100% full shard is present on current node
            int result = preparedInsertStatement.executeUpdate();
            log.debug("Shard record is created {}", result);
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

        state = MigrateState.COMPLETED;
        boolean result = optionDAO.delete(PREVIOUS_MIGRATION_KEY); // remove sharding state if all is OK
        log.debug("Shard creation, '{}' was removed = {}", state.name(), result);
        log.debug("Shard created with Hash in {} secs", (System.currentTimeMillis() - startAllTables)/1000);
        return state;
    }


    private TransactionalDataSource initializeAssignMissingDataSources(DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        if (createdShardSource == null) {
            createdShardSource = ((ShardManagement)databaseManager).getOrCreateShardDataSourceById(null, dbVersion);
            createdShardId = createdShardSource.getDbIdentity();
            return createdShardSource;
        }
        return createdShardSource;
    }

}
