/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PropertyBasedFileConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyBasedFileConfig.class);
    private final PropertiesHolder propertiesHolder;
    private final String oldDbPrefix;

    @Inject
    public PropertyBasedFileConfig(PropertiesHolder propertiesHolder, BlockchainConfig config) {
        this.propertiesHolder = propertiesHolder;
        this.oldDbPrefix = config.isTestnet() ? "apl.testDb" : "apl.db";
    }

    @Produces
    @Named("chainsConfigFilePath")
    public String getChainsConfigFileLocation() {
        return "conf/chains.json";
    }
    @Produces
    @Named("keystoreDirPath")
    public Path getKeystoreDirFilePath() {
        return AplCoreRuntime.getInstance().getVaultKeystoreDir().toAbsolutePath();
    }

    @Produces
    @Named("dbUser")
    public String getDbUser() {

        return propertiesHolder.getStringProperty(oldDbPrefix + "Username");
    }
    @Produces
    @Named("dbPassword")
    public String getDbPassword() {
        return propertiesHolder.getStringProperty(oldDbPrefix + "Password");
    }

    private String getOrDefault(String property, String defaultValue) {
        String value = propertiesHolder.getStringProperty(property, defaultValue);
        LOG.debug("{} - {}", property, value);
        return value;
    }
}
