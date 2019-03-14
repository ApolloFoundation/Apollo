/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.apollocurrency.aplwallet.apl.core.app.TrimService;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DatabaseMetaInfo;
import com.apollocurrency.aplwallet.apl.core.shard.helper.BatchedSelectInsert;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactory;
import com.apollocurrency.aplwallet.apl.core.shard.helper.HelperFactoryImpl;
import com.apollocurrency.aplwallet.apl.core.shard.helper.TableOperationParams;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class DataTransferManagementReceiverImpl implements DataTransferManagementReceiver {
    private static final Logger log = getLogger(DataTransferManagementReceiverImpl.class);

    private MigrateState state = MigrateState.INIT;
    private DbProperties dbProperties;
    private DatabaseManager databaseManager;
    private TrimService trimService;
    private OptionDAO optionDAO;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private HelperFactory<BatchedSelectInsert> helperFactory = new HelperFactoryImpl();
    private Optional<Long> createdShardId;

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager, TrimService trimService) {
        this.dbProperties = databaseManager.getBaseDbProperties();
        this.databaseManager = databaseManager;
        this.trimService = trimService;
        this.optionDAO = new OptionDAO(this.databaseManager); // actually we want to use TEMP=TARGET data source in OptionDAO
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

    @Override
    public MigrateState addOrCreateShard(/*DatabaseMetaInfo source, */DbVersion dbVersion) {
        long start = System.currentTimeMillis();
//        Objects.requireNonNull(source, "source meta-info is NULL");
//        Objects.requireNonNull(source.getNewFileName(), "new DB file is NULL");
        Objects.requireNonNull(dbVersion, "dbVersion is NULL");
        log.debug("INIT shard db file by schema={}", dbVersion.getClass().getSimpleName());
        try {
            // add info about state
            TransactionalDataSource shardDb = databaseManager.createAndAddShard(null, dbVersion);
            createdShardId = shardDb.getDbIdentity();
            if (optionDAO.get(PREVIOUS_MIGRATION_KEY/*, shardDb*/) != null) {
                state = MigrateState.SHARD_DB_CREATED; // continue to next state
                log.debug("Previously created shard db={} by schema={} in {} secs", createdShardId, dbVersion.getClass().getSimpleName(),
                        (System.currentTimeMillis() - start)/1000);
                return state;
            }
            state = MigrateState.SHARD_DB_CREATED;
            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, shardDb*/);
        } catch (Exception e) {
            log.error("Error creation Shard Db with Schema script:" + dbVersion.getClass().getSimpleName(), e);
            state = MigrateState.FAILED;
//            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, targetDataSource*/);
        }
        log.debug("INIT shard db={} by schema={} in {} secs", createdShardId, dbVersion.getClass().getSimpleName(),
                (System.currentTimeMillis() - start)/1000);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState moveData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        Objects.requireNonNull(target.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        log.debug("Starting shard data transfer from [{}] tables...", tableNameCountMap.size());
        long startAllTables = System.currentTimeMillis();
        String lastTableName = null;
        TransactionalDataSource targetDataSource = initializeAssignMissingDataSources(source, target, new ShardInitTableSchemaVersion());
        state = MigrateState.DATA_MOVING_TO_SHARD_STARTED;
        optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, targetDataSource*/);
/*
        if (optionDAO.get(PREVIOUS_MIGRATION_KEY, targetDataSource) == null) {
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_TO_SHARD_STARTED.name(), targetDataSource);
        } else {
            // continue previous run
            lastTableName = optionDAO.get(LAST_MIGRATION_OBJECT_NAME, targetDataSource);
        }
*/
        if (lastTableName != null && !lastTableName.isEmpty()) {
            // NOT FINISHED YET!!!
            // check/compare records count in source and target, clear target if needed, reinsert again in case...
        } else {
            // insert data as not processed previously
            Connection targetConnect = null;

            String currentTable = null;
            try (
                    Connection sourceConnect = source.getDataSource().getConnection();
            ) {
                for (String tableName : tableNameCountMap.keySet()) {
                    long start = System.currentTimeMillis();
                    if (!targetDataSource.isInTransaction()) {
                        targetConnect = targetDataSource.begin();
                    }
                    currentTable = tableName;
                    optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName/*, targetDataSource*/);
                    Optional<BatchedSelectInsert> sqlSelectAndInsertHelper = helperFactory.createSelectInsertHelper(tableName);
                    if (sqlSelectAndInsertHelper.isPresent()) {
                        TableOperationParams operationParams = new TableOperationParams(
                                tableName, target.getCommitBatchSize(), target.getSnapshotBlockHeight(), targetDataSource.getDbIdentity());
                        long totalCount = sqlSelectAndInsertHelper.get().selectInsertOperation(
                                sourceConnect, targetConnect, operationParams);
                        targetDataSource.commit(false);
                        log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                        sqlSelectAndInsertHelper.get().reset();
                    } else {
                        log.warn("NO processing HELPER class for table '{}'", tableName);
                    }
                }
//                optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVED_TO_SHARD.name()/*, targetDataSource*/);
            } catch (Exception e) {
                log.error("Error processing table = '" + currentTable + "'", e);
                targetDataSource.rollback(false);
                state = MigrateState.FAILED;
//                optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, targetDataSource*/);
                return state;
            } finally {
                if (targetConnect != null) {
                    targetDataSource.commit(false);
                }
            }
            state = MigrateState.DATA_MOVED_TO_SHARD;
            boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, source.getDataSource()*/);
            log.debug("Add Snapshot block MigrateState.SNAPSHOT_BLOCK_CREATED was saved = {}", result);
            log.debug("Processed table(s)=[{}] in {} secs", tableNameCountMap.size(), (System.currentTimeMillis() - startAllTables)/1000);
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState relinkDataToSnapshotBlock(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        Objects.requireNonNull(target.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting LINKED data update from [{}] tables...", tableNameCountMap.size());

        initializeAssignMissingDataSources(source, target, new ShardInitTableSchemaVersion());
//        optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_TO_SHARD_STARTED.name(), targetDataSource);
        if (target.getSnapshotBlockHeight() == null) {
            log.error("Snapshot block HEIGHT was not specified...");
            state = MigrateState.FAILED;
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.FAILED.name()/*, targetDataSource*/);
            return state;
        }
//        target.setSnapshotBlock(snapshootBlock);
        String currentTable = null;
        TransactionalDataSource sourceDataSource = source.getDataSource();
        try (
                Connection sourceConnect = sourceDataSource.begin()
        ) {
            long startTrim = System.currentTimeMillis();
            log.debug("Start trimming '{}' to HEIGHT '{}'", "PUBLIC_KEY", target.getSnapshotBlockHeight());
            trimService.doTrimDerivedTables(target.getSnapshotBlockHeight().intValue(), sourceDataSource); // TRIM 'PUBLIC_KEY' table before processing
            log.debug("Trimmed '{}' to HEIGHT '{}' within {} sec", "PUBLIC_KEY", target.getSnapshotBlockHeight(), (System.currentTimeMillis() - startTrim)/1000);

            for (String tableName : tableNameCountMap.keySet()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName/*, targetDataSource*/);
                Optional<BatchedSelectInsert> sqlSelectAndInsertHelper = helperFactory.createSelectInsertHelper(tableName);
                if (sqlSelectAndInsertHelper.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, target.getCommitBatchSize(), target.getSnapshotBlockHeight(), createdShardId);

                    long totalCount = sqlSelectAndInsertHelper.get().selectInsertOperation(
                            sourceConnect, null, operationParams);
                    sourceDataSource.commit(false);
                    log.debug("Totally updated '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                    sqlSelectAndInsertHelper.get().reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
//                optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVED_TO_SHARD.name()/*, targetDataSource*/);
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
//            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, targetDataSource*/);
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit(false);
            }
        }
        state = MigrateState.DATA_RELINKED_IN_MAIN;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, source.getDataSource()*/);
        log.debug("Add Snapshot block MigrateState.DATA_RELINKED_IN_MAIN was saved = {}", result);
        log.debug("Processed table(s)=[{}] in {} secs", tableNameCountMap.size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState updateSecondaryIndex(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source/*, DatabaseMetaInfo target*/) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
//        Objects.requireNonNull(target, "target meta-info is NULL");
        Objects.requireNonNull(source.getSnapshotBlockHeight(), "target Snapshot Block height is NULL");
        long startAllTables = System.currentTimeMillis();
        log.debug("Starting SECONDARY INDEX data update from [{}] tables...", tableNameCountMap.size());

//        initializeAssignMissingDataSources(source, target, new ShardInitTableSchemaVersion());

        if (source.getDataSource() == null) {
            source.setDataSource(databaseManager.getDataSource());
        }

//        optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_TO_SHARD_STARTED.name(), targetDataSource);
        if (source.getSnapshotBlockHeight() == null) {
            log.error("Snapshot block HEIGHT was not specified...");
            state = MigrateState.FAILED;
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.FAILED.name()/*, targetDataSource*/);
            return state;
        }
