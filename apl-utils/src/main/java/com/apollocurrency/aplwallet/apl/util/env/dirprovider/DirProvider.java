/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Path;

/**
 * Provides paths to application data, such as database, keystore, etc.
 * Returned paths are OS-dependent
 */
public interface DirProvider {
    /**
     * @return path to the directory where current database stored
     */
    Path getDbDir();

    /**
     * @return path to the directory where current keystore stored
     */
    Path getVaultKeystoreDir();

    /**
     * @return path to the directory where current 2fa data stored
     */
    Path get2FADir();

    /**
     * @return path to the directory where logs of the application stored
     */
    Path getLogsDir();

    /**
     * @return path to the PID (Process identifier) file of the application
     */
    Path getPIDFile();

    /**
     * @return base data directory of the application
     */
    Path getAppBaseDir();

    /**
     * @return return path to the directory, which store application executables
     */
    default Path getBinDir() {
        return DirProviderUtil.getBinDir();
    }
}
