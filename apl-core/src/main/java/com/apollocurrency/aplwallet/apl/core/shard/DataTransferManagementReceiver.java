package com.apollocurrency.aplwallet.apl.core.shard;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;

/**
 * Interface + Implementation for transferring data between main database and shard database.
 * Also renaming database files.
 */
public interface DataTransferManagementReceiver {

    String TEMPORARY_MIGRATION_FILE_NAME = "apl-temp-migration";

    DatabaseManager getDatabaseManager();

    MigrateState getCurrentState();

    MigrateState createTempDb(DatabaseMetaInfo source);

    MigrateState moveData(DatabaseMetaInfo source, DatabaseMetaInfo target);

    MigrateState renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target);

}
