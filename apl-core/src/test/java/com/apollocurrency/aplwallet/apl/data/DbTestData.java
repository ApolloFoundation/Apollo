/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;

public class DbTestData {
    private static final Random random = new Random();
    private static final DbProperties DB_PROPERTIES = DbProperties.builder()
        .dbName("testdb")
        .dbUsername("testuser")
        .dbPassword("testpass")
//        .databasePort(3306) // docker container default for quick test only
        .maxConnections(10)
        .dbType("mariadb")
        .chainId(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"))
        .dbParams("&TC_DAEMON=true&TC_REUSABLE=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql")
        .loginTimeout(2)
        .maxMemoryRows(100000)
        .defaultLockTimeout(1)
        .build();

    public static DbProperties getInMemDbProps() {
        DbProperties dbUrlProps = getDbUrlProps();
        return dbUrlProps;
    }

    public static DbProperties getDbUrlProps() {
        DbProperties dbProperties = DB_PROPERTIES.deepCopy();
        return dbProperties;
    }

    public static DbProperties getDbFileProperties(String fileName) {
        DbProperties dbProperties = getDbUrlProps();
        Path filePath = Paths.get(fileName).toAbsolutePath();
        dbProperties.setDbDir(filePath.getParent().toString());
        dbProperties.setDbName(filePath.getFileName().toString());
        return dbProperties;
    }

    public static DbProperties getDbFileProperties(GenericContainer jdbcDatabaseContainer) {
        DbProperties dbProperties = getDbUrlProps();
        dbProperties.setDbUsername(((MariaDBContainer) jdbcDatabaseContainer).getUsername());
        if (((MariaDBContainer) jdbcDatabaseContainer).getPassword() != null && !((MariaDBContainer) jdbcDatabaseContainer).getPassword().isEmpty()) {
            dbProperties.setDbPassword(((MariaDBContainer) jdbcDatabaseContainer).getPassword());
        }
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbProperties.setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
//        dbProperties.setDatabasePort(3306); // docker container default for quick test only
        dbProperties.setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbProperties.setDbName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());
        dbProperties.setSystemDbUrl(dbProperties.formatJdbcUrlString(true));
        return dbProperties;
    }

    public static DbProperties getDbFilePropertiesByPath(Path dbPath) {
        return DB_PROPERTIES.deepCopy();
    }
}
