/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.util.UUID;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

public class DirProviderFactory {
    public DirProvider getInstance(boolean isService, UUID chainId, String applicationName, PredefinedDirLocations dirLocations) {
        if (!isService) {
            return new UserModeDirProvider(applicationName, chainId, dirLocations);
        } else if (RuntimeEnvironment.isUnixRuntime()) {
            return new UnixServiceModeDirProvider(applicationName, chainId, dirLocations);
        } else return new ServiceModeDirProvider(applicationName, chainId, dirLocations);
    }
}
