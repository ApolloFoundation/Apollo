/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

    /**
     * Shard data sources cache loader
     */
    private CacheLoader<Long, TransactionalDataSource> loader = new CacheLoader<>() {
        public TransactionalDataSource load(Long shardId) throws CacheLoader.InvalidCacheLoadException {
            log.debug("Put DS shardId = '{}' into cache...", shardId);
            TransactionalDataSource dataSource = createAndAddShard(shardId);
            if(dataSource == null){
                throw new CacheLoader.InvalidCacheLoadException("Value can't be null");
            }
            return dataSource;
        }
    };

    /**
     * Listener to close + remove evicted shard data source
     */
    private RemovalListener<Long, TransactionalDataSource> listener = dataSource -> {
        if (dataSource.wasEvicted()) {
            String cause = dataSource.getCause().name();
            log.debug("Evicted DS, shutdown shardId = '{}', cause = {}", dataSource.getKey(), cause);
            dataSource.getValue().shutdown();
        }
    };

    /**
     * Cached shard data sources with timed eviction policy
     */
    private LoadingCache<Long, TransactionalDataSource> connectedShardDataSourceMap = CacheBuilder.newBuilder()
            .maximumSize(MAX_CACHED_SHARDS_NUMBER) // 6 by default
            .expireAfterAccess(SHARD_EVICTION_TIME, TimeUnit.MINUTES) // 15 minutes
            .weakKeys()
            .removalListener(listener)
            .build(loader);

    private Jdbi jdbi;

    /**
     * Create, initialize and return main database source.
     * @return main data source
     */
    @Override
    public TransactionalDataSource getDataSource() {
        if (currentTransactionalDataSource == null || currentTransactionalDataSource.isShutdown()) {
            currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
            jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
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
        jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
//        openAllShards(); // it's not needed in most cases, because any shard opened 'lazy' by shardId
    }
//not used yet

//    /**
//     * Try to open all shard database sources specified in main db. If
//     */
//    private void openAllShards() {
//        List<Long> shardList = findAllShards(currentTransactionalDataSource);
//        log.debug("Found [{}] shards...", shardList.size());
//        for (Long shardId : shardList) {
//            String shardName = shardNameHelper.getShardNameByShardId(shardId,null); // shard's file name formatted from Id
//            DbProperties shardDbProperties = null;
//            try {
//                // create copy instance, change file name, nullify dbUrl intentionally!
//                shardDbProperties = baseDbProperties.deepCopy().dbFileName(shardName).dbUrl(null);
//            } catch (CloneNotSupportedException e) {
//                log.error("Db props clone error", e);
//            }
//            try {
//                TransactionalDataSource shardDb = new TransactionalDataSource(shardDbProperties, propertiesHolder);
//                shardDb.init(new ShardInitTableSchemaVersion());
//                connectedShardDataSourceMap.put(shardId, shardDb);
//            } catch (Exception e) {
//                log.error("Error opening shard db by name = " + shardName, e);
//            }
//            log.debug("Prepared '{}' shard...", shardName);
//        }
//    }

    @Override
    @Produces
    public Jdbi getJdbi() {
        if (jdbi == null) {
            // should never happen, but happens sometimes in unit tests because of CDI
            jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
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
                while (rs.next()) {
                    result.add(rs.getLong("shard_id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieve shards...", e);
        }
        return result;
    }

    private Set<Long> findAllFullShardId() {
        Set<Long> result = new HashSet<>();
        try (Connection con = getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT shard_id from shard where shard_state=? order by shard_height desc")) {
            pstmt.setLong(1, ShardState.FULL.getValue()); // full state shard db only
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("shard_id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error retrieve full shard Ids...", e);
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
        shardDb.init(new ShardInitTableSchemaVersion());
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
        if (connectedShardDataSourceMap.getIfPresent(shardId) == null) {
            TransactionalDataSource dataSource = connectedShardDataSourceMap.getUnchecked(shardId);
            dataSource.init(dbVersion);
            log.debug("Init existing SHARD using db version'{}' ", dbVersion);
            return dataSource;
        } else {
            ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                    new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
            TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
            shardDb.init(dbVersion);
            connectedShardDataSourceMap.put(shardId, shardDb);
            log.debug("new SHARD '{}' is CREATED", shardDataSourceCreateHelper.getShardName());
            return shardDb;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<TransactionalDataSource> getAllFullDataSources(Long numberOfShards) {
        Set<Long> allFullShards = findAllFullShardId();
        List<TransactionalDataSource> dataSources;
        if (numberOfShards != null) {
            dataSources = allFullShards.stream().limit(numberOfShards).sorted(
                    Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
                    id, new ShardAddConstraintsSchemaVersion())).collect(Collectors.toList());
        } else {
            dataSources = allFullShards.stream().sorted(
                    Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
                    id, new ShardAddConstraintsSchemaVersion())).collect(Collectors.toList());
        }
        return dataSources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<TransactionalDataSource> getAllFullDataSourcesIterator() {
        Set<Long> allFullShards = findAllFullShardId();
        Iterator<TransactionalDataSource> dataSourcesIterator = allFullShards.stream().sorted(
                    Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
                    id, new ShardAddConstraintsSchemaVersion())).iterator();
        return dataSourcesIterator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long closeAllShardDataSources() {
        log.debug("Prepare closing [{}] shard data source(s)", connectedShardDataSourceMap.size());
        long closedDataSources = 0;
        for (TransactionalDataSource dataSource : connectedShardDataSourceMap.asMap().values()) {
            dataSource.shutdown();
            closedDataSources++;
        }
        log.debug("Closed [{}] data source(s)", closedDataSources);
        connectedShardDataSourceMap.invalidateAll();
        return closedDataSources;
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

    @Override
    public /*synchronized*/ TransactionalDataSource getShardDataSourceById(long shardId) {
        return connectedShardDataSourceMap.getUnchecked(shardId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource getOrCreateShardDataSourceById(Long shardId) {
        if (shardId != null && connectedShardDataSourceMap.getIfPresent(shardId) == null) {
            return connectedShardDataSourceMap.getUnchecked(shardId);
        } else {
            return createAndAddShard(shardId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        if (shardId != null && connectedShardDataSourceMap.getIfPresent(shardId) == null) {
            return connectedShardDataSourceMap.getUnchecked(shardId);
        } else {
            return createAndAddShard(shardId, dbVersion);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*synchronized*/ TransactionalDataSource getOrInitFullShardDataSourceById(long shardId) {
        Set<Long> fullShards = findAllFullShardId();
        if (fullShards.contains(shardId)) {
            return getOrCreateShardDataSourceById(shardId, new ShardAddConstraintsSchemaVersion());
        } else {
            return null;
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
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdown() {
        try {
            closeAllShardDataSources();
            if (currentTransactionalDataSource != null) {
                currentTransactionalDataSource.shutdown();
                currentTransactionalDataSource = null;
                jdbi = null;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
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

    @Override
    public UUID getChainId() {
        return baseDbProperties.getChainId();
    }

    public DatabaseManagerImpl() {} // never use it directly

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
