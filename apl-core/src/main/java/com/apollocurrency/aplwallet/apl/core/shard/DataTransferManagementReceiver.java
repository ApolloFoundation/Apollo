/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;

/**
 * Interface for different operation executed during sharding process.
 * Those are transferring data between main database and shard database, relinking data in main db, removing copied data.
 *
 * @author yuriy.larin
 */
public interface DataTransferManagementReceiver {

    /**
     * Common state overall shard migration process
     */
    String PREVIOUS_MIGRATION_KEY = "SHARD_MIGRATION_STATUS";
    /**
     * Last processed table/object within shard migration process
     */
    String LAST_MIGRATION_OBJECT_NAME = "LAST_MIGRATION_OBJECT_NAME";

    DatabaseManager getDatabaseManager();

    MigrateState getCurrentState();

    MigrateState addOrCreateShard(DbVersion dbVersion);

    MigrateState copyDataToShard(CommandParamInfo paramInfo);

    MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo);

    MigrateState updateSecondaryIndex(CommandParamInfo paramInfo);

    MigrateState deleteCopiedData(CommandParamInfo paramInfo);

    MigrateState addShardInfo(CommandParamInfo paramInfo);

}
