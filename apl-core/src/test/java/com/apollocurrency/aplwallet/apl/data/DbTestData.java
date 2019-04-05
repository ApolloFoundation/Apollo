/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.nio.file.Paths;
import java.util.Random;

public class DbTestData {
    private static final Random random = new Random();
    private static final DbProperties DB_PROPERTIES = new DbProperties()
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .dbType("h2")
            .dbParams("")
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);

    public static DbProperties getInMemDbProps() {
        return getDbUrlProps("jdbc:h2:mem:tempDb" + random.nextLong(), null);
    }

    private static DbProperties getDbUrlProps(String url, String dbDir) {
        try {
            DbProperties dbProperties = DB_PROPERTIES.deepCopy();
            dbProperties.dbUrl(url);
            dbProperties.dbDir(dbDir);
            return dbProperties;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to create copy of dbProperties!", e);
        }
    }
    public static DbProperties getDbFileProperties(String fileName) {
        return getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0", fileName), Paths.get(fileName).getParent().toAbsolutePath().toString());
    }
}
