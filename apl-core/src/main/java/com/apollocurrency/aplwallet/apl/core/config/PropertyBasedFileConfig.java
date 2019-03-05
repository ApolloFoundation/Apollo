/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

public class PropertyBasedFileConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyBasedFileConfig.class);
    private final PropertiesHolder propertiesHolder;

    @Inject
    public PropertyBasedFileConfig(PropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    @Produces
    @Named("keystoreDirPath")
    public Path getKeystoreDirFilePath() {
        return AplCoreRuntime.getInstance().getVaultKeystoreDir().toAbsolutePath();
    }



    private String getOrDefault(String property, String defaultValue) {
        String value = propertiesHolder.getStringProperty(property, defaultValue);
        LOG.debug("{} - {}", property, value);
        return value;
    }
}
