/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.shard.commands.DataMigrateOperation;

/**
 * Component for starting sharding process which contains several steps/states.
 *
 * @author yuriy.larin
 */
@Singleton
public class ShardMigrationExecutor {

    private final List<DataMigrateOperation> dataMigrateOperations = new LinkedList<>();

    public ShardMigrationExecutor() {
    }

    public MigrateState executeOperation(DataMigrateOperation shardOperation) {
        dataMigrateOperations.add(shardOperation);
        return shardOperation.execute();
    }

}
