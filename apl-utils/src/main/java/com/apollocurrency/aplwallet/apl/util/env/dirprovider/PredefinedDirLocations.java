/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import lombok.Getter;

public class PredefinedDirLocations {

    @Getter
    private final Path dbDir;
    @Getter
    private final Path logsDir;
    @Getter
    private final Path vaultKeystoreDir;
    @Getter
    private final Path dexKeystoreDir;
    @Getter
    private final Path pidFilePath;
    @Getter
    private final Path twoFactorAuthDir;
    @Getter
    private final Path dataExportDir; // path to keep exported CSV files

    public PredefinedDirLocations(Properties properties) {
        this(
                (String) properties.get("apl.customDbDir"),
                (String) properties.get("apl.customLogDir"),
                (String) properties.get("apl.customVaultKeystoreDir"),
                (String) properties.get("apl.customPidFile"),
                (String) properties.get("apl.dir2FA"),
                (String) properties.get("apl.customDataExportDir"),
                (String) properties.get("apl.customDexStorageDir")
        );
    }

    public PredefinedDirLocations(String dbDir,
            String logsDir,
            String vaultKeystoreDir,
            String pidFilePath,
            String twoFactorAuthDir,
            String dataExportDir,
            String dexStorage) 
    {
        this.dbDir = getPath(dbDir);
        this.logsDir = getPath(logsDir);
        this.vaultKeystoreDir = getPath(vaultKeystoreDir);
        this.dexKeystoreDir = getPath(dexStorage);

        this.pidFilePath = getPath(pidFilePath);
        this.twoFactorAuthDir = getPath(twoFactorAuthDir);
        this.dataExportDir = getPath(dataExportDir);
    }

    private Path getPath(String path) {
        try {
            if (!StringUtils.isBlank(path)) {
                return Paths.get(path);
            }
        } catch (InvalidPathException | NullPointerException ignored) {
        }
        return null;
    }

}
