/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import com.apollocurrency.aplwallet.apl.util.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import javax.sql.DataSource;

public class DbManipulator {
    protected final DataSourceWrapper dataSourceWrapper =
        new DataSourceWrapper(
            DbProperties.builder()
                .dbUrl("jdbc:h2:mem:test;MV_STORE=TRUE;CACHE_SIZE=16000").dbPassword("").dbUsername("sa").maxConnections(10).loginTimeout(10).maxMemoryRows(100000).defaultLockTimeout(10 * 1000).build());

    public DataSource getDataSource() {
        return dataSourceWrapper;
    }
}
