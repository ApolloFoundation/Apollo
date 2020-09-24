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
        .maxConnections(10)
//        .dbType("tc:mariadb:10.5")
        .dbType("mariadb")
        .chainId(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"))
        .dbParams("?TC_DAEMON=true&TC_REUSABLE=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql")
        .loginTimeout(2)
        .maxMemoryRows(100000)
        .defaultLockTimeout(1)
        .maxConnections(55)
        .build();

    public static DbProperties getInMemDbProps() {
        return getDbUrlProps("jdbc:h2:mem:tempDb" + random.nextLong() + ";MV_STORE=TRUE;CACHE_SIZE=16000");
    }

    public static DbProperties getDbUrlProps(String url) {
        DbProperties dbProperties = DB_PROPERTIES.deepCopy();
//        dbProperties.setDbUrl(url);
        return dbProperties;
    }

    public static DbProperties getDbFileProperties(String fileName) {
        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000;AUTO_SERVER=TRUE", fileName));
        Path filePath = Paths.get(fileName).toAbsolutePath();
        dbProperties.setDbDir(filePath.getParent().toString());
        dbProperties.setDbName(filePath.getFileName().toString());
        return dbProperties;
    }

    public static DbProperties getDbFileProperties(GenericContainer jdbcDatabaseContainer) {
        DbProperties dbProperties = DB_PROPERTIES.deepCopy();
        dbProperties.setDbUsername(((MariaDBContainer)jdbcDatabaseContainer).getUsername());
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbProperties.setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
        dbProperties.setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbProperties.setDbName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());
        return dbProperties;
    }

    public static DbProperties getDbFilePropertiesByPath(Path dbPath) {
//        dbPath = dbPath.toAbsolutePath().toAbsolutePath();
//        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000;AUTO_SERVER=TRUE", dbPath));
//        dbProperties.setDbDir(dbPath.getParent().toString());
//        dbProperties.setDbName(dbPath.getFileName().toString());
        return DB_PROPERTIES.deepCopy();
    }
}
