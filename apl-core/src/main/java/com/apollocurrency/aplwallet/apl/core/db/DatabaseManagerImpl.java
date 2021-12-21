/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.shard.ShardNameHelper;
import com.apollocurrency.aplwallet.apl.db.updater.AplDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardAllScriptsDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardInitDBUpdater;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.db.DataSourceCreator;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class is used for high level database and shard management.
 * It keeps track on main database's data source and internal connections as well as secondary shards.
 */
@Singleton
@Slf4j
public class DatabaseManagerImpl implements ShardManagement, DatabaseManager {
    private static final int MAX_SHARD_CONNECTIONS = 10;

    private final Object lock = new Object(); // required to sync creation of shard data sources
    private final DbProperties baseDbProperties; // main database properties
    private final DataSourceCreator dataSourceCreator;

    private volatile TransactionalDataSource currentTransactionalDataSource; // main/shard database
    /**
     * Listener to close + remove evicted shard data source
     */
//    private final RemovalListener<Long, TransactionalDataSource> listener = dataSource -> {
//        if (dataSource.wasEvicted()) {
//            String cause = dataSource.getCause().name();
//            log.debug("Evicted DS, shutdown shardId = '{}', cause = {}", dataSource.getKey(), cause);
//            dataSource.getValue().shutdown();
//        }
//    };
    private final Map<Long, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(); // secondary shards
    /**
     * Cached shard data sources with timed eviction policy
     */
/*
    private LoadingCache<Long, TransactionalDataSource> connectedShardDataSourceMap = CacheBuilder.newBuilder()
            .maximumSize(MAX_CACHED_SHARDS_NUMBER) // 6 by default
            .expireAfterAccess(SHARD_EVICTION_TIME, TimeUnit.MINUTES) // 15 minutes
            .weakKeys()
            .removalListener(listener)
            .build(loader);
*/

    private Set<Long> fullShardIds = new CopyOnWriteArraySet<>(); // store full shard ids
    private volatile boolean available; // required for db hot swap
    /**
     * Shard data sources cache loader
     */
//    private final CacheLoader<Long, TransactionalDataSource> loader = new CacheLoader<>() {
//        public TransactionalDataSource load(Long shardId) throws CacheLoader.InvalidCacheLoadException {
//            log.debug("Put DS shardId = '{}' into cache...", shardId);
//            TransactionalDataSource dataSource = createAndAddShard(shardId);
//            if (dataSource == null) {
//                throw new CacheLoader.InvalidCacheLoadException("Value can't be null");
//            }
//            return dataSource;
//        }
//    };

    /**
     * Create main db instance with db properties, all other properties injected by CDI
     *  @param dbProperties          database only properties from CDI
     */
    @Inject
    public DatabaseManagerImpl(@NonNull DbProperties dbProperties, @NonNull DataSourceCreator dataSourceCreator) {
        this.baseDbProperties = dbProperties;
        this.dataSourceCreator = dataSourceCreator;
        initDatasource();
        this.available = true;
    }

