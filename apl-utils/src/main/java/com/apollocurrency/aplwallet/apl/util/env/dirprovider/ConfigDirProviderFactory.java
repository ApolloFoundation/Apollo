/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import javax.enterprise.inject.Produces;

/**
 * Simple factory which should used for configDirProvider creation
 */
public class ConfigDirProviderFactory {
    private static boolean isService;
    private static String applicationName;
    private static int netIdx;

    public static void setup(boolean isServiceP, String applicationNameP, int netIdxP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        netIdx = netIdxP;
    }

    @Produces
    public static ConfigDirProvider getConfigDirProvider() {
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            return new UnixConfigDirProvider(applicationName, isService, netIdx);
        }
        return new DefaultConfigDirProvider(applicationName, isService, netIdx);
    }
}
