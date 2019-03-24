/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

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
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Class is used for high level database and shard management.
 * It keeps track on main database's data source and internal connections as well as secondary shards.
 */
@Singleton
public class DatabaseManagerImpl implements ShardManagement, DatabaseManager {
    private static final Logger log = getLogger(DatabaseManagerImpl.class);

    private DbProperties baseDbProperties; // main database properties
    private PropertiesHolder propertiesHolder;
    private TransactionalDataSource currentTransactionalDataSource; // main/shard database
    private Map<Long, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(); // secondary shards
    private Jdbi jdbi;

    /**
     * Create, initialize and return main database source.
     * @return main data source
     */
    @Override
    public TransactionalDataSource getDataSource() {
        if (currentTransactionalDataSource == null || currentTransactionalDataSource.isShutdown()) {
            currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
            jdbi = currentTransactionalDataSource.init(new AplDbVersion());
        }
        return currentTransactionalDataSource;
    }

    /**
     * Create main db instance with db properties, all other properties injected by CDI
     * @param dbProperties database only properties from CDI
     * @param propertiesHolderParam the rest global properties in holder from CDI
     */
    @Inject
    public DatabaseManagerImpl(DbProperties dbProperties, PropertiesHolder propertiesHolderParam) {
        baseDbProperties = Objects.requireNonNull(dbProperties, "Db Properties cannot be null");
        propertiesHolder = propertiesHolderParam;
        // init internal data source stuff only one time till next shutdown() will be called
        currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
        jdbi = currentTransactionalDataSource.init(new AplDbVersion());
//        openAllShards(); // it's not needed in most cases, because any shard opened 'lazy' by shardId
    }

    /**
     * Try to open all shard database sources specified in main db. If
     */
    private void openAllShards() {
        List<Long> shardList = findAllShards(currentTransactionalDataSource);
        log.debug("Found [{}] shards...", shardList.size());
        for (Long shardId : shardList) {
            String shardName = ShardNameHelper.getShardNameByShardId(shardId); // shard's file name formatted from Id
            DbProperties shardDbProperties = null;
            try {
                // create copy instance, change file name, nullify dbUrl intentionally!
                shardDbProperties = baseDbProperties.deepCopy().dbFileName(shardName).dbUrl(null);
            } catch (CloneNotSupportedException e) {
                log.error("Db props clone error", e);
            }
            try {
                TransactionalDataSource shardDb = new TransactionalDataSource(shardDbProperties, propertiesHolder);
                shardDb.init(new AplDbVersion());
                connectedShardDataSourceMap.put(shardId, shardDb);
            } catch (Exception e) {
                log.error("Error opening shard db by name = " + shardName, e);
            }
            log.debug("Prepared '{}' shard...", shardName);
        }
    }

