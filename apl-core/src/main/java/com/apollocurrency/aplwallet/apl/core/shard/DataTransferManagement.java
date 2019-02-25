package com.apollocurrency.aplwallet.apl.core.shard;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Interface + Implementation for transferring data between main database and shard database.
 * Also renaming database files.
 */
public interface DataTransferManagement {

    String TEMPORARY_MIGRATION_FILE_NAME = "apl-temp-shard-migration";

    MigrateState getCurrentState();

    long moveData(DatabaseMetaInfo source, DatabaseMetaInfo target) throws SQLException;

    boolean renameDataFiles(DatabaseMetaInfo source, DatabaseMetaInfo target) throws IOException;

}
