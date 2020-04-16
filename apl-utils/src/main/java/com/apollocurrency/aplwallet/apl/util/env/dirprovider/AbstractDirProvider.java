/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.StringValidator;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractDirProvider implements DirProvider {
    static final String PID_FORMAT = "%s-%s.pid";

    private String applicationName;
    private String chainId;
    private String baseDir;

    private Path dbDir;
    private Path logsDir;
    private Path vaultKeystoreDir;
    private Path dexKeystoreDir;
    private Path pidFilePath;
    private Path twoFactorAuthDir;
    private Path dataExportDir; // path to keep exported CSV files

    public AbstractDirProvider(String baseDir, String applicationName, UUID chainId) {
        this(baseDir, applicationName, chainId, null);
    }


    public AbstractDirProvider(String baseDir, String applicationName, UUID chainId, PredefinedDirLocations dirLocations) {
        Objects.requireNonNull(chainId, "ChainId cannot be null");
        StringValidator.requireNonBlank(baseDir, "Base dir");
        StringValidator.requireNonBlank(applicationName, "Application name");

        this.applicationName = applicationName;
        this.baseDir = baseDir;
        this.chainId = chainId.toString().substring(0, 6);

        if (dirLocations != null) {
            this.dbDir = dirLocations.getDbDir();
            this.logsDir = dirLocations.getLogsDir();
            this.vaultKeystoreDir = dirLocations.getVaultKeystoreDir();
            this.pidFilePath = dirLocations.getPidFilePath();
            this.twoFactorAuthDir = dirLocations.getTwoFactorAuthDir();
            this.dataExportDir = dirLocations.getDataExportDir();
            this.dexKeystoreDir = dirLocations.getDexKeystoreDir();
        }
    }

    @Override
    public Path getAppBaseDir() {
        return Paths.get(baseDir);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getChainId() {
        return chainId;
    }

    public void setPidFilePath(Path pidFilePath) {
        this.pidFilePath = pidFilePath;
    }

    public void setTwoFactorAuthDir(Path twoFactorAuthDir) {
        this.twoFactorAuthDir = twoFactorAuthDir;
    }

    @Override
    public Path getDbDir() {
        return dbDir == null
            ? Paths.get(baseDir, applicationName + "-db", (chainId))
            : dbDir;
    }

    public void setDbDir(Path dbDir) {
        this.dbDir = dbDir;
    }

    @Override
    public Path getVaultKeystoreDir() {
        return vaultKeystoreDir == null
            ? Paths.get(baseDir, applicationName + "-vault-keystore", (chainId))
            : vaultKeystoreDir;
    }

    public void setVaultKeystoreDir(Path vaultKeystoreDir) {
        this.vaultKeystoreDir = vaultKeystoreDir;
    }

    @Override
    public Path getSecureStorageDir() {
        return dexKeystoreDir == null
            ? Paths.get(baseDir, applicationName + "-secure-storage", (chainId))
            : dexKeystoreDir;
    }

    @Override
    public Path get2FADir() {
        return twoFactorAuthDir == null
            ? getVaultKeystoreDir().resolve(applicationName + "-2fa")
            : twoFactorAuthDir;
    }

    @Override
    public Path getLogsDir() {
        return logsDir == null
            ? Paths.get(baseDir, applicationName + "-logs")
            : logsDir;
    }

    public void setLogsDir(Path logsDir) {
        this.logsDir = logsDir;
    }

    @Override
    public Path getPIDFile() {
        return pidFilePath == null
            ? Paths.get(baseDir, String.format(PID_FORMAT, applicationName, (chainId)))
            : pidFilePath;
    }

    @Override
    public Path getFullTextSearchIndexDir() {
        return getDbDir().resolve(applicationName);
    }

    @Produces
    @Named("dataExportDir")
    @Override
    public Path getDataExportDir() {
        return dataExportDir == null
            // by default assign data export folder inside dbDir folder, but can be reassigned
            ? getDbDir().resolve("apl-blockchain-export-data") // default value
            : dataExportDir;
    }

    public void setDataExportDir(Path dataExportDir) {
        this.dataExportDir = dataExportDir;
    }
}
