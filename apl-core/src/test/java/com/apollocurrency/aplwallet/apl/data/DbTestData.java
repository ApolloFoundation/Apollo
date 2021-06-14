/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import com.apollocurrency.aplwallet.apl.util.env.config.BlockchainProperties;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.FeaturesHeightRequirement;
import com.apollocurrency.aplwallet.apl.util.injectable.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
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

    private static final BlockchainProperties bp0 = new BlockchainProperties(0, 0, 160, 0, 1, 2, 0, 100L);
    private static final List<BlockchainProperties> BLOCKCHAIN_PROPERTIES = Arrays.asList(
        bp0
    );
    private static final Chain chain = new Chain(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"),
        true, Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList(),
        "Apollo test",
        "Apollo test",
        "Apollo",
        "APL",
        "Apollo Test",
        10000L, 2,
        //"data.json",
        BLOCKCHAIN_PROPERTIES,
        new FeaturesHeightRequirement(100, 100, 100),
        Collections.emptySet());

    private static Properties TEST_PROPERTIES = new Properties();
    private static ChainsConfigHolder testHolder;

    static {
        TEST_PROPERTIES.put("apl.dbType", "mariadb");
        TEST_PROPERTIES.put("apl.dbUsername", "testuser");
        TEST_PROPERTIES.put("apl.dbPassword", "testpass");
        TEST_PROPERTIES.put("apl.maxDbConnections", "10");
        TEST_PROPERTIES.put("apl.dbLoginTimeout", "2");
        TEST_PROPERTIES.put("apl.dbDefaultLockTimeout", "1");
        TEST_PROPERTIES.put("apl.dbMaxMemoryRows", "100000");
        TEST_PROPERTIES.put("apl.dbParams", "&TC_DAEMON=true&TC_REUSABLE=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql");
        HashMap<UUID, Chain> chains = new HashMap<>();
        chains.put(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"), chain);
        testHolder = new ChainsConfigHolder(chains);
    }

    public static DbConfig getInMemDbConfig() {
        return new DbConfig(new PropertiesHolder(TEST_PROPERTIES), testHolder);
    }

    public static DbProperties getInMemDbProps() {
        DbProperties dbUrlProps = getDbUrlProps();
        return dbUrlProps;
    }

    public static DbProperties getDbUrlProps() {
        DbProperties dbProperties = DB_PROPERTIES.deepCopy();
        return dbProperties;
    }

    public static DbConfig getDbFileProperties(String fileName) {
        DbConfig dbConfig = new DbConfig(new PropertiesHolder(TEST_PROPERTIES), testHolder);
        Path filePath = Paths.get(fileName).toAbsolutePath();
        dbConfig.getDbProperties().setDbDir(filePath.getParent().toString());
        dbConfig.getDbProperties().setDbName(filePath.getFileName().toString());
        return dbConfig;
    }

    public static DbConfig getDbFileProperties(GenericContainer jdbcDatabaseContainer) {
        DbConfig dbConfig = new DbConfig(new PropertiesHolder(TEST_PROPERTIES), testHolder);
        dbConfig.getDbProperties().setDbUsername(((MariaDBContainer) jdbcDatabaseContainer).getUsername());
        if (((MariaDBContainer) jdbcDatabaseContainer).getPassword() != null
            && !((MariaDBContainer) jdbcDatabaseContainer).getPassword().isEmpty()) {
            dbConfig.getDbProperties().setDbPassword(((MariaDBContainer) jdbcDatabaseContainer).getPassword());
        }
        if (jdbcDatabaseContainer.getMappedPort(3306) != null) {
            dbConfig.getDbProperties().setDatabasePort(jdbcDatabaseContainer.getMappedPort(3306));
        }
//        dbProperties.setDatabasePort(3306); // docker container default for quick test only
        dbConfig.getDbProperties().setDatabaseHost(jdbcDatabaseContainer.getHost());
        dbConfig.getDbProperties().setDbName(((MariaDBContainer<?>) jdbcDatabaseContainer).getDatabaseName());
        dbConfig.getDbProperties().setSystemDbUrl(dbConfig.getDbProperties().formatJdbcUrlString(true));
        return dbConfig;
    }

    public static DbProperties getDbFilePropertiesByPath(Path dbPath) {
        return DB_PROPERTIES.deepCopy();
    }
}
