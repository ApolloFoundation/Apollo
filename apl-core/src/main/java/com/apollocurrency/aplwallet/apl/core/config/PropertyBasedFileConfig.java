/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;

public class PropertyBasedFileConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyBasedFileConfig.class);
    private final PropertiesHolder propertiesHolder;
    private final DirProvider dirProvider;

    @Inject
    public PropertyBasedFileConfig(PropertiesHolder propertiesHolder, DirProvider dirProvider) {
        this.propertiesHolder = propertiesHolder;
        this.dirProvider = dirProvider;
    }

    @Produces
    @Named("keystoreDirPath")
    public Path getKeystoreDirFilePath() {
        return dirProvider.getVaultKeystoreDir().toAbsolutePath();
    }

    @Produces
    @Named("secureStoreDirPath")
    public Path getSecureStoreDirPath() {
        return dirProvider.getSecureStorageDir().toAbsolutePath();
    }


    private String getOrDefault(String property, String defaultValue) {
        String value = propertiesHolder.getStringProperty(property, defaultValue);
        LOG.debug("{} - {}", property, value);
        return value;
    }
}
