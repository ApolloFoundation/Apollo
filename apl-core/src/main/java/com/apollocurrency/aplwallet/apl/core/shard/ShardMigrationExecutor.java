/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apollocurrency.aplwallet.apl.core.db.ShardAddConstraintsSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CopyDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CreateShardSchemaCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DeleteCopiedDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.FinishShardingCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.ReLinkDataCommand;
import com.apollocurrency.aplwallet.apl.core.shard.commands.UpdateSecondaryIndexCommand;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEvent;
import com.apollocurrency.aplwallet.apl.core.shard.observer.events.ShardChangeStateEventBinding;
import org.slf4j.Logger;

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

    @Inject
    public ShardMigrationExecutor(DataTransferManagementReceiver managementReceiver,
                                  javax.enterprise.event.Event<MigrateState> migrateStateEvent) {
        this.managementReceiver = Objects.requireNonNull(managementReceiver, "managementReceiver is NULL");
        this.migrateStateEvent = Objects.requireNonNull(migrateStateEvent, "migrateStateEvent is NULL");
    }

    public void createAllCommands(int height) {
        CreateShardSchemaCommand createShardSchemaCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardInitTableSchemaVersion());
        this.addOperation(createShardSchemaCommand);
        CopyDataCommand copyDataCommand = new CopyDataCommand(managementReceiver, height);
        this.addOperation(copyDataCommand);
        CreateShardSchemaCommand createShardConstraintsCommand = new CreateShardSchemaCommand(managementReceiver,
                new ShardAddConstraintsSchemaVersion());
        this.addOperation(createShardConstraintsCommand);
        ReLinkDataCommand reLinkDataCommand = new ReLinkDataCommand(managementReceiver,height);
        this.addOperation(reLinkDataCommand);
        UpdateSecondaryIndexCommand updateSecondaryIndexCommand = new UpdateSecondaryIndexCommand
                (managementReceiver, height);
        this.addOperation(updateSecondaryIndexCommand);
        DeleteCopiedDataCommand deleteCopiedDataCommand = new DeleteCopiedDataCommand(managementReceiver, height);
        this.addOperation(deleteCopiedDataCommand);

        // TODO: YL replace by real value OR by real call
        FinishShardingCommand finishShardingCommand = new FinishShardingCommand(managementReceiver, new byte[]{3,4,5,6,1});
        this.addOperation(finishShardingCommand);

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
        MigrateState state = MigrateState.INIT;
        for (DataMigrateOperation dataMigrateOperation : dataMigrateOperations) {
            log.debug("Before execute {}", dataMigrateOperation);
            state = dataMigrateOperation.execute();;
            log.debug("After execute step {} = '{}' before Fire Event...", dataMigrateOperation, state.name());
            migrateStateEvent.select(literal(state)).fire(state);
            if (state == MigrateState.FAILED) {
                log.warn("FAILED sharding...", dataMigrateOperation);
                break;
            }
        }
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
