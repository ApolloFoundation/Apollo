/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.auth2fa;

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

public class TwoFactorAuthMigrationExecutor extends MigrationExecutor {

    @Inject
    public TwoFactorAuthMigrationExecutor(PropertiesHolder holder, BlockchainConfig config) {
        super(holder, config, "2fa");
    }

    @Override
    protected List<Path> createSrcPaths() {
        boolean testnet = config.isTestnet();
        String twoFactorAuthDirPropertyName = testnet ? "apl.testnetDir2FA" : "apl.dir2FA";
        String twoFactorAuthDir = holder.getStringProperty(twoFactorAuthDirPropertyName);
        List<Path> paths = new ArrayList<>();
        if (twoFactorAuthDir != null) {
            Path legacyHomeDir = MigratorUtil.getLegacyHomeDir();
            paths.add(legacyHomeDir.resolve(twoFactorAuthDir));
        }
        return paths;
    }

    @Override
    protected Migrator getMigrator() {
        return new DefaultDirectoryMigrator();
    }
}
