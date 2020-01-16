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
    private static final DbProperties DB_PROPERTIES = new DbProperties()
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .dbType("h2")
            .chainId(UUID.fromString("b5d7b697-f359-4ce5-a619-fa34b6fb01a5"))
            .dbParams("")
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);

    public static DbProperties getInMemDbProps() {
        return getDbUrlProps("jdbc:h2:mem:tempDb" + random.nextLong() + ";MV_STORE=TRUE;CACHE_SIZE=16000");
    }

    private static DbProperties getDbUrlProps(String url) {
            DbProperties dbProperties = DB_PROPERTIES.deepCopy();
            dbProperties.dbUrl(url);
            return dbProperties;
    }

    public static DbProperties getDbFileProperties(String fileName) {
        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000", fileName));
        Path filePath = Paths.get(fileName).toAbsolutePath();
        dbProperties.dbDir(filePath.getParent().toString());
        dbProperties.dbFileName(filePath.getFileName().toString());
        return dbProperties;
    }


    public static DbProperties getDbFileProperties(Path dbPath) {
        dbPath = dbPath.toAbsolutePath().toAbsolutePath();
        DbProperties dbProperties = getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0;MV_STORE=TRUE;CACHE_SIZE=16000", dbPath));
        dbProperties.dbDir(dbPath.getParent().toString());
        dbProperties.dbFileName(dbPath.getFileName().toString());
        return dbProperties;
    }
}
