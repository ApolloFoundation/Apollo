/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.config.Property;
import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
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
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private BlockIndexDao blockIndexDao;
    private ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor;
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
                                  BlockIndexDao blockIndexDao,
                                  ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor,
                                  @Property(value = "apl.sharding.backupDb", defaultValue = "false") boolean backupDb) {
        this.shardEngine = Objects.requireNonNull(shardEngine, "managementReceiver is NULL");
        this.migrateStateEvent = Objects.requireNonNull(migrateStateEvent, "migrateStateEvent is NULL");
        this.shardHashCalculator = Objects.requireNonNull(shardHashCalculator, "sharding hash calculator is NULL");
        this.blockIndexDao = Objects.requireNonNull(blockIndexDao, "blockIndexDao is NULL");
        this.excludedTransactionDbIdExtractor = Objects.requireNonNull(excludedTransactionDbIdExtractor, "exluded transaction db_id extractor is NULL");
        this.backupDb = backupDb;
    }

    @Transactional
    public void createAllCommands(int height) {
        if (backupDb) {
            log.info("Will backup db before sharding");
            BackupDbBeforeShardCommand beforeShardCommand = new BackupDbBeforeShardCommand(shardEngine);
            this.addOperation(beforeShardCommand);
        }

        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(shardEngine,
                new ShardInitTableSchemaVersion(), /*hash should be null here*/ null);
        this.addOperation(createShardSchemaCommand);
        Set<Long> dbIds = new HashSet<>(excludedTransactionDbIdExtractor.getDbIds(height));
        CopyDataCommand copyDataCommand = new CopyDataCommand(shardEngine, height, dbIds);
        this.addOperation(copyDataCommand);

        byte[] hash = calculateHash(height);
        if (hash == null || hash.length <= 0) {
            throw new IllegalStateException("Cannot calculate shard hash");
        }
        log.debug("SHARD HASH = {}", hash.length);
        CreateShardSchemaCommand createShardConstraintsCommand = new CreateShardSchemaCommand(shardEngine,
                new ShardAddConstraintsSchemaVersion(), /*hash should be correct value*/ hash);
        this.addOperation(createShardConstraintsCommand);

//        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(managementReceiver,height, dbIds);
//        this.addOperation(reLinkDataCommand);

        UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand
                (shardEngine, height, dbIds);
        this.addOperation(updateSecondaryIndexCommand);

        CsvExportCommand csvExportCommand = new CsvExportCommand(shardEngine, height, dbIds);
        this.addOperation(csvExportCommand);

        ZipArchiveCommand zipArchiveCommand = new ZipArchiveCommand(shardEngine);
        this.addOperation(zipArchiveCommand);

        DeleteCopiedDataCommand deleteCopiedDataCommand =
                new DeleteCopiedDataCommand(shardEngine, ShardConstants.DEFAULT_COMMIT_BATCH_SIZE, height, dbIds);
        this.addOperation(deleteCopiedDataCommand);

        FinishShardingCommand finishShardingCommand = new FinishShardingCommand(shardEngine);
        this.addOperation(finishShardingCommand);
    }

    private byte[] calculateHash(int height) {
        int lastShardHeight = getHeight();
        byte[] hash = shardHashCalculator.calculateHash(lastShardHeight, height);
        return hash;
    }

    private int getHeight() {
        Integer lastShardHeight = blockIndexDao.getLastHeight();
        return lastShardHeight != null ? lastShardHeight + 1 : 0;
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
                log.warn("FAILED sharding...", dataMigrateOperation);
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
