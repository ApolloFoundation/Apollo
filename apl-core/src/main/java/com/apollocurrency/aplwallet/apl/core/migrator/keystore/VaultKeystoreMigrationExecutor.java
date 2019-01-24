/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.keystore;

import com.apollocurrency.aplwallet.apl.core.app.KeyStore;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.migrator.DefaultDirectoryMigrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide vaultKeystore specific components for migration
 * @see MigrationExecutor
 * @see DefaultDirectoryMigrator
 */
public class VaultKeystoreMigrationExecutor extends MigrationExecutor {
    @Inject
    public VaultKeystoreMigrationExecutor(PropertiesHolder holder, BlockchainConfig config) {
        super(holder, config, "vaultkeystore", false);
    }

    @Override

    protected void beforeMigration() {
        // init keystore before migration
        CDI.current().select(KeyStore.class).get();
    }

    @Override
    protected List<Path> getSrcPaths() {
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
