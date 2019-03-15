/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.io.IOException;
import java.nio.file.Path;

public class DbManipulator {
    protected Path tempDbFile;
    protected final DatabaseManager databaseManager;

    private DbPopulator populator;

    public DbManipulator(Path dbFile) {
        this.tempDbFile = dbFile;
        this.databaseManager = new DatabaseManager(tempDbFile == null ? DbTestData.DB_MEM_PROPS : DbTestData.getDbFileProperties(tempDbFile.toAbsolutePath().toString()), new PropertiesHolder());
        this.populator = new DbPopulator(databaseManager.getDataSource(), "db/schema.sql", "db/data.sql");
    }


    public DbManipulator()  {
        this(null);
    }

    public void init() {
        populator.initDb();
    }

    public void shutdown() throws IOException {
        databaseManager.shutdown();
    }

    public Path getTempDbFile() {
        return tempDbFile;
    }

    public void populate() {
        populator.populateDb();
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
