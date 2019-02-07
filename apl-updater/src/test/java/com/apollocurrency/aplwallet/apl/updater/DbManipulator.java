/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import javax.sql.DataSource;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.BasicDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;

public class DbManipulator {
    protected final BasicDataSource basicDataSource =
            new BasicDataSource(new DbProperties().dbUrl("jdbc:h2:mem:test").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000));

    private DbPopulator populator = new DbPopulator(basicDataSource, "db/schema.sql", "db/data.sql");

    public void init() throws SQLException {

        basicDataSource.init(new DbVersion() {
            @Override
            protected void update(int nextUpdate, boolean initFullTextSearch) {
                // do nothing to prevent version db creation (FullTextTrigger exception), instead store db structure in db/schema.sql
            }
        });
        populator.initDb();
    }
    public void shutdown() throws Exception {
        basicDataSource.shutdown();
    }

    public void populate() throws Exception {
        populator.populateDb();
    }

    public DataSource getDataSource() {
        return basicDataSource;
    }
}
