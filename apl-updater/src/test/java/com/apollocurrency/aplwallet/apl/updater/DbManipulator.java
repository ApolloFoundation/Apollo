/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.core.db.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DbManipulator {
    protected final DataSourceWrapper dataSourceWrapper =
        new DataSourceWrapper(DbProperties.builder().dbUrl("jdbc:h2:mem:test;MV_STORE=TRUE;CACHE_SIZE=16000").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000).build());

    private DbPopulator populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");

    public void init() throws SQLException {

        AplDbVersion dbVersion = new AplDbVersion();
        dataSourceWrapper.initWithJdbi(dbVersion);
        populator.initDb();
    }

    public void shutdown() throws Exception {
        dataSourceWrapper.shutdown();
    }

    public void populate() throws Exception {
        populator.populateDb();
    }

    public DataSource getDataSource() {
        return dataSourceWrapper;
    }
}
