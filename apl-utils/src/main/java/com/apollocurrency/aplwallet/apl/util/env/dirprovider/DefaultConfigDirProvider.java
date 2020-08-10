/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Paths;
import java.util.UUID;

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
    protected String partialUuid;
    protected UUID chainUuid;
    protected boolean isService;
    protected int netIndex;

    /**
     * Constructs config dir provider
     *
     * @param applicationName name of application's parameter dir
     * @param isService service mode or user mode
     * @param netIdx index of network. 0 means main net, 1,2,3 - testnets 1,2,3.
     * If index is <0, it should not be used, UUID or partial UUID should be
     * used instead @param uuid UUID of chain or few first symbols @param uuid
     * @param uuid_or_part UUID that is chainID or few first bytes in hex
     */
    public DefaultConfigDirProvider(String applicationName, boolean isService, int netIdx, String uuid_or_part) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;

        if (!uuid_or_part.isEmpty()) {
            try {
                chainUuid = UUID.fromString(uuid_or_part);
            } catch (IllegalArgumentException ex) {
                chainUuid = null;
                partialUuid = uuid_or_part;
            }
        }

        if (netIdx > CONF_DIRS.length - 1) {
            this.netIndex = CONF_DIRS.length - 1;
        } else {
            this.netIndex = netIdx;
        }
        if (netIdx >= 0) {
            chainUuid = UUID.fromString(CHAIN_IDS[netIdx]);
        }
    }

    @Override
    public String getConfigName() {
        String res;
        if (netIndex > 0) {
            res = CONF_DIRS[netIndex];
        } else {
            res = CONFIGS_DIR_NAME + "/" + chainUuid.toString();
        }
        return res;
    }

    @Override
    public String getInstallationConfigLocation() {
        return DirProvider.getBinDir().resolve("").toAbsolutePath().toString();
    }

    //this is true for Windows
    @Override
    public String getSysConfigLocation() {
        return getInstallationConfigLocation();
    }

    @Override
    public String getUserConfigLocation() {
        String res = Paths.get(System.getProperty("user.home"), "." + applicationName).toAbsolutePath().toString();
        return res;
    }

    @Override
    public String getConfigLocation() {
        String res
                = isService
                        ? getSysConfigLocation()
                        : getUserConfigLocation();
        return res;

    }
}
