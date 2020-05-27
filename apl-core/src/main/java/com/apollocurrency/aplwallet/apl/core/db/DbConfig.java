/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DbConfig {
    private PropertiesHolder propertiesHolder;
    private ChainsConfigHolder chainsConfigHolder;

    @Inject
    public DbConfig(PropertiesHolder propertiesHolder, ChainsConfigHolder chainsConfigHolder) {
        this.propertiesHolder = propertiesHolder;
        this.chainsConfigHolder = chainsConfigHolder;
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
            .dbType(propertiesHolder.getStringProperty("apl.dbType"))
            .dbDir(dp != null ? dp.getDbDir().toAbsolutePath().toString() : "./unit-test-db") // for unit tests
            .dbFileName(dbFileName)
            .chainId(chainsConfigHolder.getActiveChain().getChainId())
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
