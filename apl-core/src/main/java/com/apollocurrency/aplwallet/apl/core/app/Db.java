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
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDb;
import org.slf4j.Logger;

@Singleton
public class Db {
    private static final Logger log = getLogger(Db.class);

    private static DbProperties baseDbProperties;
    private static TransactionalDb currentDb;
    private static Map<String, TransactionalDb> shards = new ConcurrentHashMap<>(3);

    public static TransactionalDb getDb() {
        if (currentDb == null || currentDb.isShutdown()) {
            throw new RuntimeException("Db is null or was already shutdown. Call Db.init for starting current Db");
        }
        return currentDb;
    }

    public static void init(DbProperties dbProperties) {
        baseDbProperties = dbProperties;
        currentDb = new TransactionalDb(dbProperties);
        currentDb.init(new AplDbVersion(), true);
        List<String> shardList = trySelectShard(currentDb);
        log.debug("Found [{}] shards...", shardList.size());
        for (String shardName : shardList) {
            DbProperties shardDbProperties = dbProperties.dbFileName(shardName);
            TransactionalDb shardDb = new TransactionalDb(shardDbProperties);
            shardDb.init(new AplDbVersion(), false);
            shards.put(shardName, shardDb);
            log.debug("Prepared '{}' shard...", shardName);
        }
    }

    private static List<String> trySelectShard(TransactionalDb db) {
        String shardSelect = "SELECT key from shard";
        List<String> result = new ArrayList<>(3);
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(shardSelect)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("key"));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieve shards...", e);
        }
        return result;
    }

    /**
     * Method gives ability to create new 'shard database', open existing shard and add it into shard list.
     * @param shardName shard name to be added
     * @return shard database connection pool
     */
    public TransactionalDb createAndAddShard(String shardName) {
        Objects.requireNonNull(shardName, "shardName is NULL");
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties = null;
        try {
            shardDbProperties = baseDbProperties.deepCopy().dbFileName(shardName);
        } catch (CloneNotSupportedException e) {
            log.error("DbProperties cloning error", e);
        }
        TransactionalDb shardDb = new TransactionalDb(shardDbProperties);
        shardDb.init(new AplDbVersion(), false);
        shards.put(shardName, shardDb);
        log.debug("new SHARD '{}' is CREATED", shardName);
        return shardDb;
    }

/*
    public static void init() {
        DbProperties dbProperties = CDI.current().select(DbProperties.class).get();
        init(dbProperties);
    }
*/

    public static void shutdown() {
        if (shards.size() > 0) {
            shards.values().stream().forEach(shard -> shard.shutdown());
        }
        currentDb.shutdown();
    }

    public Db() {} // never use it directly

    public static void tryToDeleteDb() throws IOException {
            currentDb.shutdown();
            log.info("Removing current Db...");
            Path dbPath = AplCoreRuntime.getInstance().getDbDir();
            removeDb(dbPath);
            log.info("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
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
