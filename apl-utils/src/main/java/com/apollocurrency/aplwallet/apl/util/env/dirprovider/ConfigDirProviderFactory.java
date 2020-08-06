/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

/**
 * Simple factory which should used for configDirProvider creation
 */
public class ConfigDirProviderFactory {

    private static boolean isService;
    private static String applicationName;
    private static int netIdx;
    private static String uuid_or_part;

    public static void setup(boolean isServiceP, String applicationNameP, int netIdxP, String uuid_or_partP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        netIdx = netIdxP;
        uuid_or_part = uuid_or_partP;
    }

    public static ConfigDirProvider getConfigDirProvider() {
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            return new UnixConfigDirProvider(applicationName, isService, netIdx, uuid_or_part);
        }
        return new DefaultConfigDirProvider(applicationName, isService, netIdx, uuid_or_part);
    }
}
