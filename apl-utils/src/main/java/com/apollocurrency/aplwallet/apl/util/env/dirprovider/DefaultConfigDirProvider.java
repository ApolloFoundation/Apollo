/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Paths;

/**
 * Config dir provider which provide default config files locations
 */
public class DefaultConfigDirProvider implements ConfigDirProvider {

    /**
     * Chain IDs ow known networks from 0 (mainnet) to 3rd testnet }
     */
    protected static final String[] CHAIN_IDS = {
        "b5d7b697-f359-4ce5-a619-fa34b6fb01a5", //main net
        "a2e9b946-290b-48b6-9985-dc2e5a5860a1", //test net 1
        "2f2b6149-d29e-41ca-8c0d-f3343f5540c6", //test net 2
        "3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6" //test net 3
    };

    /**
     * Deprecated names of configuration directories for known networks. Should
     * be replaced later by chainID
     */
    protected static final String[] CONF_DIRS = {
        "conf", //main net
        "conf-tn1", //test net 1
        "conf-tn2", //test net 2
        "conf-tn3" //test net 3
    };

    protected String applicationName;
    protected boolean isService;
    protected int netIndex;

    /**
     * Constructs config dir provider
     *
     * @param applicationName name of application's parameter dir
     * @param isService service mode or user mode
     * @param netIdx index of network. 0 means main net, 1,2,3 - testnets 1,2,3
     */
    public DefaultConfigDirProvider(String applicationName, boolean isService, int netIdx) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;
        if (netIdx <= 0) {
            this.netIndex = 0;
        } else if (netIdx > CONF_DIRS.length - 1) {
            this.netIndex = CONF_DIRS.length - 1;
        } else {
            this.netIndex = netIdx;
        }
    }

    @Override
    public String getConfigDirectoryName() {
        return CONF_DIRS[netIndex];
    }

    @Override
    public String getInstallationConfigDirectory() {
        return DirProvider.getBinDir().resolve(getConfigDirectoryName()).toAbsolutePath().toString();
    }

    @Override
    public String getSysConfigDirectory() {
        return getInstallationConfigDirectory();
    }

    @Override
    public String getUserConfigDirectory() {
        String res = Paths.get(System.getProperty("user.home"), "." + applicationName, getConfigDirectoryName()).toAbsolutePath().toString();
        return res;
    }

    @Override
    public String getConfigDirectory() {
        String res
                = isService
                        ? getSysConfigDirectory()
                        : getUserConfigDirectory();
        return res;

    }
}
