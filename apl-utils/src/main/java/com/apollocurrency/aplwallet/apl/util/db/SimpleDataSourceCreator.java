/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.MigrationParams;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class SimpleDataSourceCreator implements DataSourceCreator {
    private final PropertiesHolder propertiesHolder;

    public SimpleDataSourceCreator(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Override
    public TransactionalDataSource createDataSource(DbProperties dbProperties, DBUpdater dbUpdater) {
        TransactionalDataSource dataSource = new TransactionalDataSource(dbProperties, propertiesHolder);
        dbProperties.setDbUrl(dbProperties.formatJdbcUrlString(false));
        dataSource.init();
        dbUpdater.update(new MigrationParams(dbProperties.getDbUrl(), dbProperties.getDbType(), dbProperties.getDbUsername(), dbProperties.getDbPassword()));
        return dataSource;
    }
}
