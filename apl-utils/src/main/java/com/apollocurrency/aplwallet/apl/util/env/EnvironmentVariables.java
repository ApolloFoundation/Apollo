/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import org.apache.commons.lang3.StringUtils;

public class EnvironmentVariables {
    public String logDir = "";
    public String dbDir = "";
    public String vaultKeystoreDir = "";
    public String twoFactorAuthDir = "";
    public String pidFile = "";
    private String applicationName;
    public String configDir ="";
    
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
        pidFile = System.getenv(applicationName + "_PID_FILE");
        twoFactorAuthDir = System.getenv(applicationName + "_2FA_DIR");
        configDir = System.getenv(applicationName + "_CONFIG_DIR");
    }

}
