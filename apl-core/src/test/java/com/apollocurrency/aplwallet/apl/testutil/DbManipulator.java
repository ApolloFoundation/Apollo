/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

import static org.slf4j.LoggerFactory.getLogger;


public class DbManipulator {
    private static final Logger logger = getLogger(DbManipulator.class);
    protected Path tempDbFile;
    protected final DatabaseManager databaseManager;

    private DbPopulator populator;

    public DbManipulator(Path dbFile) {
        this.tempDbFile = dbFile;
        DbProperties dbProperties = tempDbFile == null ? DbTestData.getInMemDbProps() : DbTestData.getDbFileProperties(tempDbFile.toAbsolutePath().toString());
        this.databaseManager = new DatabaseManagerImpl(dbProperties, new PropertiesHolder());
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
