/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.shard.ShardConstants;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private Map<Long, TransactionalDataSource> connectedShardDataSourceMap = new ConcurrentHashMap<>(); // secondary shards
    private Jdbi jdbi;
    private JdbiHandleFactory jdbiHandleFactory;
//    @Inject @Setter
//    private ShardNameHelper shardNameHelper;
    /**
     * Create, initialize and return main database source.
     * @return main data source
     */
    @Override
    public synchronized TransactionalDataSource getDataSource() {
        if (currentTransactionalDataSource == null || currentTransactionalDataSource.isShutdown()) {
            initDatasource();
        }
        return currentTransactionalDataSource;
    }

    /**
     * Create main db instance with db properties, all other properties injected by CDI
     * @param dbProperties database only properties from CDI
     * @param propertiesHolderParam the rest global properties in holder from CDI
     */
    @Inject
    public DatabaseManagerImpl(DbProperties dbProperties, PropertiesHolder propertiesHolderParam, JdbiHandleFactory jdbiHandleFactory) {
        baseDbProperties = Objects.requireNonNull(dbProperties, "Db Properties cannot be null");
        propertiesHolder = propertiesHolderParam;
        this.jdbiHandleFactory = jdbiHandleFactory;
        initDatasource();
    }
    public void initDatasource() {
        jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
        jdbiHandleFactory.setJdbi(jdbi);
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
    public synchronized Jdbi getJdbi() {
        if (jdbi == null) {
            // should never happen, but happens sometimes in unit tests because of CDI
            jdbi = currentTransactionalDataSource.initWithJdbi(new AplDbVersion());
        }
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
    public synchronized List<Long> findAllShards(TransactionalDataSource transactionalDataSource) {
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

    private synchronized Set<Long> findAllFullShards() {
        Set<Long> result = new HashSet<>();
        try (Connection con = getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT shard_id from shard where shard_state=? order by shard_height desc")) {
            pstmt.setLong(1, ShardState.FULL.getValue());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource createAndAddShard(Long shardId) {
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
    public synchronized TransactionalDataSource createAndAddShard(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        if (connectedShardDataSourceMap.containsKey(shardId)) {
            TransactionalDataSource dataSource = connectedShardDataSourceMap.get(shardId);
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

    @Override
    public synchronized List<TransactionalDataSource> getFullDatasources() {
        Set<Long> allFullShards = findAllFullShards();
        List<TransactionalDataSource> dataSources = allFullShards.stream().sorted(Comparator.reverseOrder()).map(id-> getOrCreateShardDataSourceById(id, new ShardAddConstraintsSchemaVersion())).collect(Collectors.toList());
        return dataSources;
    }

    @Override
    public synchronized int closeAllShardDataSources() {
        int closedDatasources = 0;
        for (TransactionalDataSource dataSource : connectedShardDataSourceMap.values()) {
            dataSource.shutdown();
            closedDatasources++;
        }
        connectedShardDataSourceMap.clear();
        return closedDatasources;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource createAndAddTemporaryDb(String temporaryDatabaseName) {
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
    public synchronized TransactionalDataSource getShardDataSourceById(long shardId) {
        return connectedShardDataSourceMap.get(shardId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource getOrCreateShardDataSourceById(Long shardId) {
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
    public synchronized TransactionalDataSource getOrCreateShardDataSourceById(Long shardId, DbVersion dbVersion) {
        Objects.requireNonNull(dbVersion, "dbVersion is null");
        if (shardId != null && connectedShardDataSourceMap.containsKey(shardId)) {
            return connectedShardDataSourceMap.get(shardId);
        } else {
            return createAndAddShard(shardId, dbVersion);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized TransactionalDataSource getOrInitFullShardDataSourceById(long shardId) {
        Set<Long> fullShards = findAllFullShards();
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
     * Shutdown main db and secondary shards.
     * After that the db can be reinitialized/opened again
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
