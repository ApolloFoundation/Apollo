/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard;

import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.shard.commands.DatabaseMetaInfo;

/**
 * Interface for different operation executed during sharding process.
 * Those are transferring data between main database and shard database, relinking data in main db, removing copied data.
 *
 * @author yuriy.larin
 */
public interface DataTransferManagementReceiver {

    String TEMPORARY_MIGRATION_FILE_NAME = "apl-temp-migration";
    String PREVIOUS_MIGRATION_KEY = "SHARD_MIGRATION_STATUS";
    String LAST_MIGRATION_OBJECT_NAME = "LAST_MIGRATION_OBJECT_NAME";

//    Map<String, Long> getTableNameWithCountMap();

    DatabaseManager getDatabaseManager();

    MigrateState getCurrentState();

    MigrateState addOrCreateShard(DatabaseMetaInfo source, DbVersion dbVersion);

//    MigrateState createTempDb(DatabaseMetaInfo source);

//    MigrateState addSnapshotBlock(DatabaseMetaInfo targetDataSource);

    MigrateState moveDataBlockLinkedData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target);

    MigrateState moveData(Map<String, Long> tableNameCountMap, DatabaseMetaInfo source, DatabaseMetaInfo target);

//    MigrateState renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target);

}
