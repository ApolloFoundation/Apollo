/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class DbConfig {
    private PropertiesHolder propertiesHolder;

    @Inject
    public DbConfig(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Produces
    public DbProperties getDbConfig() {
        PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        DbProperties dbProperties = new DbProperties()
                .maxCacheSize(propertiesHolder.getIntProperty("apl.dbCacheKB"))
                .dbUrl(propertiesHolder.getStringProperty("apl.dbUrl"))
                .dbType(propertiesHolder.getStringProperty("apl.dbType"))
                .dbDir(AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString())
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