    @Override
    @Produces
    public Jdbi getJdbi() {
        if (jdbi == null) {
            // should never happen, but happens sometimes in unit tests because of CDI
            jdbi = currentTransactionalDataSource.init(new AplDbVersion());
        }
        return jdbi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> findAllShards(TransactionalDataSource transactionalDataSource) {
        Objects.requireNonNull(transactionalDataSource, "DataSource cannot be null");
        String shardSelect = "SELECT shard_id from shard";
        List<Long> result = new ArrayList<>();
        try (Connection con = transactionalDataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement(shardSelect)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    result.add(rs.getLong("shard_id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieve shards...", e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createAndAddShard(Long shardId) {
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        shardDb.init(new AplDbVersion());
        connectedShardDataSourceMap.put(shardDataSourceCreateHelper.getShardId(), shardDb);
        log.debug("new SHARD '{}' is CREATED", shardDataSourceCreateHelper.getShardName());
        return shardDb;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createAndAddShard(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        shardId = shardDataSourceCreateHelper.getShardId();
        if (connectedShardDataSourceMap.containsKey(shardId)) {
            TransactionalDataSource dataSource = connectedShardDataSourceMap.get(shardId);
            if (dataSource != null && !dataSource.isShutdown()) {
                shutdown(dataSource);
                shardDb.init(dbVersion);
            }
            connectedShardDataSourceMap.replace(shardId, shardDb);
        } else {
            shardDb.init(dbVersion);
            connectedShardDataSourceMap.put(shardId, shardDb);
        }
        log.debug("new SHARD '{}' is CREATED", shardDataSourceCreateHelper.getShardName());
        return shardDb;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createAndAddTemporaryDb(String temporaryDatabaseName) {
        Objects.requireNonNull(temporaryDatabaseName, "temporary Database Name is NULL");
        log.debug("Create new SHARD '{}'", temporaryDatabaseName);
        if (temporaryDatabaseName.isEmpty() || temporaryDatabaseName.length() > 255) {
            String error = String.format(
                    "Parameter for temp database name is EMPTY or TOO LONG (>255 symbols) = '%s'", temporaryDatabaseName.length());
            log.error(error);
            throw new RuntimeException(error);
        }
        DbProperties shardDbProperties = null;
        try {
            shardDbProperties = baseDbProperties.deepCopy().dbFileName(temporaryDatabaseName)
                    .dbUrl(null) // nullify dbUrl intentionally!;
                    .dbIdentity(TEMP_DB_IDENTITY);
        } catch (CloneNotSupportedException e) {
            log.error("DbProperties cloning error", e);
        }
        TransactionalDataSource temporaryDataSource = new TransactionalDataSource(shardDbProperties, propertiesHolder);
        temporaryDataSource.init(new AplDbVersion());
        connectedShardDataSourceMap.put(TEMP_DB_IDENTITY, temporaryDataSource); // put temporary DS with special ID
        log.debug("new temporaryDataSource '{}' is CREATED", temporaryDatabaseName);
        return temporaryDataSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource getOrCreateShardDataSourceById(Long shardId) {
        if (shardId != null && connectedShardDataSourceMap.containsKey(shardId)) {
            return connectedShardDataSourceMap.get(shardId);
        } else {
            return createAndAddShard(shardId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        if (shardId != null && connectedShardDataSourceMap.containsKey(shardId)) {
            return connectedShardDataSourceMap.get(shardId);
        } else {
            return createAndAddShard(shardId, dbVersion);
        }
    }

    @Override
    public DbProperties getBaseDbProperties() {
        return baseDbProperties;
    }

    @Override
    public PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
    }

    /**
     * Shutdown main db and secondary shards.
     * After that the db can be reinitialized/opened again
     */
    @Override
    public void shutdown() {
        if (connectedShardDataSourceMap.size() > 0) {
            connectedShardDataSourceMap.values().stream().forEach(DataSourceWrapper::shutdown);
        }
        if (currentTransactionalDataSource != null) {
            currentTransactionalDataSource.shutdown();
            currentTransactionalDataSource = null;
            jdbi = null;
        }
    }

    /**
     * Be CAREFUL using this method. It's better to use it for explicit DataSources (like temporary)
     * @param dataSource not null data source to be closed
     */
    @Override
    public void shutdown(TransactionalDataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource is NULL");
        dataSource.shutdown();
    }

    public DatabaseManagerImpl() {} // never use it directly

//    /**
//     * Optional method, needs revising for shards
//     * @throws IOException
//     */
//    public static void tryToDeleteDb() throws IOException {
//            currentTransactionalDataSource.shutdown();
//            log.info("Removing current Db...");
//            Path dbPath = AplCoreRuntime.getInstance().getDbDir();
//            removeDb(dbPath);
//            log.info("Db: " + dbPath.toAbsolutePath().toString() + " was successfully removed!");
//    }

    /**
     * Optional method, needs revising for shards
     * @param dbPath path to db folder
     * @throws IOException
     */
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DatabaseManager{");
        sb.append("baseDbProperties=").append(baseDbProperties);
        sb.append(", propertiesHolder=[{}]").append(propertiesHolder != null ? propertiesHolder : -1);
        sb.append(", currentTransactionalDataSource={}").append(currentTransactionalDataSource != null ? "initialized" : "NULL");
        sb.append(", connectedShardDataSourceMap=[{}]").append(connectedShardDataSourceMap != null ? connectedShardDataSourceMap.size() : -1);
        sb.append('}');
        return sb.toString();
    }

}
