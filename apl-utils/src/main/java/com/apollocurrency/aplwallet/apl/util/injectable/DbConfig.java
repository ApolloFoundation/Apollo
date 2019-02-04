/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.injectable;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;



import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

public class DbConfig {
    private PropertiesHolder propertiesHolder;
    private DirProvider dirProvider;
    
    @Inject
    public DbConfig(PropertiesHolder propertiesHolder, DirProvider dirProvider) {
        this.propertiesHolder = propertiesHolder;
        this.dirProvider = dirProvider;
    }

    @Produces
    public DbProperties getDbConfig() {
        DbProperties dbProperties = new DbProperties()
                .maxCacheSize(propertiesHolder.getIntProperty("apl.dbCacheKB"))
                .dbUrl(propertiesHolder.getStringProperty("apl.dbUrl"))
                .dbType(propertiesHolder.getStringProperty("apl.dbType"))
                .dbDir(dirProvider.getDbDir().toAbsolutePath().toString())
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
