/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.db;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class LegacyDbLocationsProvider {
    private PropertiesHolder propertiesHolder;
    private boolean isTestnet;
    private String oldDbPrefix;
    private BlockchainConfig blockchainConfig;
    @Inject
    public LegacyDbLocationsProvider(BlockchainConfig config, PropertiesHolder propertiesHolder) {
        Objects.requireNonNull(propertiesHolder, "Properties holder cannot be null");
        Objects.requireNonNull(config, "Blockchain config cannot be null");
        this.propertiesHolder = propertiesHolder;
        this.isTestnet = config.isTestnet();
        this.oldDbPrefix = isTestnet ? "apl.testDb" : "apl.db";
        this.propertiesHolder = propertiesHolder;
        this.blockchainConfig = config;
    }

    public List<Path> getDbLocations() {
        List<Path> dbsPath = new ArrayList<>();
        String dbName = propertiesHolder.getStringProperty(oldDbPrefix + "Name");
        String dbDir = propertiesHolder.getStringProperty(oldDbPrefix + "Dir");
        UUID chainId = blockchainConfig.getChain().getChainId();

        String initialDbPath = dbDir + File.separator + dbName;
        String oldChainIdDbPath = chainId + File.separator + dbDir + File.separator + dbName;
        String recentChainIdDbPath = dbDir + File.separator + chainId + File.separator + dbName;
        Path homeDirPath = MigratorUtil.getLegacyHomeDir();

        dbsPath.add(homeDirPath.resolve(recentChainIdDbPath).normalize());
        dbsPath.add(homeDirPath.resolve(oldChainIdDbPath).normalize());
        dbsPath.add(homeDirPath.resolve(initialDbPath).normalize());
        return dbsPath;
    }
}
