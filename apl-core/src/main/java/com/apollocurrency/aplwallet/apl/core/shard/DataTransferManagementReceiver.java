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
     * Downloading shard process in percent
     */
    Long SHARD_PERCENTAGE_FULL = 100L;

    DatabaseManager getDatabaseManager();

    MigrateState getCurrentState();

    MigrateState addOrCreateShard(DbVersion dbVersion);

    MigrateState copyDataToShard(CommandParamInfo paramInfo);

    MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo);

    MigrateState updateSecondaryIndex(CommandParamInfo paramInfo);

    MigrateState deleteCopiedData(CommandParamInfo paramInfo);

    MigrateState addShardInfo(CommandParamInfo paramInfo);

}
