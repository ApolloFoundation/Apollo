/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import com.apollocurrency.aplwallet.apl.db.DbVersion;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import util.DbPopulator;

import java.sql.SQLException;

public class DbIntegrationTest {
    protected static final BasicDb db =
            new BasicDb(new BasicDb.DbProperties().dbUrl("jdbc:h2:mem:test").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000));

    private static DbPopulator populator = new DbPopulator(db, "db/schema.sql", "db/data.sql");

    @BeforeClass
    public static void before() throws SQLException {

        db.init(new DbVersion() {
            @Override
            protected void update(int nextUpdate) {
                // do nothing to prevent version db creation (FullTextTrigger exception), instead store db structure in db/schema.sql
            }
        });
        populator.initDb();
    }
    @AfterClass
    public static void tearDown() throws Exception {
        db.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        populator.populateDb();
    }
}
