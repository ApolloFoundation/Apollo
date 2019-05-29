/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.shard.commands.CommandParamInfo;

/**
 * Interface for different operation executed during sharding process.
 * Those are transferring data between main database and shard database, data export into csv file,
 * zipping csv into archive, removing copied data, updating shard table record.
 *
 * @author yuriy.larin
 */
public interface ShardEngine {

    /**
     * Downloading shard process in percent
     */
    Long SHARD_PERCENTAGE_FULL = 100L;

    DatabaseManager getDatabaseManager();

    MigrateState getCurrentState();

    /**
     * Create database BACKUP-BEFORE-0000001.zip file before sharding in case configured setting
     * apl.sharding.backupDb=true
     *
     * @return MigrateState.MAIN_DB_BACKUPED if success, MigrateState.FAILED otherwise
     */
    MigrateState createBackup();

    /**
     * Create either 'initial' shard db with tables only or full schema with all indexes/constrains/PK/FK
     *
     * @param dbVersion supplied schema name class
     * @return state enum - MigrateState.SHARD_SCHEMA_CREATED or MigrateState.SHARD_SCHEMA_FULL if success, MigrateState.FAILED otherwise
     */
    MigrateState addOrCreateShard(DbVersion dbVersion);

    /**
     * Copy block + transaction data excluding phased transaction into shard db
     *
     * @param paramInfo configured params
     * @return MigrateState.DATA_COPY_TO_SHARD_STARTED,
     */
    MigrateState copyDataToShard(CommandParamInfo paramInfo);

//    @Deprecated
//    MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo);

    MigrateState updateSecondaryIndex(CommandParamInfo paramInfo);

    MigrateState exportCsv(CommandParamInfo paramInfo);

    MigrateState archiveCsv(CommandParamInfo paramInfo);

    MigrateState deleteCopiedData(CommandParamInfo paramInfo);

    MigrateState addShardInfo(CommandParamInfo paramInfo);

}
