/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.apollocurrency.aplwallet.apl.util.Constants;

@Singleton
public class EnvironmentVariables {
    public String logDir = "";
    public String dbDir = "";
    public String vaultKeystoreDir = "";
    public String dexKeystoreDir = "";
    public String twoFactorAuthDir = "";
    public String pidFile = "";
    public String configDir = "";
    public String dataExportDir = ""; // path to keep exported CSV files
    private String applicationName = Constants.APPLICATION_DIR_NAME.toUpperCase();

    @Inject
    public  EnvironmentVariables() {
        init();
    }

    protected final void init() {
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
