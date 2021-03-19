/*
 * Copyright © 2018 - 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static String uuidOrPart;
    private static String configDir = null;

    public static void setup(boolean isServiceP, String applicationNameP, int netIdxP, String uuidOrPartP, String configDirP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        netIdx = netIdxP;
        uuidOrPart = uuidOrPartP;
        configDir = configDirP;
    }

    private static ConfigDirProvider createConfigDirProvider() {
        ConfigDirProvider res;
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            res = new UnixConfigDirProvider(applicationName, isService, netIdx, uuidOrPart);
        } else {
            res = new DefaultConfigDirProvider(applicationName, isService, netIdx, uuidOrPart);
        }
        return res;
    }

    public static ConfigDirProvider getConfigDirProvider() {
        ConfigDirProvider cdp = createConfigDirProvider();
        if (cdp.getChainId() == null) {
            //chainID is not resolved internally so
            //try to resolve ChainID by search of configs in standard locations
            List<String> searchPaths = new ArrayList<>();
            searchPaths.add(cdp.getInstallationConfigLocation());
            searchPaths.add(cdp.getSysConfigLocation());
            searchPaths.add(cdp.getUserConfigLocation());
            for (String p : searchPaths) {
                String dir = Path.of(p, ConfigDirProvider.CONFIGS_DIR_NAME).toAbsolutePath().toString();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
                    for (Path path : stream) {
                        //at the moment we check directories only
                        if (Files.isDirectory(path)) {
                            String fname = path.getFileName().toString();
                            if (fname.toLowerCase().startsWith(uuidOrPart)) {
                                cdp.setChainID(UUID.fromString(fname));
                                break;
                            }
                        }
                    }
                } catch (IOException ex) {

                }
            }
            if (cdp.getChainId() == null) {
                System.err.println("UUID part: " + uuidOrPart + " can not be resolved by installed configurations.");
            }
        }
        return cdp;
    }

    /**
     * Returns the user-defined config directory,
     * that dir was set from command line parameter or environment variable
     *
     * @return the user-defined config directory or {@code null} if it wasn't set
     */
    public static String getConfigDir() {
        return configDir;
    }
}
