/*
 * Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

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
        String res = "";
        try {
            //get location of main app class
            String path = RuntimeEnvironment.getInstance().getMain().getProtectionDomain().getCodeSource().getLocation().getPath();
            res = URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException ignored) {
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
