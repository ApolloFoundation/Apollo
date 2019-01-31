/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PredefinedDirLocations {
    private Path dbDir;
    private Path logsDir;
    private Path vaultKeystoreDir;
    private Path pidFilePath;
    private Path twoFactorAuthDir;

    public PredefinedDirLocations() {
        this(null, null, null, null, null);
    }

    public PredefinedDirLocations(String dbDir, String logsDir, String vaultKeystoreDir, String pidFilePath, String twoFactorAuthDir) {
        this.dbDir = getPath(dbDir);
        this.logsDir = getPath(logsDir);
        this.vaultKeystoreDir = getPath(vaultKeystoreDir);
        this.pidFilePath = getPath(pidFilePath);
        this.twoFactorAuthDir = getPath(twoFactorAuthDir);
    }

    private Path getPath(String path) {
        try {
            if (!StringUtils.isBlank(path)) {
                return Paths.get(path);
            }
        }
        catch (InvalidPathException | NullPointerException ignored) {}
        return null;
    }

    public Path getDbDir() {
        return dbDir;
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public Path getVaultKeystoreDir() {
        return vaultKeystoreDir;
    }

    public Path getPidFilePath() {
        return pidFilePath;
    }

    public Path getTwoFactorAuthDir() {
        return twoFactorAuthDir;
    }
}
