/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;

public class DbTestData {
    private static final Random random = new Random();
    private static final DbProperties DB_PROPERTIES = DbProperties.builder()
        .dbPassword("mypass")
//        .dbUsername("root")
        .dbUsername("testuser")
        .maxConnections(10)
        .dbType("tc:mariadb:10.4")
//        .dbType("tc:apl-mariadb-rocksdb:10.4")
        .chainId(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"))
        .dbParams("?TC_DAEMON=true&TC_INITSCRIPT=file:src/test/resources/db/schema.sql")
        .loginTimeout(10)
        .maxMemoryRows(100000)
        .defaultLockTimeout(10)
        .build();

    public static DbProperties getInMemDbProps() {
        return getDbUrlProps("jdbc:h2:mem:tempDb" + random.nextLong() + ";MV_STORE=TRUE;CACHE_SIZE=16000");
    }

    private static DbProperties getDbUrlProps(String url) {
        DbProperties dbProperties = DB_PROPERTIES.deepCopy();
//        dbProperties.setDbUrl(url);
        return dbProperties;
    }

    public static DbProperties getDbFileProperties(String fileName) {
        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000;AUTO_SERVER=TRUE", fileName));
        Path filePath = Paths.get(fileName).toAbsolutePath();
        dbProperties.setDbDir(filePath.getParent().toString());
        dbProperties.setDbFileName(filePath.getFileName().toString());
        return dbProperties;
    }


    public static DbProperties getDbFileProperties(Path dbPath) {
        dbPath = dbPath.toAbsolutePath().toAbsolutePath();
        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000;AUTO_SERVER=TRUE", dbPath));
        dbProperties.setDbDir(dbPath.getParent().toString());
        dbProperties.setDbFileName(dbPath.getFileName().toString());
        return dbProperties;
    }
}
