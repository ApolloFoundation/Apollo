/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

public class SelfInitializableDataSourceCreator implements DataSourceCreator {
    private final DatabaseAdministratorFactory dbAdminFactory;

    public SelfInitializableDataSourceCreator(DatabaseAdministratorFactory dbAdminFactory) {
        this.dbAdminFactory = dbAdminFactory;
    }

    @Override
    public TransactionalDataSource createDataSource(DbProperties dbProperties, DBUpdater dbUpdater) {
        DatabaseAdministrator dbAdmin = dbAdminFactory.createDbAdmin(dbProperties);
        SelfInitializableTransactionalDataSource dataSource = new SelfInitializableTransactionalDataSource(dbProperties, dbAdmin);
        dataSource.init();
        dataSource.update(dbUpdater);
        return dataSource;
    }

    private static class SelfInitializableTransactionalDataSource extends TransactionalDataSource {
        private final DatabaseAdministrator databaseAdministrator;

        public SelfInitializableTransactionalDataSource(DbProperties dbProperties, DatabaseAdministrator administrator) {
            super(dbProperties);
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
