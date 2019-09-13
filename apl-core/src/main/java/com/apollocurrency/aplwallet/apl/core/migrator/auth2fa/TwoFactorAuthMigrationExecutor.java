/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.auth2fa;

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
 * Provide 2fa specific components for migration
 * @see MigrationExecutor
 * @see DefaultDirectoryMigrator
 */
public class TwoFactorAuthMigrationExecutor extends MigrationExecutor {

    public static final String TWO_FACTOR_AUTH_DIR_PROPERTY_NAME = "apl.dir2FA";

    @Inject
    public TwoFactorAuthMigrationExecutor(DatabaseManager databaseManager, PropertiesHolder holder) {
        super(holder,  databaseManager, "2fa", true);
    }

    @Override
    protected List<Path> getSrcPaths() {
        String twoFactorAuthDir = holder.getStringProperty(TWO_FACTOR_AUTH_DIR_PROPERTY_NAME);
        List<Path> paths = new ArrayList<>();
        if (twoFactorAuthDir != null) {
            Path legacyHomeDir = MigratorUtil.getLegacyHomeDir();
            paths.add(legacyHomeDir.resolve(twoFactorAuthDir).normalize());
        }
        return paths;
    }

    @Override
    protected Migrator getMigrator() {
        return new DefaultDirectoryMigrator();
    }
}
