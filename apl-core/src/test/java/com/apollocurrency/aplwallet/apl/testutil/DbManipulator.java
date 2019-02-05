/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.testutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.core.db.BasicDb;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;

public class DbManipulator {
    protected final Path tempDbFile;
    protected final BasicDb db;

    private DbPopulator populator;

    public DbManipulator(Path dbFile, String user, String password) throws IOException {
        tempDbFile = dbFile;
        db =
                new BasicDb(new DbProperties()
                        .dbUrl(String.format("jdbc:h2:%s", tempDbFile.toAbsolutePath().toString()))
                        .dbPassword(password)
                        .dbUsername(user)
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(100000)
                        .defaultLockTimeout(10 * 1000));
        populator = new DbPopulator(db, "db/schema.sql", "db/data.sql");
    }


    public DbManipulator()  {
        tempDbFile = createTempFile();
        db = new BasicDb(new DbProperties()
                        .dbUrl(String.format("jdbc:h2:%s", tempDbFile.toAbsolutePath().toString()))
                        .dbPassword("sa")
                        .dbUsername("sa")
                        .maxConnections(10)
                        .loginTimeout(10)
                        .maxMemoryRows(100000)
                        .defaultLockTimeout(10 * 1000));
        populator = new DbPopulator(db, "db/schema.sql", "db/data.sql");
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

        db.init(new DbVersion() {
            @Override
            protected void update(int nextUpdate) {
                // do nothing to prevent version db creation (FullTextTrigger exception), instead store db structure in db/schema.sql
            }
        });
        populator.initDb();
    }

    public void shutdown() throws Exception {
        db.shutdown();
        Files.deleteIfExists(Paths.get(tempDbFile.toAbsolutePath().toString() + ".h2.db"));
    }

    public void populate() {
        populator.populateDb();
    }

    public BasicDb getDb() {
        return db;
    }
}