//        target.setSnapshotBlock(snapshootBlock);
        String currentTable = null;
        TransactionalDataSource sourceDataSource = source.getDataSource();
        Connection sourceConnect = null;
        try {
            if (!sourceDataSource.isInTransaction()) {
                sourceConnect = sourceDataSource.begin();
            } else {
                sourceConnect = sourceDataSource.getConnection();
            }

            for (String tableName : tableNameCountMap.keySet()) {
                long start = System.currentTimeMillis();
                currentTable = tableName;
                optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName/*, targetDataSource*/);
                Optional<BatchedSelectInsert> updateIndexHelper = helperFactory.createSelectInsertHelper(tableName);
                if (updateIndexHelper.isPresent() && createdShardId.isPresent()) {
                    TableOperationParams operationParams = new TableOperationParams(
                            tableName, source.getCommitBatchSize(), source.getSnapshotBlockHeight(), createdShardId);

                    long totalCount = updateIndexHelper.get().selectInsertOperation(
                            sourceConnect, null, operationParams);
                    sourceDataSource.commit(false);
                    log.debug("Totally updated '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
//                    sourceConnect.commit();
                    updateIndexHelper.get().reset();
                } else {
                    log.warn("NO processing HELPER class for table '{}'", tableName);
                }
            }
//                optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.SECONDARY_INDEX_UPDATED.name()/*, targetDataSource*/);
        } catch (Exception e) {
            log.error("Error processing table = '" + currentTable + "'", e);
            sourceDataSource.rollback(false);
            state = MigrateState.FAILED;
//            optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, targetDataSource*/);
            return state;
        } finally {
            if (sourceDataSource != null) {
                sourceDataSource.commit(false);
            }
        }
        state = MigrateState.SECONDARY_INDEX_UPDATED;
        boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, state.name()/*, source.getDataSource()*/);
        log.debug("Add Snapshot block MigrateState.SECONDARY_INDEX_UPDATED was saved = {}", result);
        log.debug("Processed table(s)=[{}] in {} secs", tableNameCountMap.size(), (System.currentTimeMillis() - startAllTables)/1000);

        return state;
    }

    private TransactionalDataSource initializeAssignMissingDataSources(DatabaseMetaInfo source, DatabaseMetaInfo target, DbVersion dbVersion) {
        if (source.getDataSource() == null) {
            source.setDataSource(databaseManager.getDataSource());
        }
        TransactionalDataSource targetDataSource = target.getDataSource();
        if (targetDataSource == null) {
            target.setDataSource(databaseManager.getOrCreateShardDataSourceById(
                    createdShardId.orElse(null), dbVersion));
            targetDataSource = target.getDataSource();
        }
        return targetDataSource;
    }

    /**
     * {@inheritDoc}
     */
/*
    @Override
    public MigrateState renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target) {
        log.debug("Starting shard files renaming...");
        return MigrateState.COMPLETED;
    }
*/

}
