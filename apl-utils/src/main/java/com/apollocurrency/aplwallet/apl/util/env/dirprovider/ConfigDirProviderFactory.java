/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

public class ConfigDirProviderFactory {
    public ConfigDirProvider getInstance(boolean isService, String applicationName) {
            if (RuntimeEnvironment.isUnixRuntime()) {
                return new UnixConfigDirProvider(applicationName, isService);
            }
        return new DefaultConfigDirProvider(applicationName, isService);
    }
}
