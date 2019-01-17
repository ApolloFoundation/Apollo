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

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.BasicDb;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

public final class Db {
    private static final Logger LOG = getLogger(Db.class);

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesLoader = CDI.current().select(PropertiesHolder.class).get();
    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    public static String PREFIX;
    private static BasicDb.DbProperties dbProperties;
    private static TransactionalDb db;

    public static TransactionalDb getDb() {
        if (db == null || db.isShutdown()) {
            throw new RuntimeException("Db is null or was already shutdown. Call Db.init for starting db");
        }
        return db;
    }

    public static void init(int cacheKb, String dbUrl, String dbType, String dbDir, String dbFileName, String params, String username,
                            String password,
                            int maxConnections, int loginTimeout, int defaultLockTimeout, int maxMemoryRows) {
        dbProperties =  new BasicDb.DbProperties()
                .maxCacheSize(cacheKb)
                .dbUrl(dbUrl)
                .dbType(dbType)
                .dbDir(dbDir)
                .dbParams(params)
                .dbFileName(dbFileName)
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

        PREFIX = blockchainConfig.isTestnet() ? "apl.testDb" : "apl.db";
        init(
                 propertiesLoader.getIntProperty("apl.dbCacheKB")
                , propertiesLoader.getStringProperty(PREFIX + "Url")
                , propertiesLoader.getStringProperty(PREFIX + "Type")
                , propertiesLoader.getStringProperty(PREFIX + "Dir")
                , propertiesLoader.getStringProperty(PREFIX + "Name")
                , propertiesLoader.getStringProperty(PREFIX + "Params")
                , propertiesLoader.getStringProperty(PREFIX + "Username")
                , propertiesLoader.getStringProperty(PREFIX + "Password", null, true)
                , propertiesLoader.getIntProperty("apl.maxDbConnections")
                , propertiesLoader.getIntProperty("apl.dbLoginTimeout")
                , propertiesLoader.getIntProperty("apl.dbDefaultLockTimeout") * 1000
                , propertiesLoader.getIntProperty("apl.dbMaxMemoryRows")
        );
    }
    public static void init(String dbUrl) {

        PREFIX = blockchainConfig.isTestnet() ? "apl.testDb" : "apl.db";
        init(
                propertiesLoader.getIntProperty("apl.dbCacheKB")
                , dbUrl
                , propertiesLoader.getStringProperty(PREFIX + "Type")
                , null
                , null
                , propertiesLoader.getStringProperty(PREFIX + "Params")
                , propertiesLoader.getStringProperty(PREFIX + "Username")
                , propertiesLoader.getStringProperty(PREFIX + "Password", null, true)
                , propertiesLoader.getIntProperty("apl.maxDbConnections")
                , propertiesLoader.getIntProperty("apl.dbLoginTimeout")
                , propertiesLoader.getIntProperty("apl.dbDefaultLockTimeout") * 1000
                , propertiesLoader.getIntProperty("apl.dbMaxMemoryRows")
        );
    }

    public static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

    public static void tryToDeleteDb() throws IOException {
            db.shutdown();
            LOG.info("Removing db...");
            Path dbPath = AplCoreRuntime.getInstance().getDbDir();
            removeDb(dbPath);
            LOG.info("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
    }
    public static void removeDb(Path dbPath) throws IOException {
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
