/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import com.apollocurrency.aplwallet.apl.db.DbVersion;

public class DbManipulator {
    protected final BasicDb db =
            new BasicDb(new BasicDb.DbProperties().dbUrl("jdbc:h2:mem:test").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000));

    private DbPopulator populator = new DbPopulator(db, "db/schema.sql", "db/data.sql");

    public void init() throws SQLException {

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
    }

    public void populate() throws Exception {
        populator.populateDb();
    }

    public BasicDb getDb() {
        return db;
    }
}
