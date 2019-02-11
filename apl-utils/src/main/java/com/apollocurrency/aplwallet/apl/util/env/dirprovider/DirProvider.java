/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * @return path to the directory where current fulltext search index stored
     */
    Path getFullTextSearchIndexDir();

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
     * Path to directory where a project is installed or top project dir if we're in IDE or tests
     * TODO: maybe find some better solution
     * @return File path denoting path to directory with main executable jar
     */
    public static Path getBinDir() {
        URI res = Paths.get("").toUri();
        if(RuntimeEnvironment.getInstance().getMain()==null){
            RuntimeEnvironment.getInstance().setMain(DirProvider.class);
        }
        try {
            //get location of main app class
            res = RuntimeEnvironment.getInstance().getMain().getProtectionDomain().getCodeSource().getLocation().toURI();
        }
        catch (URISyntaxException ignored) {
        }
        // remove jar name or "classes". Should be location jar directory
        Path createdBinPath = Paths.get(res).getParent().toAbsolutePath();
        if (createdBinPath.endsWith("target")) { //we are in dev env or IDE
            createdBinPath = createdBinPath.getParent().getParent();
        }
        return createdBinPath;
    }
}
