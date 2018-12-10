package com.apollocurrency.aplwallet.apl.chainid;

import java.io.IOException;
import java.nio.file.Path;

public interface DbMigrator {
    /**
     * <p>
     * Migrate database from old location to the new location specified by 'targetDbDir'.
     * </p>
     * @param targetDbDir path to directory, whither current database should be migrated
     * @return path to the old database location or null when database for migration is not exist
     * @throws IOException when IO error occurred while db migration
     */
    Path migrate(String targetDbDir) throws IOException;
}
