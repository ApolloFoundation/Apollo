/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import com.apollocurrency.aplwallet.apl.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.util.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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

    public static void init() {
        db.init(new AplDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

    public static void tryToDeleteDb() throws IOException {
            db.shutdown();
            Logger.logInfoMessage("Removing db...");
            Path dbPath = Paths.get(Apl.getDbDir(Apl.getStringProperty(PREFIX + "Dir"))).getParent();
            removeDb(dbPath);
            Logger.logInfoMessage("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
    }
    private static void removeDb(Path dbPath) throws IOException {
        Files.walkFileTree(dbPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
