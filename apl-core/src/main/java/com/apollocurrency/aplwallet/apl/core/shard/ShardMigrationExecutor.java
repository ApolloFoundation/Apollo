/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.core.shard.commands.BackupDbBeforeShardCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CsvExportCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ZipArchiveCommand;
import com.apollocurrency.aplwallet.apl.core.shard.hash.ShardHashCalculator;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEvent;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEventBinding;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Component for starting sharding process which contains several steps/states.
 *
 * @author yuriy.larin
 */
@Singleton
public class ShardMigrationExecutor {
    private static final Logger log = getLogger(ShardMigrationExecutor.class);
    private final List<DataMigrateOperation> dataMigrateOperations = new ArrayList<>();

    private final javax.enterprise.event.Event<MigrateState> migrateStateEvent;
    private ShardEngine shardEngine;
    private ShardHashCalculator shardHashCalculator;
    private ShardDao shardDao;
    private ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor;
    private DerivedTablesRegistry derivedTablesRegistry;
    private GeneratorIdsExtractor generatorIdsExtractor;
    private volatile boolean backupDb;

    public boolean backupDb() {
        return backupDb;
    }

    public void setBackupDb(boolean backupDb) {
        this.backupDb = backupDb;
    }

    @Inject
    public ShardMigrationExecutor(ShardEngine shardEngine,
                                  javax.enterprise.event.Event<MigrateState> migrateStateEvent,
                                  ShardHashCalculator shardHashCalculator,
                                  ShardDao shardDao,
                                  ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor,
                                  GeneratorIdsExtractor generatorIdsExtractor,
                                  DerivedTablesRegistry registry,
                                  @Property(value = "apl.sharding.backupDb", defaultValue = "false") boolean backupDb) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "managementReceiver is NULL");
        this.migrateStateEvent = Objects.requireNonNull(migrateStateEvent, "migrateStateEvent is NULL");
        this.shardHashCalculator = Objects.requireNonNull(shardHashCalculator, "sharding hash calculator is NULL");
        this.shardDao = Objects.requireNonNull(shardDao, "shardDao is NULL");
        this.excludedTransactionDbIdExtractor = Objects.requireNonNull(excludedTransactionDbIdExtractor, "exluded transaction db_id extractor is NULL");
        this.derivedTablesRegistry = Objects.requireNonNull(registry, "derived table registry is null");
        this.backupDb = backupDb;
        this.generatorIdsExtractor = Objects.requireNonNull(generatorIdsExtractor);
    }

    private void addCreateSchemaCommand(long shardId) {
        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(shardId, shardEngine,
                new ShardInitTableSchemaVersion(), /*hash should be null here*/ null, null);
        this.addOperation(createShardSchemaCommand);
    }

    @Transactional
    public void createAllCommands(int height, long shardId, MigrateState state) {

        switch (state) {
            case INIT:
                if (backupDb) {
                    log.info("Will backup db before sharding");
                    BackupDbBeforeShardCommand beforeShardCommand = new BackupDbBeforeShardCommand(shardEngine);
                    this.addOperation(beforeShardCommand);
                } else {
                    addCreateSchemaCommand(shardId);
                }
            case MAIN_DB_BACKUPED:
                addCreateSchemaCommand(shardId); //should happen only when enabled backup
            case SHARD_SCHEMA_CREATED:
            case DATA_COPY_TO_SHARD_STARTED:
                ExcludeInfo excludeInfo = excludedTransactionDbIdExtractor.getExcludeInfo(height);
                CopyDataCommand copyDataCommand = new CopyDataCommand(shardId, shardEngine, height, excludeInfo);
                this.addOperation(copyDataCommand);
            case DATA_COPY_TO_SHARD_FINISHED:
                byte[] hash = calculateHash(height);
                if (hash == null || hash.length <= 0) {
                    throw new IllegalStateException("Cannot calculate shard hash");
                }
                log.debug("SHARD HASH = {}", hash.length);
                Long[] generatorIds = Convert.toObjectArray(generatorIdsExtractor.extractGeneratorIdsBefore(height, 3));
                CreateShardSchemaCommand createShardConstraintsCommand = new CreateShardSchemaCommand(shardId, shardEngine,
                        new ShardAddConstraintsSchemaVersion(), /*hash should be correct value*/ hash, generatorIds);
                this.addOperation(createShardConstraintsCommand);
            case SHARD_SCHEMA_FULL:
            case SECONDARY_INDEX_STARTED:
                excludeInfo = excludedTransactionDbIdExtractor.getExcludeInfo(height);
                UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand
                        (shardEngine, height, excludeInfo);
                this.addOperation(updateSecondaryIndexCommand);
            case SECONDARY_INDEX_FINISHED:
            case CSV_EXPORT_STARTED:
                excludeInfo = excludedTransactionDbIdExtractor.getExcludeInfo(height);
                List<String> tablesToExport = new ArrayList<>(derivedTablesRegistry.getDerivedTableNames());
                tablesToExport.addAll(List.of(ShardConstants.BLOCK_TABLE_NAME, ShardConstants.TRANSACTION_TABLE_NAME, ShardConstants.BLOCK_INDEX_TABLE_NAME, ShardConstants.TRANSACTION_INDEX_TABLE_NAME, ShardConstants.SHARD_TABLE_NAME));
                CsvExportCommand csvExportCommand = new CsvExportCommand(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, height, tablesToExport, excludeInfo);

                this.addOperation(csvExportCommand);
            case CSV_EXPORT_FINISHED:
            case ZIP_ARCHIVE_STARTED:
                ZipArchiveCommand zipArchiveCommand = new ZipArchiveCommand(shardId, shardEngine);
                this.addOperation(zipArchiveCommand);
            case ZIP_ARCHIVE_FINISHED:
            case DATA_REMOVE_STARTED:
                excludeInfo = excludedTransactionDbIdExtractor.getExcludeInfo(height);
                DeleteCopiedDataCommand deleteCopiedDataCommand =
                        new DeleteCopiedDataCommand(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, height, excludeInfo);
                this.addOperation(deleteCopiedDataCommand);
            case DATA_REMOVED_FROM_MAIN:
                FinishShardingCommand finishShardingCommand = new FinishShardingCommand(shardEngine, shardId);
                this.addOperation(finishShardingCommand);
                break;
            default:
                throw new IllegalArgumentException("Unable to create commands for state " + state);
        }

//        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(managementReceiver,height, dbIds);
//        this.addOperation(reLinkDataCommand);
    }

    private byte[] calculateHash(int height) {
        int lastShardHeight = getHeight();
        return shardHashCalculator.calculateHash(lastShardHeight, height);
    }

    private int getHeight() {
        Shard shard = shardDao.getLastCompletedShard();
        return shard != null ? shard.getShardHeight() + 1 : 0;
    }

    @Transactional
    public void cleanCommands() {
        dataMigrateOperations.clear();
    }

    public void addOperation(DataMigrateOperation shardOperation) {
        Objects.requireNonNull(shardOperation, "operation is NULL");
        log.debug("Add {}", shardOperation);
        dataMigrateOperations.add(shardOperation);
    }

    @Transactional
    public MigrateState executeAllOperations() {
        log.debug("START SHARDING...");
        MigrateState state = MigrateState.INIT;
        for (DataMigrateOperation dataMigrateOperation : dataMigrateOperations) {
            log.debug("Before execute {}", dataMigrateOperation);
            state = dataMigrateOperation.execute();
            log.debug("After execute step {} = '{}' before Fire Event...", dataMigrateOperation, state.name());
            migrateStateEvent.select(literal(state)).fire(state);
            if (state == MigrateState.FAILED) {
                log.warn("{} FAILED sharding...", dataMigrateOperation);
                break;
            }
        }
        log.debug("FINISHED SHARDING '{}'..", state);
        return state;
    }

    public MigrateState executeOperation(DataMigrateOperation shardOperation) {
        dataMigrateOperations.add(shardOperation);
        return shardOperation.execute();
    }

    private AnnotationLiteral<ShardChangeStateEvent> literal(MigrateState migrateState) {
        return new ShardChangeStateEventBinding() {
            @Override
            public MigrateState value() {
                return migrateState;
            }
        };
    }


}
