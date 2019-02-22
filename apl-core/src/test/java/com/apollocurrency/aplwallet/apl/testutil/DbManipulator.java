/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import javax.sql.DataSource;

import com.apollocurrency.aplwallet.apl.core.app.AplDbVersion;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;

public class DbManipulator {
    protected final Path tempDbFile;
    protected final DataSourceWrapper dataSourceWrapper;

    private DbPopulator populator;

    public DbManipulator(Path dbFile, String user, String password) throws IOException {
        tempDbFile = dbFile;
        dataSourceWrapper =
                new DataSourceWrapper(new DbProperties()
                        .dbUrl(String.format("jdbc:h2:%s", tempDbFile.toAbsolutePath().toString()))
                        .dbPassword(password)
                        .dbUsername(user)
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(100000)
                        .defaultLockTimeout(10 * 1000));
        populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");
    }


    public DbManipulator()  {
        tempDbFile = createTempFile();
        dataSourceWrapper = new DataSourceWrapper(new DbProperties()
                        .dbUrl(String.format("jdbc:h2:%s", tempDbFile.toAbsolutePath().toString()))
                        .dbPassword("sa")
                        .dbUsername("sa")
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(100000)
                        .defaultLockTimeout(10 * 1000));
        populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");
    }

    private Path createTempFile() {
        try {
            return Files.createTempFile("testdb", "h2");
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void init() {
        AplDbVersion dbVersion = new AplDbVersion();
        dataSourceWrapper.init(dbVersion);
        populator.initDb();
    }

    public void shutdown() throws IOException {
        dataSourceWrapper.shutdown();
        Files.deleteIfExists(Paths.get(tempDbFile.toAbsolutePath().toString() + ".h2.db"));
    }

    public void populate() {
        populator.populateDb();
    }

    public DataSource getDataSource() {
        return dataSourceWrapper;
    }
}
