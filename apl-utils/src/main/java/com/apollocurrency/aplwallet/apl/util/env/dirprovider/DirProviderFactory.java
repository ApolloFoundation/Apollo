/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.util.UUID;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

/**
 * Simple factory for DirProvider creation
 */
public class DirProviderFactory {
    public static DirProvider getProvider(boolean isService, UUID chainId, String applicationName, PredefinedDirLocations dirLocations) {
        if (!isService) {
            return new UserModeDirProvider(applicationName, chainId, dirLocations);
        } else if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            return new UnixServiceModeDirProvider(applicationName, chainId, dirLocations);
        } else return new ServiceModeDirProvider(applicationName, chainId, dirLocations);
    }
}
