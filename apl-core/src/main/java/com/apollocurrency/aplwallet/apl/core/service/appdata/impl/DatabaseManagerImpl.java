/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata.impl;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardState;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.db.updater.AplDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardAllScriptsDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardInitDBUpdater;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import javax.enterprise.inject.Produces;
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

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Class is used for high level database and shard management.
 * It keeps track on main database's data source and internal connections as well as secondary shards.
 */
@Singleton
public class DatabaseManagerImpl implements ShardManagement, DatabaseManager {
    private static final Logger log = getLogger(DatabaseManagerImpl.class);
    private final Object lock = new Object(); // required to sync creation of shard datasources
    private DbProperties baseDbProperties; // main database properties
    private PropertiesHolder propertiesHolder;
    private volatile TransactionalDataSource currentTransactionalDataSource; // main/shard database
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
    private Map<Long, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(); // secondary shards
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

    private Jdbi jdbi;
    private JdbiHandleFactory jdbiHandleFactory;
    private Set<Long> fullShardIds = new CopyOnWriteArraySet<>(); // store full shard ids
    private boolean available; // required for db hot swap
    /**
     * Shard data sources cache loader
     */
    private CacheLoader<Long, TransactionalDataSource> loader = new CacheLoader<>() {
        public TransactionalDataSource load(Long shardId) throws CacheLoader.InvalidCacheLoadException {
            log.debug("Put DS shardId = '{}' into cache...", shardId);
            TransactionalDataSource dataSource = createAndAddShard(shardId);
            if (dataSource == null) {
                throw new CacheLoader.InvalidCacheLoadException("Value can't be null");
            }
            return dataSource;
        }
    };

    /**
     * Create main db instance with db properties, all other properties injected by CDI
     *
     * @param dbProperties          database only properties from CDI
     * @param propertiesHolderParam the rest global properties in holder from CDI
     */
    @Inject
    public DatabaseManagerImpl(DbProperties dbProperties, PropertiesHolder propertiesHolderParam, JdbiHandleFactory jdbiHandleFactory) {
        this.baseDbProperties = Objects.requireNonNull(dbProperties, "Db Properties is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolderParam, "Properties holder is NULL");
        this.jdbiHandleFactory = Objects.requireNonNull(jdbiHandleFactory, "jdbiHandleFactory is NULL");
        initDatasource();
        this.available = true;
    }

    public DatabaseManagerImpl() {
    } // never use it directly

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

    private void initDatasource() {
        currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
        jdbi = currentTransactionalDataSource.initWithJdbi(new AplDBUpdater());
        jdbiHandleFactory.setJdbi(jdbi);
    }

    private void waitAvailability() {
        while (!available) {
            ThreadUtils.sleep(100);
        }
    }

    @Override
    @Produces
    public Jdbi getJdbi() {
        return jdbi;
    }

    @Override
    public JdbiHandleFactory getJdbiHandleFactory() {
        return jdbiHandleFactory;
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

    private TransactionalDataSource createAndAddShard(Long shardId) {
        long start = System.currentTimeMillis();
        waitAvailability();
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
            new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        //TODO shard
        shardDb.init(new ShardInitDBUpdater());
        connectedShardDataSourceMap.put(shardDataSourceCreateHelper.getShardId(), shardDb);
        log.debug("new SHARD '{}' is CREATED in {} ms", shardDataSourceCreateHelper.getShardName(), System.currentTimeMillis() - start);
        return shardDb;
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
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
            new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        shardDb.init(dbUpdater);
        connectedShardDataSourceMap.put(shardId, shardDb);
        log.debug("new SHARD datasource'{}' is ADDED in {} ms", shardDataSourceCreateHelper.getShardName(), System.currentTimeMillis() - start);
        return shardDb;
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
        Iterator<TransactionalDataSource> dataSourcesIterator = allFullShards.stream().sorted(
            Comparator.reverseOrder()).map(id -> getOrCreateShardDataSourceById(
            id, new ShardAllScriptsDBUpdater())).iterator();
        return dataSourcesIterator;
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

    @Override
    public PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
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
                    jdbi = null;
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
            ", propertiesHolder=" + propertiesHolder +
            ", currentTransactionalDataSource=" + currentTransactionalDataSource +
            ", connectedShardDataSourceMap=" + connectedShardDataSourceMap.size() +
            '}';
    }

}
