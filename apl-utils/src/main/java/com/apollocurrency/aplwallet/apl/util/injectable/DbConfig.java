/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;



import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

public class DbConfig {
    private PropertiesHolder propertiesHolder;
    
    @Inject
    public DbConfig(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Produces
    public DbProperties getDbConfig() {
        DirProvider dp = RuntimeEnvironment.getInstance().getDirProvider();
        DbProperties dbProperties = new DbProperties()
                .maxCacheSize(propertiesHolder.getIntProperty("apl.dbCacheKB"))
                .dbUrl(propertiesHolder.getStringProperty("apl.dbUrl"))
                .dbType(propertiesHolder.getStringProperty("apl.dbType"))
                .dbDir(dp != null ? dp.getDbDir().toAbsolutePath().toString() : "./unit-test-db") // for unit tests
                .dbFileName(Constants.APPLICATION_DIR_NAME)
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
