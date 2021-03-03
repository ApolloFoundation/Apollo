/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.regex.Matcher;


@Singleton
public class DbConfig {
    private final PropertiesHolder propertiesHolder;
    private final ChainsConfigHolder chainsConfigHolder;

    @Inject
    public DbConfig(PropertiesHolder propertiesHolder, ChainsConfigHolder chainsConfigHolder) {
        this.propertiesHolder = propertiesHolder;
        this.chainsConfigHolder = chainsConfigHolder;
    }

    @Produces
    public DbProperties getDbConfig() {
        String dbName = Constants.APPLICATION_DB_NAME;
        DirProvider dp = RuntimeEnvironment.getInstance().getDirProvider();
        UUID chainId = chainsConfigHolder.getActiveChain().getChainId();

        DbProperties dbProperties =  DbProperties.builder()
            .dbType(propertiesHolder.getStringProperty("apl.dbType"))
            .dbUrl(propertiesHolder.getStringProperty("apl.dbUrl"))
            .dbDir(dp != null ? dp.getDbDir().toAbsolutePath().toString() : "./unit-test-db") // for unit tests
            .dbName(dbName.concat("_".concat(chainId.toString().substring(0, 6))))
            .chainId(chainId)
            .dbParams(propertiesHolder.getStringProperty("apl.dbParams"))
            .dbUsername(propertiesHolder.getStringProperty("apl.dbUsername"))
            .dbPassword(propertiesHolder.getStringProperty("apl.dbPassword", null, true))
            .maxConnections(propertiesHolder.getIntProperty("apl.maxDbConnections"))
            .loginTimeout(propertiesHolder.getIntProperty("apl.dbLoginTimeout"))
            .defaultLockTimeout(propertiesHolder.getIntProperty("apl.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(propertiesHolder.getIntProperty("apl.dbMaxMemoryRows"))
            .databaseHost(propertiesHolder.getStringProperty("apl.databaseHost"))
            .databasePort(propertiesHolder.getIntProperty("apl.databasePort"))
            .build();
        if (StringUtils.isBlank(dbProperties.getSystemDbUrl())) {
            String systemDbUrl = dbProperties.formatJdbcUrlString( true);
            dbProperties.setSystemDbUrl(systemDbUrl);
        } 
       
        return dbProperties;
    }
}
