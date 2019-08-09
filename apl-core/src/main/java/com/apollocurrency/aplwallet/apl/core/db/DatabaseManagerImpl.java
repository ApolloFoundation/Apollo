/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.core.utils.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.StringValidator;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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
    private volatile TransactionalDataSource currentTransactionalDataSource; // main/shard database
    private Map<Long, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(); // secondary shards
    private Jdbi jdbi;
    private JdbiHandleFactory jdbiHandleFactory;
    private Set<Long> fullShardIds = new CopyOnWriteArraySet<>(); // store full shard ids
    private boolean available; // required for db hot swap
    private final Object lock = new Object(); // required to sync creation of shard datasources
    /**
     * Create, initialize and return main database source.
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
     * Create main db instance with db properties, all other properties injected by CDI
     * @param dbProperties database only properties from CDI
     * @param propertiesHolderParam the rest global properties in holder from CDI
     */
    @Inject
    public DatabaseManagerImpl(DbProperties dbProperties, PropertiesHolder propertiesHolderParam, JdbiHandleFactory jdbiHandleFactory) {
        this.baseDbProperties = Objects.requireNonNull(dbProperties, "Db Properties cannot be null");
        this.propertiesHolder = propertiesHolderParam;
        this.jdbiHandleFactory = jdbiHandleFactory;
        initDatasource();
        this.available = true;
    }

    private void initDatasource() {
        currentTransactionalDataSource = new TransactionalDataSource(baseDbProperties, propertiesHolder);
        jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
        jdbiHandleFactory.setJdbi(jdbi);
    }

    private void waitAvailability() {
        while (!available) {
            ThreadUtils.sleep(100);
        }
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
//        if (jdbi == null) {
            // should never happen, but happens sometimes in unit tests because of CDI
//            jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
//        }
        return jdbi;
    }

    @Override
    public JdbiHandleFactory getJdbiHandleFactory() {
        return jdbiHandleFactory;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createOrUpdateShard(Long shardId, DbVersion dbVersion) {
        waitAvailability();
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        if (connectedShardDataSourceMap.containsKey(shardId)) {
            TransactionalDataSource dataSource = connectedShardDataSourceMap.get(shardId);
            dataSource.update(dbVersion);
            log.debug("Init existing SHARD using db version'{}' ", dbVersion);
            return dataSource;
        } else {
            return createShardDatasource(shardId, dbVersion);
        }
    }

    TransactionalDataSource createShardDatasource(Long shardId, DbVersion dbVersion) {
        waitAvailability();
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                new ShardDataSourceCreateHelper(this, shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        shardDb.init(dbVersion);
        connectedShardDataSourceMap.put(shardId, shardDb);
        log.debug("new SHARD datasource'{}' is CREATED", shardDataSourceCreateHelper.getShardName());
        return shardDb;
    }

    @Override
    public List<TransactionalDataSource> getFullDataSources() {
        waitAvailability();
        return fullShardIds.stream()
                .sorted(Comparator.reverseOrder())
                .map(id-> getOrCreateShardDataSourceById(id, new ShardAddConstraintsSchemaVersion()))
                .collect(Collectors.toList());
    }

    @Override
    public int closeAllShardDataSources() {
        synchronized (lock) {
            int closedDatasources = 0;
            for (TransactionalDataSource dataSource : connectedShardDataSourceMap.values()) {
                dataSource.shutdown();
                closedDatasources++;
            }
            connectedShardDataSourceMap.clear();
            return closedDatasources;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource createAndAddTemporaryDb(String temporaryDatabaseName) {
        StringValidator.requireNonBlank(temporaryDatabaseName, "temporary database name");
        waitAvailability();
        log.debug("Create new SHARD '{}'", temporaryDatabaseName);
        DbProperties shardDbProperties = baseDbProperties.deepCopy()
                    .dbFileName(temporaryDatabaseName)
                    .dbUrl(null) // nullify dbUrl intentionally!;
                    .dbIdentity(TEMP_DB_IDENTITY);

        TransactionalDataSource temporaryDataSource = new TransactionalDataSource(shardDbProperties, propertiesHolder);
        temporaryDataSource.init(new AplDbVersion());
        connectedShardDataSourceMap.put(TEMP_DB_IDENTITY, temporaryDataSource); // put temporary DS with special ID
        log.debug("new temporaryDataSource '{}' is CREATED", temporaryDatabaseName);
        return temporaryDataSource;
    }

    @Override
    public TransactionalDataSource getShardDataSourceById(long shardId) {
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
    public TransactionalDataSource getOrCreateShardDataSourceById(Long shardId) {
        return getOrCreateShardDataSourceById(shardId, new ShardInitTableSchemaVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        Objects.requireNonNull(shardId, "shard id is null");
        TransactionalDataSource dataSource = getShardDataSourceById(shardId);
        if (dataSource == null) {
            synchronized (lock) {
                dataSource = getShardDataSourceById(shardId);
                if (dataSource == null) {
                    dataSource =  createShardDatasource(shardId, dbVersion);
                }
            }
        }
        return dataSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionalDataSource getOrInitFullShardDataSourceById(long shardId) {
        waitAvailability();
        if (fullShardIds.contains(shardId)) {
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
     * Shutdown main db and secondary shards.
     * After that the db can be reinitialized/opened again
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