    /**
     * Create, initialize and return main database source.
     *
     * @return main data source
     */
    @Override
    public TransactionalDataSource getDataSource() {
        waitAvailability();
        if (currentTransactionalDataSource == null || currentTransactionalDataSource.isShutdown()) {
            initDatasource();
        }
        return currentTransactionalDataSource;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createOrUpdateShard(Long shardId, DBUpdater dbUpdater) {
        Objects.requireNonNull(dbUpdater, "dbUpdater is null");
        long start = System.currentTimeMillis();
        waitAvailability();
//        if (connectedShardDataSourceMap.getIfPresent(shardId) == null) {
//            TransactionalDataSource dataSource = connectedShardDataSourceMap.getUnchecked(shardId);
        if (connectedShardDataSourceMap.containsKey(shardId)) {
            TransactionalDataSource dataSource = connectedShardDataSourceMap.get(shardId);
            dataSource.update(dbUpdater);
            log.debug("Init existing SHARD using db version'{}' in {} ms", dbUpdater, System.currentTimeMillis() - start);
            return dataSource;
        } else {
            return createShardDatasource(shardId, dbUpdater);
        }
    }

    private TransactionalDataSource createShardDatasource(Long shardId, DBUpdater dbUpdater) {
        long start = System.currentTimeMillis();
        waitAvailability();
        TransactionalDataSource shardDataSource = createShardDataSource(shardId, dbUpdater);
        connectedShardDataSourceMap.put(shardId, shardDataSource);
        log.debug("new SHARD datasource'{}' is ADDED in {} ms", shardDataSource.getUrl(), System.currentTimeMillis() - start);
        return shardDataSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<TransactionalDataSource> getAllFullDataSources(Long numberOfShards) {
        List<TransactionalDataSource> dataSources;
        if (numberOfShards != null) {
            dataSources = fullShardIds.stream().limit(numberOfShards).sorted(
                Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
                id, new ShardAllScriptsDBUpdater())).collect(Collectors.toList());
        } else {
            fullShardIds = findAllFullShardId();
            dataSources = fullShardIds.stream().sorted(
                Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
                id, new ShardAllScriptsDBUpdater())).collect(Collectors.toList());
        }
        return dataSources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<TransactionalDataSource> getAllFullDataSourcesIterator() {
        Set<Long> allFullShards = findAllFullShardId();
        return allFullShards.stream()
            .sorted(Comparator.reverseOrder())
            .map(id -> getOrCreateShardDataSourceById(id, new ShardAllScriptsDBUpdater()))
            .iterator();
    }

    @Override
    public Iterator<TransactionalDataSource> getAllSortedDataSourcesIterator(Comparator<TransactionalDataSource> comparator) {
        Set<Long> allFullShards = findAllFullShardId();
        return
            Stream.concat(allFullShards.stream()
                    .map(id -> getOrCreateShardDataSourceById(id, new ShardAllScriptsDBUpdater())),
                Stream.of(currentTransactionalDataSource))
                .sorted(comparator)
                .iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long closeAllShardDataSources() {
        log.debug("Prepare closing [{}] shard data source(s)", connectedShardDataSourceMap.size());
        long closedDataSources = 0;
        for (TransactionalDataSource dataSource : connectedShardDataSourceMap.values()) {
            dataSource.shutdown();
            closedDataSources++;
        }
        log.debug("Closed [{}] data source(s)", closedDataSources);
        connectedShardDataSourceMap.clear();
        return closedDataSources;
    }

    @Override
    public /*synchronized*/ TransactionalDataSource getShardDataSourceById(long shardId) {
        waitAvailability();
        return connectedShardDataSourceMap.get(shardId);
    }

    @Override
    public void initFullShards(Collection<Long> ids) {
        fullShardIds.clear();
        fullShardIds.addAll(ids);
    }

    @Override
    public void addFullShard(Long shard) {
        fullShardIds.add(shard);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource getOrCreateShardDataSourceById(Long shardId) {
        return getOrCreateShardDataSourceById(shardId, new ShardInitDBUpdater());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DBUpdater dbUpdater) {
        Objects.requireNonNull(dbUpdater, "dbUpdater is null");
        if (shardId != null && connectedShardDataSourceMap.containsKey(shardId)) {
            return connectedShardDataSourceMap.get(shardId);
        } else {
            return createOrUpdateShard(shardId, dbUpdater);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*synchronized*/ TransactionalDataSource getOrInitFullShardDataSourceById(long shardId) {
        waitAvailability();
        Set<Long> fullShards = findAllFullShardId();
        if (fullShards.contains(shardId)) {
            return getOrCreateShardDataSourceById(shardId, new ShardAllScriptsDBUpdater());
        } else {
            return null;
        }
    }

    @Override
    public DbProperties getBaseDbProperties() {
        return baseDbProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            synchronized (lock) {
                closeAllShardDataSources();
                if (currentTransactionalDataSource != null) {
                    currentTransactionalDataSource.shutdown();
                    currentTransactionalDataSource = null;
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public UUID getChainId() {
        return baseDbProperties.getChainId();
    }

    @Override
    public String toString() {
        return "DatabaseManager{" + "baseDbProperties=" + baseDbProperties +
            ", currentTransactionalDataSource=" + currentTransactionalDataSource +
            ", connectedShardDataSourceMap=" + connectedShardDataSourceMap.size() +
            '}';
    }

    private TransactionalDataSource createShardDataSource(long shardId, DBUpdater dbUpdater) {
        String shardName = getShardName(shardId);
        log.debug("Create new SHARD '{}'", shardName);
        DbProperties shardDbProperties;
        shardDbProperties = baseDbProperties.deepCopy();
        shardDbProperties.setDbName(shardName); // change file name
        shardDbProperties.setMaxConnections(MAX_SHARD_CONNECTIONS);
        shardDbProperties.setDbUrl(null);  // nullify dbUrl intentionally!;
        shardDbProperties.setDbIdentity(shardDbProperties.getDbName() != null ? shardDbProperties.getDbName() : DbProperties.DB_SYSTEM_NAME); // put shard related info
        return dataSourceCreator.createDataSource(shardDbProperties, dbUpdater);
    }

    private String getShardName(Long shardId) {
         return new ShardNameHelper().getShardNameByShardId(shardId, getChainId());
    }

    private void initDatasource() {
        currentTransactionalDataSource = dataSourceCreator.createDataSource(baseDbProperties, new AplDBUpdater());
    }

    private void waitAvailability() {
        while (!available) {
            ThreadUtils.sleep(100);
        }
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

//    private TransactionalDataSource createAndAddShard(Long shardId) {
//        long start = System.currentTimeMillis();
//        waitAvailability();
//        TransactionalDataSource shardDataSource = createShardDataSource(shardId, new ShardInitDBUpdater());
//        connectedShardDataSourceMap.put(shardId, shardDataSource);
//        log.debug("new SHARD '{}' is CREATED in {} ms", shardDataSource.getUrl(), System.currentTimeMillis() - start);
//        return shardDataSource;
//    }

}
