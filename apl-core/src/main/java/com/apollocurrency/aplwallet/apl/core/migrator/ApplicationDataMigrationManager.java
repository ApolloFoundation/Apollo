/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.PublicKeyMigrator;
import com.apollocurrency.aplwallet.apl.core.migrator.auth2fa.TwoFactorAuthMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.db.DbMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.keystore.VaultKeystoreMigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.inject.Inject;

/**
 * Perform all application data migration
 */
public class ApplicationDataMigrationManager {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDataMigrationManager.class);

    @Inject
    private VaultKeystoreMigrationExecutor vaultKeystoreMigrationExecutor;
    @Inject
    private DbMigrationExecutor dbMigrationExecutor                      ;
    @Inject
    private TwoFactorAuthMigrationExecutor twoFactorAuthMigrationExecutor;
    @Inject
    private PublicKeyMigrator publicKeyMigrator;

    public void executeDataMigration() {
        try {
            dbMigrationExecutor.performMigration(AplCoreRuntime.getInstance().getDbDir().resolve(Constants.APPLICATION_DIR_NAME));
            twoFactorAuthMigrationExecutor.performMigration(AplCoreRuntime.getInstance().get2FADir());
            vaultKeystoreMigrationExecutor.performMigration(AplCoreRuntime.getInstance().getVaultKeystoreDir());

            if (!dbMigrationExecutor.isAutoCleanup()) {
                dbMigrationExecutor.performAfterMigrationCleanup();
            }
            if (!vaultKeystoreMigrationExecutor.isAutoCleanup()) {
                vaultKeystoreMigrationExecutor.performAfterMigrationCleanup();
            }
            if (!twoFactorAuthMigrationExecutor.isAutoCleanup()) {
                twoFactorAuthMigrationExecutor.performAfterMigrationCleanup();
            }
            publicKeyMigrator.migrate();
        }
        catch (IOException e) {
            LOG.error("Fatal error. Cannot proceed data migration", e);
            System.exit(-1);
        }
    }

    public void setVaultKeystoreMigrationExecutor(VaultKeystoreMigrationExecutor vaultKeystoreMigrationExecutor) {
        this.vaultKeystoreMigrationExecutor = vaultKeystoreMigrationExecutor;
    }

    public void setDbMigrationExecutor(DbMigrationExecutor dbMigrationExecutor) {
        this.dbMigrationExecutor = dbMigrationExecutor;
    }

    public void setTwoFactorAuthMigrationExecutor(TwoFactorAuthMigrationExecutor twoFactorAuthMigrationExecutor) {
        this.twoFactorAuthMigrationExecutor = twoFactorAuthMigrationExecutor;
    }

    public void setPublicKeyMigrator(PublicKeyMigrator publicKeyMigrator) {
        this.publicKeyMigrator = publicKeyMigrator;
    }
}
