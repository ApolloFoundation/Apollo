/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Paths;

public class DefaultConfigDirProvider implements ConfigDirProvider {
    protected String applicationName;
    protected boolean isService;

    public DefaultConfigDirProvider(String applicationName, boolean isService) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;
    }

    @Override
    public String getInstallationConfigDirectory() {
        return this.getClass().getClassLoader().getResource("").getPath() + "conf";
    }

    @Override
    public String getSysConfigDirectory() {
        return getInstallationConfigDirectory();
    }

    @Override
    public String getUserConfigDirectory() {
        return isService
                ? getInstallationConfigDirectory()
                : Paths.get(System.getProperty("user.home"), applicationName, "conf").toAbsolutePath().toString();
    }
}
