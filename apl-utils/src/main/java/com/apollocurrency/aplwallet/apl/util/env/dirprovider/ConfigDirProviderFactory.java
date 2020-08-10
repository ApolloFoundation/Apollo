/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.FileUtils;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple factory which should used for configDirProvider creation
 */
public class ConfigDirProviderFactory {

    private static boolean isService;
    private static String applicationName;
    private static int netIdx;
    private static String uuid_or_part;
    private static UUID uuid;

    public static void setup(boolean isServiceP, String applicationNameP, int netIdxP, String uuid_or_partP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        netIdx = netIdxP;
        uuid_or_part = uuid_or_partP;
    }

    public static ConfigDirProvider getConfigDirProvider() {
        ConfigDirProvider res;
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            res = new UnixConfigDirProvider(applicationName, isService, netIdx, "");
        } else {
            res = new DefaultConfigDirProvider(applicationName, isService, netIdx, "");
        }
        return res;
    }
}
