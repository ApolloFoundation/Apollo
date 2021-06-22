/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

public class H2DatabaseAdministrator implements DatabaseAdministrator {
    private final DbProperties dbProperties;

    public H2DatabaseAdministrator(DbProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

    @Override
    public void startDatabase() {

    }

    @Override
    public void deleteDatabase() {

    }

    @Override
    public void createDatabase() {
        dbProperties.setDbUrl(dbProperties.formatEmbeddedJdbcUrlString());
    }

    @Override
    public void migrateDatabase(DBUpdater dbUpdater) {
        dbUpdater.update(dbProperties.formatJdbcUrlString(false), dbProperties.getDbUsername(), dbProperties.getDbPassword());
    }

    @Override
    public void stopDatabase() {

    }
}
