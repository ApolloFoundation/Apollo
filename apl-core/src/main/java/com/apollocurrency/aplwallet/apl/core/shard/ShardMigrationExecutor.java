/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation.DEFAULT_COMMIT_BATCH_SIZE;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ReLinkDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
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
    private DataTransferManagementReceiver managementReceiver;
    private ShardHashCalculator shardHashCalculator;
    private BlockIndexDao blockIndexDao;
    private ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor;

    @Inject
    public ShardMigrationExecutor(DataTransferManagementReceiver managementReceiver,
                                  javax.enterprise.event.Event<MigrateState> migrateStateEvent,
                                  ShardHashCalculator shardHashCalculator,
                                  BlockIndexDao blockIndexDao,
                                  ExcludedTransactionDbIdExtractor excludedTransactionDbIdExtractor) {
        this.managementReceiver = Objects.requireNonNull(managementReceiver, "managementReceiver is NULL");
        this.migrateStateEvent = Objects.requireNonNull(migrateStateEvent, "migrateStateEvent is NULL");
        this.shardHashCalculator = Objects.requireNonNull(shardHashCalculator, "sharding hash calculator is NULL");
        this.blockIndexDao = Objects.requireNonNull(blockIndexDao, "blockIndexDao is NULL");
        this.excludedTransactionDbIdExtractor = Objects.requireNonNull(excludedTransactionDbIdExtractor, "exluded transaction db_id extractor is NULL");
    }

    public void createAllCommands(int height) {
        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardInitTableSchemaVersion());
        this.addOperation(createShardSchemaCommand);
        Set<Long> dbIds = new HashSet<>(excludedTransactionDbIdExtractor.getDbIds(height));

        CopyDataCommand copyDataCommand = new CopyDataCommand(managementReceiver, height, dbIds);
        this.addOperation(copyDataCommand);

        CreateShardSchemaCommand createShardConstraintsCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardAddConstraintsSchemaVersion());
        this.addOperation(createShardConstraintsCommand);

        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(managementReceiver,height, dbIds);
        this.addOperation(reLinkDataCommand);

        UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand
                (managementReceiver, height, dbIds);
        this.addOperation(updateSecondaryIndexCommand);

        DeleteCopiedDataCommand deleteCopiedDataCommand =
                new DeleteCopiedDataCommand(managementReceiver, DEFAULT_COMMIT_BATCH_SIZE * 5, height);
        this.addOperation(deleteCopiedDataCommand);

        byte[] hash = calculateHash(height);
        if (hash == null) {
            throw new IllegalStateException("Cannot calculate shard hash");
        }

        log.debug("SHARD HASH = {}", hash.length);
        FinishShardingCommand finishShardingCommand = new FinishShardingCommand(managementReceiver, hash);
        this.addOperation(finishShardingCommand);
    }

    private byte[] calculateHash(int height) {
        int lastShardHeight = getHeight();
        byte[] hash = shardHashCalculator.calculateHash(lastShardHeight + 1, height);
        return hash;
    }

    private int getHeight() {
        Integer lastShardHeight = blockIndexDao.getLastHeight();
        return lastShardHeight != null ? lastShardHeight + 1 : 0;
    }

    public void cleanCommands() {
        dataMigrateOperations.clear();
    }

    public void addOperation(DataMigrateOperation shardOperation) {
        Objects.requireNonNull(shardOperation, "operation is NULL");
        log.debug("Add {}", shardOperation);
        dataMigrateOperations.add(shardOperation);
    }

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
