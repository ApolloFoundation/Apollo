/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.firstbridge.kms.infrastructure.utils.StringUtils;
import lombok.ToString;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Singleton
@ToString
public class DbConfig {
    private PropertiesHolder propertiesHolder;
    private ChainsConfigHolder chainsConfigHolder;
    private DbProperties dbProperties;
    private Optional<String> kmsSchemaName = Optional.empty();

    @Inject
    public DbConfig(PropertiesHolder propertiesHolder, ChainsConfigHolder chainsConfigHolder) {
        this.propertiesHolder = propertiesHolder;
        this.chainsConfigHolder = chainsConfigHolder;
    }

    @Produces
    public DbProperties getDbProperties() {
        String dbName = Constants.APPLICATION_DB_NAME;
        DirProvider dp = RuntimeEnvironment.getInstance().getDirProvider();
        UUID chainId = chainsConfigHolder.getActiveChain().getChainId();

        if (this.dbProperties == null) {
            this.dbProperties = DbProperties.builder()
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
                .thresholdData(
                    DbProperties.ThresholdData.builder()
                        .stmtThreshold(propertiesHolder.getIntProperty("apl.statementLogThreshold", 1000))
                        .txThreshold(propertiesHolder.getIntProperty("apl.statementLogThreshold", 1000))
                        .txInterval(propertiesHolder.getIntProperty("apl.transactionLogInterval", 15) * 60 * 1000)
                        .enableSqlLogs(propertiesHolder.getBooleanProperty("apl.enableSqlLogs", false))
                    .build()
                )
                .build();
        }
        if (StringUtils.isBlank(this.dbProperties.getSystemDbUrl())) {
            String systemDbUrl = this.dbProperties.formatJdbcUrlString( true);
            this.dbProperties.setSystemDbUrl(systemDbUrl);
        }
        return this.dbProperties;
    }

    public PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
    }

    /**
     * Added to make CDI correctly work with Proxied class
     * @param propertiesHolder proxied cdi component
     */
    public void setPropertiesHolder(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    public ChainsConfigHolder getChainsConfigHolder() {
        return chainsConfigHolder;
    }

    /**
     * Added to make CDI correctly work with Proxied class
     * @param chainsConfigHolder proxied cdi component
     */
    public void setChainsConfigHolder(ChainsConfigHolder chainsConfigHolder) {
        this.chainsConfigHolder = chainsConfigHolder;
    }

    /**
     * Set value as sign on embedded mode
     * @param kmsSchemaName kms db schema name in embedded mode
     */
    public void setKmsSchemaName(String kmsSchemaName) {
        Objects.requireNonNull(kmsSchemaName, "kmsSchemaName is NULL");
        this.kmsSchemaName = Optional.of(kmsSchemaName);
    }

    public Optional<String> getKmsSchemaName() {
        return kmsSchemaName;
    }
}
