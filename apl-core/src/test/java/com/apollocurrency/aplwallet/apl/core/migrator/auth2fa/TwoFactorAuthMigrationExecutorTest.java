/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.auth2fa;

import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.migrator.AbstractMigrationExecutorTest;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TwoFactorAuthMigrationExecutorTest extends AbstractMigrationExecutorTest {
    @RegisterExtension
    TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    public TwoFactorAuthMigrationExecutorTest() {
        super("apl.migrator.2fa.deleteAfterMigration", "2faMigrationRequired-0", "./keystore/2fa", "apl.dir2FA");
    }

    @Override
    public MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder) {
        return new TwoFactorAuthMigrationExecutor(databaseManager, propertiesHolder);
    }
    @Override
    public TemporaryFolderExtension getTempFolder() {
        return temporaryFolderExtension;
    }
}
