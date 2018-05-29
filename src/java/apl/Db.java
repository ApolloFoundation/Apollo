/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation B.V.,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl;

import apl.db.BasicDb;
import apl.db.TransactionalDb;

import java.sql.SQLException;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "apl.testDb" : "apl.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Apl.getIntProperty("apl.dbCacheKB"))
            .dbUrl(Apl.getStringProperty(PREFIX + "Url"))
            .dbType(Apl.getStringProperty(PREFIX + "Type"))
            .dbDir(Apl.getStringProperty(PREFIX + "Dir"))
            .dbParams(Apl.getStringProperty(PREFIX + "Params"))
            .dbUsername(Apl.getStringProperty(PREFIX + "Username"))
            .dbPassword(Apl.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Apl.getIntProperty("apl.maxDbConnections"))
            .loginTimeout(Apl.getIntProperty("apl.dbLoginTimeout"))
            .defaultLockTimeout(Apl.getIntProperty("apl.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Apl.getIntProperty("apl.dbMaxMemoryRows"))
    );

    public static void init() throws SQLException {
        db.init(new AplDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
