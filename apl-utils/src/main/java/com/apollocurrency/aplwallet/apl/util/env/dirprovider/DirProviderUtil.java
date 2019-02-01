/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Util class for dir providers
 */
public class DirProviderUtil {
    /**
     * Path to directory where a project is installed or top project dir if we're in IDE or tests
     * TODO: maybe find some better solution
     * @return File path denoting path to directory with main executable jar
     */
    public static Path getBinDir() {
        URI res = Paths.get("").toUri();
        if(RuntimeEnvironment.getInstance().getMain()==null){
            RuntimeEnvironment.getInstance().setMain(DirProviderUtil.class);
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
    private DirProviderUtil() {} //never, only static methods
}
