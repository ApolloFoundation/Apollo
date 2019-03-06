package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.model.OptionDAO;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.slf4j.Logger;

/**
 * {@inheritDoc}
 */
@Singleton
public class DataTransferManagementReceiverImpl implements DataTransferManagementReceiver {
    private static final Logger log = getLogger(DataTransferManagementReceiverImpl.class);
    private final SqlSelectAndInsertHelper sqlSelectAndInsertHelper = new SqlSelectAndInsertHelper();

    private MigrateState state = MigrateState.INIT;
    private DbProperties dbProperties;
    private DatabaseManager databaseManager;
    private Blockchain blockchain;
    private OptionDAO optionDAO;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public DataTransferManagementReceiverImpl() {
    }

    @Inject
    public DataTransferManagementReceiverImpl(DatabaseManager databaseManager, Blockchain blockchain) {
        this.dbProperties = databaseManager.getBaseDbProperties();
        this.databaseManager = databaseManager;
        this.blockchain = blockchain;
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
    public MigrateState addOrCreateShard(DatabaseMetaInfo source) {
        log.debug("Creating shard db file...");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(source.getNewFileName(), "new DB file is NULL");
        try {
            // add info about state
            TransactionalDataSource shardDb = databaseManager.createAndAddShard(null);
            if (optionDAO.get(PREVIOUS_MIGRATION_KEY, shardDb) != null) {
                return MigrateState.SHARD_DB_CREATED; // continue to next state
            }
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.SHARD_DB_CREATED.name(), shardDb);
            return MigrateState.SHARD_DB_CREATED;
        } catch (Exception e) {
            log.error("Error creation Temp Db", e);
            return MigrateState.FAILED;
        }
    }

/*
    @Override
    public MigrateState createTempDb(DatabaseMetaInfo source) {
        log.debug("Creating TEMP db file...");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(source.getNewFileName(), "new DB file is NULL");
        try {
            // add info about state
            TransactionalDataSource temporaryDb = databaseManager.createAndAddTemporaryDb(source.getNewFileName());
//            databaseManager.shutdown(temporaryDb); // temp db is in Database manager
            if (optionDAO.get(PREVIOUS_MIGRATION_KEY, temporaryDb) != null) {
                return MigrateState.SHARD_DB_CREATED; // continue to next state
            }
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.SHARD_DB_CREATED.name(), temporaryDb);
            return MigrateState.SHARD_DB_CREATED;
        } catch (Exception e) {
            log.error("Error creation Temp Db", e);
            return MigrateState.FAILED;
        }
    }

    @Override
    public MigrateState addSnapshotBlock(DatabaseMetaInfo targetDataSource) {
        log.debug("Add Snapshot block into TEMP db...");
        Objects.requireNonNull(targetDataSource, "target Data Source meta-info is NULL");
        Block snapshotBlock = targetDataSource.getSnapshotBlock();
        Objects.requireNonNull(snapshotBlock, "snapshot Block is NULL");
        TransactionalDataSource temporaryDb = databaseManager.getOrCreateShardDataSourceById(-1L); // temp db is cached
        if (optionDAO.get(PREVIOUS_MIGRATION_KEY, temporaryDb) != null
                && (optionDAO.get(PREVIOUS_MIGRATION_KEY, temporaryDb).equalsIgnoreCase( MigrateState.SNAPSHOT_BLOCK_CREATED.name())
                || optionDAO.get(PREVIOUS_MIGRATION_KEY, temporaryDb).equalsIgnoreCase( MigrateState.DATA_MOVING_STARTED.name()) ) ) {
            return MigrateState.SNAPSHOT_BLOCK_CREATED; // continue to next state
        }
        try (Connection connection = temporaryDb.getConnection()) {
            blockchain.saveBlock(connection, snapshotBlock);
            log.debug("Saved Snapshot block = {}", snapshotBlock);
            boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.SNAPSHOT_BLOCK_CREATED.name(), temporaryDb);
            log.debug("Add Snapshot block MigrateState.SNAPSHOT_BLOCK_CREATED was saved = {}", result);
            return MigrateState.SNAPSHOT_BLOCK_CREATED;
        } catch (Exception e) {
            log.error("Error creation Temp Db", e);
            return MigrateState.FAILED;
        }
    }
/*
    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState moveDataBlockLinkedData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        log.debug("Starting LINKED data transfer from [{}] tables...", tableNameCountMap.size());
        TransactionalDataSource targetDataSource = assignDataSourceIfMissing(source, target);
        optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_STARTED.name(), targetDataSource);
//        Block snapshootBlock = this.blockchain.getLastBlock(targetDataSource);
        if (target.getSnapshotBlock() == null) {
            log.error("Snapshot block was NOT inserted previously...");
            return MigrateState.FAILED;
        }
//        target.setSnapshotBlock(snapshootBlock);
        return this.moveData(tableNameCountMap, source, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState moveData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target) {
        Objects.requireNonNull(tableNameCountMap, "tableNameCountMap is NULL");
        Objects.requireNonNull(source, "source meta-info is NULL");
        Objects.requireNonNull(target, "target meta-info is NULL");
        log.debug("Starting shard data transfer from [{}] tables...", tableNameCountMap.size());
        String lastTableName = null;
        TransactionalDataSource targetDataSource = assignDataSourceIfMissing(source, target);
        optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_STARTED.name(), targetDataSource);
/*
        if (optionDAO.get(PREVIOUS_MIGRATION_KEY, targetDataSource) == null) {
            optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVING_STARTED.name(), targetDataSource);
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
            long startAllTables = System.currentTimeMillis();
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
                    optionDAO.set(LAST_MIGRATION_OBJECT_NAME, tableName, targetDataSource);
                    long totalCount = sqlSelectAndInsertHelper.generateInsertStatementsWithPaging(
                            sourceConnect, targetConnect, tableName, target.getCommitBatchSize(), target.getSnapshotBlock());
                    targetDataSource.commit(false);
                    log.debug("Totally inserted '{}' records in table ='{}' within {} sec", totalCount, tableName, (System.currentTimeMillis() - start)/1000);
                    sqlSelectAndInsertHelper.reset();
                }
            } catch (Exception e) {
                log.error("Error processing table = '" + currentTable + "'", e);
                targetDataSource.rollback(false);
            } finally {
                if (targetConnect != null) {
                    targetDataSource.commit();
                }
            }
            log.debug("Processed table(s)=[{}] in {} secs", tableNameCountMap.size(), (System.currentTimeMillis() - startAllTables)/1000);
            boolean result = optionDAO.set(PREVIOUS_MIGRATION_KEY, MigrateState.DATA_MOVED.name(), source.getDataSource());
            log.debug("Add Snapshot block MigrateState.SNAPSHOT_BLOCK_CREATED was saved = {}", result);

        }
        return MigrateState.DATA_MOVING_STARTED;
    }

    private TransactionalDataSource assignDataSourceIfMissing(DatabaseMetaInfo source, DatabaseMetaInfo target) {
        if (source.getDataSource() == null) {
            source.setDataSource(databaseManager.getDataSource());
        }
        TransactionalDataSource targetDataSource = target.getDataSource();
        if (targetDataSource == null) {
            target.setDataSource(databaseManager.getOrCreateShardDataSourceById(null));
            targetDataSource = target.getDataSource();
        }
        return targetDataSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrateState renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target) {
        log.debug("Starting shard files renaming...");
        return MigrateState.COMPLETED;
    }

}
