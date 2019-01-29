/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import org.apache.commons.lang3.StringUtils;

public class EnvironmentVariables {
    String logDir = "";
    String dbDir = "";
    String vaultKeystoreDir = "";
    String twoFactorAuthDir = "";
    String pidFile = "";
    String configDir = "";
    private String applicationName;

    public EnvironmentVariables(String applicationName) {
        if (StringUtils.isBlank(applicationName)) {
            throw new IllegalArgumentException("Application name cannot be null or blank");
        }
        this.applicationName = applicationName.toUpperCase();
        retrieve();
    }

    protected void retrieve() {
        logDir = System.getenv(applicationName + "_LOG_DIR");
        dbDir = System.getenv(applicationName + "_DB_DIR");
        vaultKeystoreDir = System.getenv(applicationName + "_VAULT_KEY_DIR");
        pidFile = System.getenv(applicationName + "_PID_FILE");
        twoFactorAuthDir = System.getenv(applicationName + "_2FA_DIR");
        configDir = System.getenv(applicationName + "_CONFIG_DIR");
    }

    public PredefinedDirLocations merge(CmdLineArgs args) {
        return new PredefinedDirLocations(
                StringUtils.isBlank(args.dbDir)            ? dbDir            : args.dbDir,
                StringUtils.isBlank(args.logDir)           ? logDir           : args.logDir,
                StringUtils.isBlank(args.vaultKeystoreDir) ? vaultKeystoreDir : args.vaultKeystoreDir,
                StringUtils.isBlank(args.twoFactorAuthDir) ? twoFactorAuthDir : args.twoFactorAuthDir,
                StringUtils.isBlank(args.pidFile)          ? pidFile          : args.pidFile
        );
    }

}
