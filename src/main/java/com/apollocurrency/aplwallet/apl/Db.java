/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.apollocurrency.aplwallet.apl.db.BasicDb;
import com.apollocurrency.aplwallet.apl.db.TransactionalDb;
import org.slf4j.Logger;

public final class Db {
    private static final Logger LOG = getLogger(Db.class);


    public static final String PREFIX = Constants.isTestnet() ? "apl.testDb" : "apl.db";
    private static BasicDb.DbProperties dbProperties;
    private static TransactionalDb db;

    public static TransactionalDb getDb() {
        if (db == null || db.isShutdown()) {
            throw new RuntimeException("Db is null or was already shutdown. Call Db.init for starting db");
        }
        return db;
    }

    public static void init(int cacheKb, String dbUrl, String dbType, String dbDir, String params, String username, String password,
                            int maxConnections, int loginTimeout, int defaultLockTimeout, int maxMemoryRows) {
        dbProperties =  new BasicDb.DbProperties()
                .maxCacheSize(cacheKb)
                .dbUrl(dbUrl)
                .dbType(dbType)
                .dbDir(dbDir)
                .dbParams(params)
                .dbUsername(username)
                .dbPassword(password)
                .maxConnections(maxConnections)
                .loginTimeout(loginTimeout)
                .defaultLockTimeout(defaultLockTimeout)
                .maxMemoryRows(maxMemoryRows);
        db = new TransactionalDb(dbProperties);
        db.init(new AplDbVersion());
    }

    public static void init() {
        init(
                Apl.getIntProperty("apl.dbCacheKB")
                , Apl.getStringProperty(PREFIX + "Url")
                , Apl.getStringProperty(PREFIX + "Type")
                , Apl.getStringProperty(PREFIX + "Dir")
                , Apl.getStringProperty(PREFIX + "Params")
                , Apl.getStringProperty(PREFIX + "Username")
                , Apl.getStringProperty(PREFIX + "Password", null, true)
                , Apl.getIntProperty("apl.maxDbConnections")
                , Apl.getIntProperty("apl.dbLoginTimeout")
                , Apl.getIntProperty("apl.dbDefaultLockTimeout") * 1000
                ,Apl.getIntProperty("apl.dbMaxMemoryRows")
        );
    }
    public static void init(String dbUrl) {
        init(
                Apl.getIntProperty("apl.dbCacheKB")
                , dbUrl
                , Apl.getStringProperty(PREFIX + "Type")
                , null
                , Apl.getStringProperty(PREFIX + "Params")
                , Apl.getStringProperty(PREFIX + "Username")
                , Apl.getStringProperty(PREFIX + "Password", null, true)
                , Apl.getIntProperty("apl.maxDbConnections")
                , Apl.getIntProperty("apl.dbLoginTimeout")
                , Apl.getIntProperty("apl.dbDefaultLockTimeout") * 1000
                , Apl.getIntProperty("apl.dbMaxMemoryRows")
        );
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

    public static void tryToDeleteDb() throws IOException {
            db.shutdown();
            LOG.info("Removing db...");
            Path dbPath = Paths.get(Apl.getDbDir(Apl.getStringProperty(PREFIX + "Dir"))).getParent();
            removeDb(dbPath);
            LOG.info("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
    }
    static void removeDb(Path dbPath) throws IOException {
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
