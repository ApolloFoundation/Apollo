/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class SelfInitializableDataSourceCreator implements DataSourceCreator {
    private final DatabaseAdministratorFactory dbAdminFactory;
    private final PropertiesHolder propertiesHolder;

    public SelfInitializableDataSourceCreator(DatabaseAdministratorFactory dbAdminFactory, PropertiesHolder propertiesHolder) {
        this.dbAdminFactory = dbAdminFactory;
        this.propertiesHolder = propertiesHolder;
    }

    @Override
    public TransactionalDataSource createDataSource(DbProperties dbProperties, DBUpdater dbUpdater) {
        DatabaseAdministrator dbAdmin = dbAdminFactory.createDbAdmin(dbProperties);
        SelfInitializableTransactionalDataSource dataSource = new SelfInitializableTransactionalDataSource(dbProperties, dbAdmin, propertiesHolder);
        dataSource.init();
        dataSource.update(dbUpdater);
        return dataSource;
    }

    private static class SelfInitializableTransactionalDataSource extends TransactionalDataSource {
        private final DatabaseAdministrator databaseAdministrator;

        public SelfInitializableTransactionalDataSource(DbProperties dbProperties, DatabaseAdministrator administrator, PropertiesHolder propertiesHolder) {
            super(dbProperties, propertiesHolder);
            this.databaseAdministrator = administrator;
        }

        @Override
        public void init() {
            databaseAdministrator.startDatabase();
            String url = databaseAdministrator.createDatabase();
            dbProperties.setDbUrl(url);
            super.init();
        }

        @Override
        public void update(DBUpdater dbUpdater) {
            databaseAdministrator.migrateDatabase(dbUpdater);
        }

    }
}
