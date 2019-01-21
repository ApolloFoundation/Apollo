/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.keystore;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.migrator.DefaultDirectoryMigrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class VaultKeystoreMigrationExecutor extends MigrationExecutor {
    @Inject
    public VaultKeystoreMigrationExecutor(PropertiesHolder holder, BlockchainConfig config) {
        super(holder, config, "vaultkeystore");
    }

    @Override
    protected List<Path> createSrcPaths() {
        boolean testnet = config.isTestnet();
        String keystorePrefix = testnet ? "apl.keystoreTestnet" : "apl.keystore";
        String keystoreDir = holder.getStringProperty(keystorePrefix + "Dir");
        List<Path> paths = new ArrayList<>();
        if (keystoreDir != null) {
            Path legacyHomeDir = MigratorUtil.getLegacyHomeDir();
            paths.add(legacyHomeDir.resolve(keystoreDir));
        }
        return paths;
    }

    @Override
    protected Migrator getMigrator() {
        return new DefaultDirectoryMigrator();
    }
}
