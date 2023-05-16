/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatabaseAdministratorFactoryImplTest {
    @Mock
    DirProvider dirProvider;

    @Test
    void createMariaDbAdmin_url() {
        DbProperties dbProperties = DbProperties.builder()
            .dbUrl("jdbc:p6spy:mariadb://localhost:9898/apl")
            .build();
        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);
        DatabaseAdministrator dbAdmin = factory.createDbAdmin(dbProperties);
        assertEquals(MariadbDatabaseAdministrator.class, dbAdmin.getClass());
    }

    @Test
    void createMariaDbAdmin_dbType() {
        DbProperties dbProperties = DbProperties.builder()
            .dbType("mariadb")
            .build();
        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);
        DatabaseAdministrator dbAdmin = factory.createDbAdmin(dbProperties);
        assertEquals(MariadbDatabaseAdministrator.class, dbAdmin.getClass());
    }

    @Test
    void createH2DbAdmin_url() {
        DbProperties dbProperties = DbProperties.builder()
            .dbUrl("jdbc:h2:file:/home/myuser/db;MODE=MYSQL")
            .build();
        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);
        DatabaseAdministrator dbAdmin = factory.createDbAdmin(dbProperties);
        assertEquals(H2DatabaseAdministrator.class, dbAdmin.getClass());
    }

    @Test
    void createH2DbAdmin_dbType() {
        DbProperties dbProperties = DbProperties.builder()
            .dbType("h2")
            .build();
        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);
        DatabaseAdministrator dbAdmin = factory.createDbAdmin(dbProperties);
        assertEquals(H2DatabaseAdministrator.class, dbAdmin.getClass());
    }

    @Test
    void createDbAdmin_notSupportedDbType() {
        DbProperties dbProperties = DbProperties.builder()
            .dbType("mysql")
            .build();

        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);

        assertThrows(IllegalArgumentException.class, () -> factory.createDbAdmin(dbProperties));
    }

    @Test
    void createDbAdmin_notSupportedUrl() {
        DbProperties dbProperties = DbProperties.builder()
            .dbUrl("jdbc:oracle://localhost:1111")
            .build();

        DatabaseAdministratorFactoryImpl factory = new DatabaseAdministratorFactoryImpl(dirProvider);

        assertThrows(IllegalArgumentException.class, () -> factory.createDbAdmin(dbProperties));
    }
}