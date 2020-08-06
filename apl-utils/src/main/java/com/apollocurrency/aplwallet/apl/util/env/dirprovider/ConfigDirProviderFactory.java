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

    private static ConfigDirProvider createCP(String uuid) {
        ConfigDirProvider res;
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            res = new UnixConfigDirProvider(applicationName, isService, netIdx, "");
        } else {
            res = new DefaultConfigDirProvider(applicationName, isService, netIdx, "");
        }
        return res;
    }

    public static ConfigDirProvider getConfigDirProvider() {
        ConfigDirProvider res;
        //do we have full UUID?
        if (!uuid_or_part.isEmpty()) {
            try {
                uuid = UUID.fromString(uuid_or_part);
            } catch (IllegalArgumentException ex) {
                uuid = null;
            }
            if (uuid != null) {
                res = createCP(uuid.toString());
            } else { //we have part of UUID, try to find directory or zip that starts with
                ConfigDirProvider tmpCP = createCP("");
                String[] locations = new String[3];
                locations[0] = tmpCP.getInstallationConfigLocation();
                locations[1] = tmpCP.getSysConfigLocation();
                locations[3] = tmpCP.getUserConfigLocation();
                List<String> found = new ArrayList<>();
                for (String l : locations) {
                    found.addAll(FileUtils.searchByNamePart(l, uuid_or_part));
                }
                //now we have all  files/dirs that start with
                //TODO: check for mess
                res = tmpCP; // should new if found
            }
        } else {
            res = createCP("");
        }

        return res;
    }
}
