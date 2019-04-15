/*
 * Copyright © 2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.migrator.auth2fa.TwoFactorAuthMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.db.DbMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.keystore.VaultKeystoreMigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
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
    @Inject
    private ReferencedTransactionMigrator referencedTransactionMigrator;

    public void executeDataMigration() {
        try {
//            String customDbDir = propertiesHolder.getStringProperty("apl.customDbDir");
            String fileName = Constants.APPLICATION_DIR_NAME;
//            if (!StringUtils.isBlank(customDbDir)) {
//                fileName = propertiesHolder.getStringProperty("apl.dbName");
//            }
            Path targetDbPath = AplCoreRuntime.getInstance().getDbDir().resolve(fileName);
            dbMigrationExecutor.performMigration(targetDbPath);
            Path target2FADir = AplCoreRuntime.getInstance().get2FADir();
            twoFactorAuthMigrationExecutor.performMigration(target2FADir);
            Path targetKeystoreDir = AplCoreRuntime.getInstance().getVaultKeystoreDir();
            vaultKeystoreMigrationExecutor.performMigration(targetKeystoreDir);

            if (!dbMigrationExecutor.isAutoCleanup()) {
                dbMigrationExecutor.performAfterMigrationCleanup(targetDbPath);
            }
            if (!vaultKeystoreMigrationExecutor.isAutoCleanup()) {
                vaultKeystoreMigrationExecutor.performAfterMigrationCleanup(targetKeystoreDir);
            }
            if (!twoFactorAuthMigrationExecutor.isAutoCleanup()) {
                twoFactorAuthMigrationExecutor.performAfterMigrationCleanup(target2FADir);
            }
            publicKeyMigrator.migrate();
            referencedTransactionMigrator.migrate();
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
