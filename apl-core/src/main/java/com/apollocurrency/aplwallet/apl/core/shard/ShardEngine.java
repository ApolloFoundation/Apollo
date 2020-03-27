/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

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
     * Downloading shard process in percent (field is not used now)
     */


    MigrateState getCurrentState();

    /**
     * Create database BACKUP-BEFORE-0000001.zip file before sharding in case configured setting
     * apl.sharding.backupDb=f
     *
     * @return MigrateState.MAIN_DB_BACKUPED if success, MigrateState.FAILED otherwise
     */
    MigrateState createBackup();

    /**
     * Create either 'initial' shard db with tables only or full schema with all indexes/constrains/PK/FK
     *
     * @param dbVersion supplied schema name class
     * @param paramInfo require for FULL schema. It's a shard data HASH, also supply shard id
     * @return state enum - MigrateState.SHARD_SCHEMA_CREATED or MigrateState.SHARD_SCHEMA_FULL if success, MigrateState.FAILED otherwise
     */
    MigrateState addOrCreateShard(DbVersion dbVersion, CommandParamInfo paramInfo);

    /**
     * Copy block + transaction data excluding phased transaction into shard db
     *
     * @param paramInfo configured params
     * @return MigrateState.DATA_COPY_TO_SHARD_STARTED,
     */
    MigrateState copyDataToShard(CommandParamInfo paramInfo);

//    @Deprecated
//    MigrateState relinkDataToSnapshotBlock(CommandParamInfo paramInfo);

    /**
     * BLOCK_INDEX and TRANSACTION_SHARD_INDEX tables are filled with necessary information in main db
     * on that step
     *
     * @param paramInfo table name list, block height
     * @return SECONDARY_INDEX_FINISHED or FAILED state
     */
    MigrateState updateSecondaryIndex(CommandParamInfo paramInfo);

    /**
     * Trim and export all derived table list (+ more) into CSV
     *
     * @param paramInfo block height
     * @return CSV_EXPORT_FINISHED or FAILED
     */
    MigrateState exportCsv(CommandParamInfo paramInfo);

    /**
     * Archive all csv files into zip and compute CRC internally
     *
     * @param paramInfo empty, left for compatibility mostly
     * @return ZIP_ARCHIVE_FINISHED or FAILED
     */
    MigrateState archiveCsv(CommandParamInfo paramInfo);

    /**
     * Delete block + transaction data in main db after it was copied into shard db
     *
     * @param paramInfo table list
     * @return DATA_REMOVED_FROM_MAIN or FAILED
     */
    MigrateState deleteCopiedData(CommandParamInfo paramInfo);

    /**
     * Remove recovery data, so the process is finished and ready for next time
     * @param paramInfo empty, left for compatibility
     * @return COMPLETED usually
     */
    MigrateState finishShardProcess(CommandParamInfo paramInfo);

}
