/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import javax.enterprise.inject.Produces;
import java.util.UUID;

/**
 * Simple factory for DirProvider creation
 */
public class DirProviderFactory {
    private static boolean isService;
    private static String applicationName;
    private static PredefinedDirLocations dirLocations;
    private static UUID chainId;

    public static void setup(boolean isServiceP, UUID chainIdP, String applicationNameP, PredefinedDirLocations dirLocationsP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        chainId = chainIdP;
        dirLocations = dirLocationsP;
    }

    @Produces
    public static DirProvider getProvider() {
        if (!isService) {
            return new UserModeDirProvider(applicationName, chainId, dirLocations);
        } else if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            return new UnixServiceModeDirProvider(applicationName, chainId, dirLocations);
        } else return new ServiceModeDirProvider(applicationName, chainId, dirLocations);
    }
}
