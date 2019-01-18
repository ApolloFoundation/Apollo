/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class UnixServiceModeDirProvider extends ServiceModeDirProvider {

    public UnixServiceModeDirProvider(String applicationName, UUID chainId) {
        this(applicationName, chainId, null);
    }

    public UnixServiceModeDirProvider(String applicationName, UUID chainId, PredefinedDirLocations dirLocations) {
        super(applicationName, chainId, dirLocations);
        if (dirLocations == null || dirLocations.getLogsDir() == null) {
            setLogsDir(getLogsDirPath());
        }
        if (dirLocations == null || dirLocations.getPidFilePath() == null) {
            setPidFilePath(getPIDFilePath());
        }
    }


    private Path getLogsDirPath() {
        return Paths.get("/var/log", getApplicationName());
    }


    private Path getPIDFilePath() {
        return Paths.get("/var/run", getApplicationName(), String.format(AbstractDirProvider.PID_FORMAT, getApplicationName(), getChainId()));
    }
}
