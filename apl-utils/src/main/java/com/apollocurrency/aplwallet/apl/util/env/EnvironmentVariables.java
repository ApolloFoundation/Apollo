/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import com.apollocurrency.aplwallet.apl.util.StringUtils;

public class EnvironmentVariables {
    public String logDir = "";
    public String dbDir = "";
    public String vaultKeystoreDir = "";
    public String dexKeystoreDir = "";
    public String twoFactorAuthDir = "";
    public String pidFile = "";
    public String configDir = "";
    public String dataExportDir = ""; // path to keep exported CSV files
    private String applicationName;

    public EnvironmentVariables(String applicationName) {
        if (StringUtils.isBlank(applicationName)) {
            throw new IllegalArgumentException("Application name cannot be null or blank");
        }
        this.applicationName = applicationName.toUpperCase();
        retrieve();
    }

    protected final void retrieve() {
        logDir = System.getenv(applicationName + "_LOG_DIR");
        dbDir = System.getenv(applicationName + "_DB_DIR");
        vaultKeystoreDir = System.getenv(applicationName + "_VAULT_KEY_DIR");
        dexKeystoreDir = System.getenv(applicationName + "_DEX_KEY_DIR");
        pidFile = System.getenv(applicationName + "_PID_FILE");
        twoFactorAuthDir = System.getenv(applicationName + "_2FA_DIR");
        configDir = System.getenv(applicationName + "_CONFIG_DIR");
        dataExportDir = System.getenv(applicationName + "_DATA_EXPORT_DIR"); //
    }

}
