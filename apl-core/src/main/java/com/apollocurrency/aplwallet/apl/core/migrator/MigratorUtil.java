/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

public class MigratorUtil {
    public static Path getLegacyHomeDir() {
        Path homeDirPath;
        if (!RuntimeEnvironment.isServiceMode()) {
            if (RuntimeEnvironment.isWindowsRuntime()) {
                homeDirPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "APOLLO");
            } else {
                homeDirPath = Paths.get(System.getProperty("user.home"), ".apollo");
            }
        } else {
            homeDirPath = Paths.get("");
        }
        return homeDirPath;
    }
}
