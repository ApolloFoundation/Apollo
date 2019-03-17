/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

import java.util.Random;

public class DbTestData {
    private static final Random random = new Random();
    private static final DbProperties DB_FILE_PROPERTIES = new DbProperties()
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);
    public static final DbProperties DB_MEM_PROPS = new DbProperties()
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);

    public static DbProperties getInMemDbProps() {
        return getDbUrlProps("jdbc:h2:mem:tempDb" + random.nextLong());
    }

    private static DbProperties getDbUrlProps(String url) {
        try {
            return DB_FILE_PROPERTIES.deepCopy().dbUrl(url);
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to create copy of dbProperties!", e);
        }
    }
    public static DbProperties getDbFileProperties(String fileName) {
        return getDbUrlProps(String.format("jdbc:h2:%s;TRACE_LEVEL_FILE=0", fileName));
    }
}
