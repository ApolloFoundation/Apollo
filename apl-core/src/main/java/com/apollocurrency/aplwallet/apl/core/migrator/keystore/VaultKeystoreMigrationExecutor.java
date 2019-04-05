/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.keystore;

import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.migrator.DefaultDirectoryMigrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.Migrator;
import com.apollocurrency.aplwallet.apl.core.migrator.MigratorUtil;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Provide vaultKeystore specific components for migration
 * @see MigrationExecutor
 * @see DefaultDirectoryMigrator
 */
public class VaultKeystoreMigrationExecutor extends MigrationExecutor {

    public static final String KEYSTORE_PROPERTY_PREFIX = "apl.keystore";

    @Inject
    public VaultKeystoreMigrationExecutor(DatabaseManager databaseManager, PropertiesHolder holder) {
        super(holder, databaseManager,  "vaultkeystore", false);
    }


    @Override
    protected List<Path> getSrcPaths() {
        String keystoreDir = holder.getStringProperty(KEYSTORE_PROPERTY_PREFIX + "Dir");
        List<Path> paths = new ArrayList<>();
        if (keystoreDir != null) {
            Path legacyHomeDir = MigratorUtil.getLegacyHomeDir();
            paths.add(legacyHomeDir.resolve(keystoreDir).normalize());
        }
        return paths;
    }

    @Override
    protected Migrator getMigrator() {
        return new DefaultDirectoryMigrator();
    }
}
