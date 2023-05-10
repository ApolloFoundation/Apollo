/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.MigrationParams;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

public class H2DatabaseAdministrator implements DatabaseAdministrator {
    private final DbProperties dbProperties;

    public H2DatabaseAdministrator(DbProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

    @Override
    public synchronized void startDatabase() {

    }

    @Override
    public synchronized void deleteDatabase() {

    }

    @Override
    public synchronized String createDatabase() { // database will be created automatically
        return dbUrl();
    }

    @Override
    public synchronized void migrateDatabase(DBUpdater dbUpdater) {
        dbUpdater.update(new MigrationParams(dbUrl(), dbProperties.getDbType(), dbProperties.getDbUsername(), dbProperties.getDbPassword()));
    }

    @Override
    public synchronized void stopDatabase() {

    }

    private String dbUrl() {
        if (StringUtils.isNotBlank(dbProperties.getDbUrl())) { // do not try to replace url
            return dbProperties.getDbUrl();
        }
        return dbProperties.formatEmbeddedJdbcUrlString();
    }
}
