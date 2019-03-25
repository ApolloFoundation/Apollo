/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;


public class DbManipulator {
    private static final Logger logger = getLogger(DbManipulator.class);
    protected Path tempDbFile;
    protected DatabaseManager databaseManager;

    private DbPopulator populator;

    public DbManipulator(Path dbFile) {
        this(dbFile == null ? DbTestData.getInMemDbProps() : DbTestData.getDbFileProperties(dbFile.toAbsolutePath().toString()), null);
        this.tempDbFile = dbFile;
    }

    public DbManipulator(DbProperties dbProperties, PropertiesHolder propertiesHolder) {
        Objects.requireNonNull(dbProperties, "dbProperties is NULL");
        this.databaseManager = new DatabaseManagerImpl(dbProperties, propertiesHolder == null ? new PropertiesHolder() : propertiesHolder);
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
