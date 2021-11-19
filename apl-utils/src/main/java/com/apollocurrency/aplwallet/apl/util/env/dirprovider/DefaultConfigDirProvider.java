/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Config dir provider which provide default config files locations
 */
public class DefaultConfigDirProvider implements ConfigDirProvider {
    /**
     * Know network ID's including alias index and config-specific dir
     */
    static final ChainSpecsHolder CHAINS = new ChainSpecsHolder(Set.of(
        ChainSpec.createFull(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"),"conf", 0), // main net
        ChainSpec.createFull(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1"),"conf-tn1", 1), // test net 1
        ChainSpec.createFull(UUID.fromString("00150000-cb84-4c31-9f7c-7af4c3c0080a"),"conf-tn15", 15), // test net 15 (stage)
        ChainSpec.createFull(UUID.fromString("2f2b6149-d29e-41ca-8c0d-f3343f5540c6"),"conf-tn2", 2), // test net 2
        ChainSpec.createFull(UUID.fromString("3fecf3bd-86a3-436b-a1d6-41eefc0bd1c6"),"conf-tn3", 3) //test net 3
    ));

    protected String applicationName;
    protected boolean isService;
    protected volatile ChainSpec chainSpec;

    /**
     * Constructs config dir provider
     *
     * @param applicationName name of application's parameter dir
     * @param isService       service mode or user mode
     * @param netIdx          index of network. 0 means main net, 1,15,2,3 - testnets 1,15,2,3.
     *                        If index is <0, it should not be used, UUID or partial UUID should be
     *                        used instead @param uuid UUID of chain or few first symbols @param uuid
     * @param uuidOrPart      UUID that is chainID or few first bytes in hex
     */
    public DefaultConfigDirProvider(String applicationName, boolean isService, int netIdx, String uuidOrPart) {
        if (StringUtils.isBlank(applicationName)) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;

        Optional<ChainSpec> byIndex = CHAINS.getByIndex(netIdx);
        if (StringUtils.isBlank(uuidOrPart) && byIndex.isEmpty()){
            this.chainSpec = CHAINS.getMainnet(); //default to main net if no params
            return;
        }
        if (byIndex.isPresent() && StringUtils.isNotBlank(uuidOrPart)) {
            throw new IllegalArgumentException("Both netIdx and uuidOrPart specified for chain selection, required " +
                "only one parameter: netIdx= " + netIdx + ", uuidOrPart " + uuidOrPart
            );
        }
        if (StringUtils.isNotBlank(uuidOrPart)) {
            this.chainSpec = CHAINS.findByChainId(uuidOrPart).orElse(ChainSpec.createExternal(uuidOrPart));
        } else {
            this.chainSpec = byIndex.get();
        }
    }

    @Override
    public String getConfigName() {
        return chainSpec.getConfigDirPath();
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
        return Paths.get(System.getProperty("user.home"), "." + applicationName).toAbsolutePath().toString();
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
        return chainSpec.getChainId();
    }

    @Override
    public void setChainID(UUID newID) {
        if (chainSpec.isFullyInitialized()) {
            System.err.println("Chain spec is already initialized to: " + chainSpec);
            System.err.println("Changing Chain ID to: " + newID.toString());
            return;
        }
        chainSpec = ChainSpec.createInitializedExternal(newID);
    }

    @Override
    public String getChainIdPart() {
        return chainSpec.getChainIdShortened();
    }

    private static class ChainSpecsHolder {
        private final Set<ChainSpec> specs = new HashSet<>();

        private ChainSpecsHolder(Set<ChainSpec> specs) {
            this.specs.addAll(specs);
        }

        private Optional<ChainSpec> getByIndex(int index) {
            return this.specs.stream().filter(e -> e.getIndex() == index).findAny();
        }

        private ChainSpec getMainnet() {
            return getByIndex(0).orElseThrow(() -> new IllegalStateException("Mainnet network spec is not exist inside the chain specs holder: " + specs + ", expected network with index 0 - MAINNET"));
        }

        private Optional<ChainSpec> findByChainId(String partialChainId) {
            return this.specs.stream().filter(e -> e.getChainId().toString().startsWith(partialChainId)).findAny();
        }
    }

    @Getter
    @ToString
    private static class ChainSpec {
        private final UUID chainId;
        private final String confDir;
        private final int index;
        private final String partialChainId;

        private ChainSpec(UUID chainId, String confDir, int index, String partialChainId) {
            this.chainId = chainId;
            this.confDir = confDir;
            this.index = index;
            this.partialChainId = partialChainId;
        }

        public String getChainIdShortened() {
            return partialChainId;
        }

        public String getConfigDirPath() {
            return confDir;
        }

        public boolean isFullyInitialized() {
            return chainId != null;
        }

        public String getChainIdString() {
            return chainId.toString();
        }

        public static ChainSpec createExternal(String chainIdString) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(chainIdString);
                return createInitializedExternal(uuid);
            } catch (IllegalArgumentException ignored) {
                return new ChainSpec(uuid, externalConfigPath(chainIdString), -1, chainIdString);
            }
        }

        public static ChainSpec createInitializedExternal(UUID chainId) {
            return new ChainSpec(chainId, externalConfigPath(chainId.toString()), -1, chainId.toString().substring(0, 6));
        }

        public static ChainSpec createFull(UUID chainId, String confDir, int index) {
            return new ChainSpec(chainId, confDir, index, chainId.toString().substring(0, 6));
        }

        private static String externalConfigPath(String chainIdString) {
            return CONFIGS_DIR_NAME + File.separator + chainIdString;
        }
    }
}
