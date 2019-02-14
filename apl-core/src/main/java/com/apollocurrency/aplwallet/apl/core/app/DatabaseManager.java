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

import javax.inject.Inject;
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

import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

@Singleton
public class DatabaseManager {
    private static final Logger log = getLogger(DatabaseManager.class);

    private PropertiesHolder propertiesHolder;
    private DbProperties baseDbProperties;
    private static TransactionalDataSource currentTransactionalDataSource;
    private Map<String, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(3);

    public TransactionalDataSource getDataSource() {
        if (currentTransactionalDataSource == null || currentTransactionalDataSource.isShutdown()) {
            currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
            currentTransactionalDataSource.init(new AplDbVersion(), true);
//            throw new RuntimeException("DatabaseManager is null or was already shutdown. Call DatabaseManager.init for starting current DatabaseManager");
        }
        return currentTransactionalDataSource;
    }

    @Inject
    public DatabaseManager(DbProperties dbProperties, PropertiesHolder propertiesHolderParam) {
        baseDbProperties = dbProperties;
        propertiesHolder = propertiesHolderParam;
        currentTransactionalDataSource = new TransactionalDataSource(dbProperties, propertiesHolder);
        currentTransactionalDataSource.init(new AplDbVersion(), true);
        List<String> shardList = findAllShards(currentTransactionalDataSource);
        log.debug("Found [{}] shards...", shardList.size());
        for (String shardName : shardList) {
            DbProperties shardDbProperties = dbProperties.dbFileName(shardName);
            TransactionalDataSource shardDb = new TransactionalDataSource(shardDbProperties, propertiesHolder);
            shardDb.init(new AplDbVersion(), false);
            connectedShardDataSourceMap.put(shardName, shardDb);
            log.debug("Prepared '{}' shard...", shardName);
        }
    }

    public List<String> findAllShards(TransactionalDataSource transactionalDataSource) {
        String shardSelect = "SELECT key from shard";
        List<String> result = new ArrayList<>(3);
        try (Connection con = transactionalDataSource.getConnection();
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
    public TransactionalDataSource createAndAddShard(String shardName) {
        Objects.requireNonNull(shardName, "shardName is NULL");
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties = null;
        try {
            shardDbProperties = baseDbProperties.deepCopy().dbFileName(shardName);
        } catch (CloneNotSupportedException e) {
            log.error("DbProperties cloning error", e);
        }
        TransactionalDataSource shardDb = new TransactionalDataSource(shardDbProperties, propertiesHolder);
        shardDb.init(new AplDbVersion(), false);
        connectedShardDataSourceMap.put(shardName, shardDb);
        log.debug("new SHARD '{}' is CREATED", shardName);
        return shardDb;
    }

    public void shutdown() {
        if (connectedShardDataSourceMap.size() > 0) {
            connectedShardDataSourceMap.values().stream().forEach(DataSourceWrapper::shutdown);
        }
        if (currentTransactionalDataSource != null) {
            currentTransactionalDataSource.shutdown();
            currentTransactionalDataSource = null;
        }
    }

    public DatabaseManager() {} // never use it directly

    public static void tryToDeleteDb() throws IOException {
            currentTransactionalDataSource.shutdown();
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
