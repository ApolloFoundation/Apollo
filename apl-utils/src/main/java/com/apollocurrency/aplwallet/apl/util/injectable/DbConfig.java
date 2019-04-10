/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class DbConfig {
    private PropertiesHolder propertiesHolder;

    @Inject
    public DbConfig(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Produces
    public DbProperties getDbConfig() {
//        String customDbDir = propertiesHolder.getStringProperty("apl.customDbDir");
        String dbFileName = Constants.APPLICATION_DIR_NAME;
//        if (!StringUtils.isBlank(customDbDir)) {
//            dbFileName = propertiesHolder.getStringProperty("apl.dbName");
//        }
        DirProvider dp = RuntimeEnvironment.getInstance().getDirProvider();
        DbProperties dbProperties = new DbProperties()
                .maxCacheSize(propertiesHolder.getIntProperty("apl.dbCacheKB"))
                .dbUrl(propertiesHolder.getStringProperty("apl.dbUrl"))
                .dbType(propertiesHolder.getStringProperty("apl.dbType"))
                .dbDir(dp != null ? dp.getDbDir().toAbsolutePath().toString() : "./unit-test-db") // for unit tests
                .dbFileName(dbFileName)
                .dbParams(propertiesHolder.getStringProperty("apl.dbParams"))
                .dbUsername(propertiesHolder.getStringProperty("apl.dbUsername"))
                .dbPassword(propertiesHolder.getStringProperty("apl.dbPassword", null, true))
                .maxConnections(propertiesHolder.getIntProperty("apl.maxDbConnections"))
                .loginTimeout(propertiesHolder.getIntProperty("apl.dbLoginTimeout"))
                .defaultLockTimeout(propertiesHolder.getIntProperty("apl.dbDefaultLockTimeout") * 1000)
                .maxMemoryRows(propertiesHolder.getIntProperty("apl.dbMaxMemoryRows")
                );
        return dbProperties;
    }
}
