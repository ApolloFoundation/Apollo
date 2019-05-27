/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

/**
 * Simple factory which should used for configDirProvider creation
 */
public class ConfigDirProviderFactory {
    public ConfigDirProvider getInstance(boolean isService, String applicationName, int netIdx) {
            if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
                return new UnixConfigDirProvider(applicationName, isService, netIdx);
            }
        return new DefaultConfigDirProvider(applicationName, isService, netIdx);
    }
}
