/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.io.File;
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
     * @param isService       service mode or user mode
     * @param netIdx          index of network. 0 means main net, 1,2,3 - testnets 1,2,3.
     *                        If index is <0, it should not be used, UUID or partial UUID should be
     *                        used instead @param uuid UUID of chain or few first symbols @param uuid
     * @param uuidOrPart      UUID that is chainID or few first bytes in hex
     */
    public DefaultConfigDirProvider(String applicationName, boolean isService, int netIdx, String uuidOrPart) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;

        if (netIdx < 0 && uuidOrPart.isEmpty()) {
            uuidOrPart = CHAIN_IDS[0]; //default to main net if no params
        }

        if (!uuidOrPart.isEmpty()) {
            try {
                chainUuid = UUID.fromString(uuidOrPart);
            } catch (IllegalArgumentException ex) {
                partialUuid = uuidOrPart;
            }
        }

        if (netIdx > CONF_DIRS.length - 1) {
            System.err.println("Net index " + netIdx + " is greater than last known.");
            this.netIndex = CONF_DIRS.length - 1;
            System.err.println("Net index now is last one: " + netIdx);
        } else {
            this.netIndex = netIdx;
        }

        if (netIdx >= 0) {
            chainUuid = UUID.fromString(CHAIN_IDS[netIdx]);
        }

        //find in known networks
        if (uuidOrPart != null && !uuidOrPart.isEmpty()) {
            for (int i = 0; i < CHAIN_IDS.length; i++) {
                String id = CHAIN_IDS[i];
                if (id.startsWith(uuidOrPart.toLowerCase())) {
                    this.netIndex = i;
                    chainUuid = UUID.fromString(id);
                    break;
                }
            }
        }

    }

    @Override
    public String getConfigName() {
        String res;
        if (netIndex >= 0) {
            res = CONF_DIRS[netIndex];
        } else {
            res = CONFIGS_DIR_NAME + File.separator + chainUuid.toString();
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
        String res;
        if (isService) {
            res = getSysConfigLocation();
        } else {
            res = getUserConfigLocation();
        }
        return res;

    }

    @Override
    public UUID getChainId() {
        return chainUuid;
    }

    @Override
    public void setChainID(UUID newID) {
        if (chainUuid != null) {
            System.err.println("Chain ID is already set to: " + chainUuid.toString());
            System.err.println("Changing Chain ID to: " + newID.toString());
        }
        chainUuid = newID;
    }

    @Override
    public String getChainIdPart() {
        return partialUuid;
    }
}
