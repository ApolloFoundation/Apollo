/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import javax.sql.DataSource;
import com.apollocurrency.aplwallet.apl.core.app.AplDbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.sql.SQLException;

public class DbManipulator {
    protected final DataSourceWrapper dataSourceWrapper =
            new DataSourceWrapper(new DbProperties().dbUrl("jdbc:h2:mem:test").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000));

    private DbPopulator populator = new DbPopulator(dataSourceWrapper, "db/schema.sql", "db/data.sql");

    public void init() throws SQLException {

        AplDbVersion dbVersion = new AplDbVersion();
        dataSourceWrapper.init(dbVersion);
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
