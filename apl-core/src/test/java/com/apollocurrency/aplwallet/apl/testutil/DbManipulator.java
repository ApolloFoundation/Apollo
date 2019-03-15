/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import com.apollocurrency.aplwallet.apl.core.app.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.io.IOException;
import java.nio.file.Path;
import javax.sql.DataSource;

public class DbManipulator {
    protected Path tempDbFile;
    protected final TransactionalDataSource dataSourceWrapper;

    private DbPopulator populator;

    public DbManipulator(Path dbFile, String user, String password) {
        tempDbFile = dbFile;
        dataSourceWrapper =
                new TransactionalDataSource(new DbProperties()
                        .dbUrl(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0", tempDbFile.toAbsolutePath().toString()))
                        .dbPassword(password)
                        .dbUsername(user)
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(10000)
                        .defaultLockTimeout(10 * 1000), 1000, 1, 1000, false);
        populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");
    }


    public DbManipulator()  {
        dataSourceWrapper = new TransactionalDataSource(new DbProperties()
                        .dbUrl("jdbc:h2:mem:testdb")
                        .dbPassword("sa")
                        .dbUsername("sa")
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(10000)
                        .defaultLockTimeout(10 * 1000), 1000, 1, 1000, false);
        populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");
    }

    public void init() {
        AplDbVersion dbVersion = new AplDbVersion();
        dataSourceWrapper.init(dbVersion);
        populator.initDb();
    }

    public void shutdown() throws IOException {
        dataSourceWrapper.shutdown();
    }

    public Path getTempDbFile() {
        return tempDbFile;
    }

    public void populate() {
        populator.populateDb();
    }

    public DataSource getDataSource() {
        return dataSourceWrapper;
    }
}
