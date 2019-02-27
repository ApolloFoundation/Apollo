/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

public class DbTestData {
    private static final DbProperties DB_FILE_PROPERTIES = new DbProperties()
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);
    public static final DbProperties DB_MEM_PROPS = new DbProperties()
            .dbUrl("jdbc:h2:mem:temp")
            .dbPassword("sa")
            .dbUsername("sa")
            .maxConnections(10)
            .loginTimeout(10)
            .maxMemoryRows(100000)
            .defaultLockTimeout(10 * 1000);

    public static DbProperties getDbFileProperties(String fileName) {
        try {
            return DB_FILE_PROPERTIES.deepCopy().dbUrl(String.format("jdbc:h2:%s", fileName));
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to create copy of dbProperties!", e);
        }
    }
}
