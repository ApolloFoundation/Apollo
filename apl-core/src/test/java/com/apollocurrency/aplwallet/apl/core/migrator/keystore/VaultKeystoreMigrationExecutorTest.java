/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.keystore;

import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.migrator.AbstractMigrationExecutorTest;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.extension.RegisterExtension;

public class VaultKeystoreMigrationExecutorTest extends AbstractMigrationExecutorTest {
    @RegisterExtension
    TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    public VaultKeystoreMigrationExecutorTest() {
        super("apl.migrator.vaultkeystore.deleteAfterMigration", "vaultkeystoreMigrationRequired-0", "./keystore", "apl.keystoreDir");
    }

    @Override
    public MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder) {
        VaultKeystoreMigrationExecutor migrationExecutor = new VaultKeystoreMigrationExecutor(databaseManager, propertiesHolder);
        migrationExecutor.setAutoCleanup(true);
        return migrationExecutor;
    }
    @Override
    public TemporaryFolderExtension getTempFolder() {
        return temporaryFolderExtension;
    }
}
