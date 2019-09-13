/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MigratorUtil {
    public static Path getLegacyHomeDir() {
        Path homeDirPath;
        if (!RuntimeEnvironment.getInstance().isServiceMode()) {
            if (RuntimeEnvironment.getInstance().isWindowsRuntime()) {
                homeDirPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "APOLLO");
            } else {
                homeDirPath = Paths.get(System.getProperty("user.home"), ".apollo");
            }
        } else {
            homeDirPath = DirProvider.getBinDir();
        }
        return homeDirPath;
    }
}
