/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;

import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.migrator.auth2fa.TwoFactorAuthMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.db.DbMigrationExecutor;
import com.apollocurrency.aplwallet.apl.core.migrator.keystore.VaultKeystoreMigrationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDataMigrationManager {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDataMigrationManager.class);

    public void executeDataMigration() {
        VaultKeystoreMigrationExecutor vaultKeystoreMigrationExecutor = CDI.current().select(VaultKeystoreMigrationExecutor.class).get();
        DbMigrationExecutor dbMigrationExecutor = CDI.current().select(DbMigrationExecutor.class).get();
        TwoFactorAuthMigrationExecutor twoFactorAuthMigrationExecutor = CDI.current().select(TwoFactorAuthMigrationExecutor.class).get();
        try {
            dbMigrationExecutor.performMigration(AplCoreRuntime.getInstance().getDbDir().resolve(Constants.APPLICATION_DIR_NAME));
            vaultKeystoreMigrationExecutor.performMigration(AplCoreRuntime.getInstance().getVaultKeystoreDir());
            twoFactorAuthMigrationExecutor.performMigration(AplCoreRuntime.getInstance().get2FADir());
        }
        catch (IOException e) {
            LOG.error("Fatal error. Cannot proceed data migration", e);
            System.exit(-1);
        }
    }
}
