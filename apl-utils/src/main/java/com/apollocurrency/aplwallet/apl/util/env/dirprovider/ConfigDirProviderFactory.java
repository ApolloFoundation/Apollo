/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.FileUtils;
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
    private static String uuid_or_part;
    private static UUID uuid;

    public static void setup(boolean isServiceP, String applicationNameP, int netIdxP, String uuid_or_partP) {
        isService = isServiceP;
        applicationName = applicationNameP;
        netIdx = netIdxP;
        uuid_or_part = uuid_or_partP;
    }

    public static ConfigDirProvider createConfigDirProvider() {
        ConfigDirProvider res;
        if (RuntimeEnvironment.getInstance().isUnixRuntime()) {
            res = new UnixConfigDirProvider(applicationName, isService, netIdx, uuid_or_part);
        } else {
            res = new DefaultConfigDirProvider(applicationName, isService, netIdx, uuid_or_part);
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
                        //at the mopment we chack directories only
                        if (Files.isDirectory(path)) {
                            String fname = path.getFileName().toString();
                            if (fname.toLowerCase().startsWith(uuid_or_part)) {
                                cdp.setChainID(UUID.fromString(fname));
                                break;
                            }
                        }
                    }
                } catch (IOException ex) {

                }
            }
            if (cdp.getChainId() == null) {
                System.err.println("UUID part: " + uuid_or_part + " can not be resolved by installed configurations.");
            }
        }
        return cdp;
    }
}
