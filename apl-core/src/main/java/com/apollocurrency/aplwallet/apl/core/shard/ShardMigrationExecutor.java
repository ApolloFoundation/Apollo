package com.apollocurrency.aplwallet.apl.core.shard;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;

@Singleton
public class ShardMigrationExecutor {

    private final List<DataMigrateOperation> dataMigrateOperations = new ArrayList<>();

    public ShardMigrationExecutor() {
    }

    public MigrateState executeOperation(DataMigrateOperation shardOperation) {
        dataMigrateOperations.add(shardOperation);
        return shardOperation.execute();
    }

}
