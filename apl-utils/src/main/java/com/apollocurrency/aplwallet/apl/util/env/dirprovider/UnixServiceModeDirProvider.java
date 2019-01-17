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
    }

    @Override
    public Path getLogsDir() {
        return Paths.get("/var/log", getApplicationName());
    }

    @Override
    public Path getPIDFile() {
        return Paths.get("/var/run", String.format(AbstractDirProvider.PID_FORMAT, getApplicationName(), getChainId()));
    }
}
