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

import com.apollocurrency.aplwallet.apl.core.db.BasicDb;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public final class Db {
    private static final Logger LOG = getLogger(Db.class);

    // TODO: YL remove static instance later
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();

    private static TransactionalDb db;

    public static TransactionalDb getDb() {
        if (db == null || db.isShutdown()) {
            throw new RuntimeException("Db is null or was already shutdown. Call Db.init for starting db");
        }
        return db;
    }

    public static void init(BasicDb.DbProperties dbProperties) {
        db = new TransactionalDb(dbProperties);
        db.init(new AplDbVersion());
    }

    public static void init() {
        init("");
    }
    public static void init(String dbUrl) {
        BasicDb.DbProperties dbProperties = new BasicDb.DbProperties()
                .maxCacheSize(propertiesHolder.getIntProperty("apl.dbCacheKB"))
                .dbUrl(StringUtils.isBlank(dbUrl) ? propertiesHolder.getStringProperty("apl.dbUrl") : dbUrl)
                .dbType(propertiesHolder.getStringProperty("apl.dbType"))
                .dbDir(AplCoreRuntime.getInstance().getDbDir().toAbsolutePath().toString())
                .dbFileName(Constants.APPLICATION_DIR_NAME)
                .dbParams(propertiesHolder.getStringProperty("apl.dbParams"))
                .dbUsername(propertiesHolder.getStringProperty("apl.dbUsername"))
                .dbPassword(propertiesHolder.getStringProperty("apl.dbPassword", null, true))
                .maxConnections(propertiesHolder.getIntProperty("apl.maxDbConnections"))
                .loginTimeout(propertiesHolder.getIntProperty("apl.dbLoginTimeout"))
                .defaultLockTimeout(propertiesHolder.getIntProperty("apl.dbDefaultLockTimeout") * 1000)
                .maxMemoryRows(propertiesHolder.getIntProperty("apl.dbMaxMemoryRows")
                );
        init(dbProperties);
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
