package com.apollocurrency.aplwallet.apl.chainid;

import java.io.IOException;

public interface ChainIdDbMigrator {
    void migrate(String targetDbDir, boolean deleteOldDb) throws IOException;
}
