/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.keystore;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.migrator.AbstractMigrationExecutorTest;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class VaultKeystoreMigrationExecutorTest extends AbstractMigrationExecutorTest {

    public VaultKeystoreMigrationExecutorTest() {
        super("apl.migrator.vaultkeystore.deleteAfterMigration", "vaultkeystoreMigrationRequired-0", "./keystore", "apl.keystoreDir");
    }

    @Override
    public MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder) {
        VaultKeystoreMigrationExecutor migrationExecutor = new VaultKeystoreMigrationExecutor(databaseManager, propertiesHolder);
        migrationExecutor.setAutoCleanup(true);
        return migrationExecutor;
    }
}
