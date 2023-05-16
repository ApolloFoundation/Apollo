/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.MigrationParams;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class H2DatabaseAdministratorTest {
    @Mock
    DBUpdater dbUpdater;

    @Test
    void createDatabaseAndMigrate_customUrl() {
        String predefinedUrl = "jdbc:h2:file:/file";
        H2DatabaseAdministrator administrator = new H2DatabaseAdministrator(DbProperties.builder()
            .dbUrl(predefinedUrl)
            .dbType("h2")
            .dbPassword("pass")
            .dbUsername("user").build());

        String targetDbUrl = administrator.createDatabase();

        assertEquals(predefinedUrl, targetDbUrl);

        administrator.migrateDatabase(dbUpdater);

        verify(dbUpdater).update(new MigrationParams(predefinedUrl, "h2", "user", "pass"));
    }

    @Test
    void migrateDatabase_separateParams() {
        String resultUrl = "jdbc:h2:/home/user/dir/test;";
        H2DatabaseAdministrator administrator = new H2DatabaseAdministrator(DbProperties.builder()
            .dbDir("/home/user/dir")
            .dbName("test")
            .dbType("h2")
            .dbPassword("pass")
            .dbUsername("user").build());

        String targetDbUrl = administrator.createDatabase();

        assertEquals(resultUrl, targetDbUrl);

        administrator.migrateDatabase(dbUpdater);

        verify(dbUpdater).update(new MigrationParams(resultUrl, "h2", "user", "pass"));
    }
}