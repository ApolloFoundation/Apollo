/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.migrator.auth2fa;

import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.migrator.AbstractMigrationExecutorTest;
import com.apollocurrency.aplwallet.apl.core.migrator.MigrationExecutor;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

public class TwoFactorAuthMigrationExecutorTest extends AbstractMigrationExecutorTest {

    public TwoFactorAuthMigrationExecutorTest() {
        super("apl.migrator.2fa.deleteAfterMigration", "2faMigrationRequired-0", "./keystore/2fa", "apl.dir2FA");
    }

    @Override
    public MigrationExecutor getExecutor(DatabaseManager databaseManager, PropertiesHolder propertiesHolder) {
        return new TwoFactorAuthMigrationExecutor(databaseManager, propertiesHolder);
    }
}
