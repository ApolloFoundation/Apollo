/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.UUID;

public class ServiceModeDirProvider extends AbstractDirProvider {
    private static final String INSTALLATION_DIR = getInstallationDir();

    private static String getInstallationDir() {
        try {
            return Paths.get(ServiceModeDirProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public ServiceModeDirProvider(String applicationName, UUID chainId) {
        super(INSTALLATION_DIR, applicationName, chainId);
    }

    public ServiceModeDirProvider(String applicationName, UUID chainId, PredefinedDirLocations dirLocations) {
        super(INSTALLATION_DIR, applicationName, chainId, dirLocations);
    }
}
