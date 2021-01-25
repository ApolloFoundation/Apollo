/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.DBContainerRootTest;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.db.updater.ShardAllScriptsDBUpdater;
import com.apollocurrency.aplwallet.apl.db.updater.ShardInitDBUpdater;
import com.apollocurrency.aplwallet.apl.testutil.DbPopulator;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Slf4j
@Tag("slow")
class DatabaseManagerTest extends DBContainerRootTest {
    private static PropertiesHolder propertiesHolder = new PropertiesHolder();
    private DbProperties baseDbProperties;
    private DatabaseManagerImpl databaseManager;

    @BeforeEach
    public void setUp() throws IOException {
        baseDbProperties = DbTestData.getDbFileProperties(mariaDBContainer);
        baseDbProperties.setDbParams("&TC_DAEMON=true&TC_REUSABLE=true");
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder, new JdbiHandleFactory());
        DbPopulator dbPopulator = new DbPopulator(null, "db/db-manager-data.sql");
        dbPopulator.initDb(databaseManager.getDataSource());
        dbPopulator.populateDb(databaseManager.getDataSource());
        databaseManager.initFullShards(Set.of(2L, 3L));
    }

    @AfterEach
    public void tearDown() {
        if(databaseManager!=null) {
            databaseManager.shutdown();
        }
    }

    @Test
    void init() {
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
    }


    @Test
    void createShardInitTableSchemaVersion() throws Exception {
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement) databaseManager).createOrUpdateShard(1L, new ShardInitDBUpdater());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    private void checkTablesCreated(Connection newShardDbConnection) throws SQLException {
        PreparedStatement sqlStatement = newShardDbConnection.prepareStatement("select * from block");
        sqlStatement.execute();
        sqlStatement = newShardDbConnection.prepareStatement("select * from transaction");
        sqlStatement.execute();
    }

    @Test
    void createShardAddConstraintsSchemaVersion() throws Exception {
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement) databaseManager).createOrUpdateShard(1L, new ShardAllScriptsDBUpdater());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    @Test
    void createShardTwoSchemaVersion() throws Exception {
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement) databaseManager).createOrUpdateShard(1L, new ShardInitDBUpdater());
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
        newShardDb = ((ShardManagement) databaseManager).createOrUpdateShard(1L, new ShardAllScriptsDBUpdater());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    @Test
    void testFindFullDatasources() {
        Collection<TransactionalDataSource> fullDatasources = ((ShardManagement) databaseManager).getAllFullDataSources(2L);
        assertEquals(2, fullDatasources.size());
        Iterator<TransactionalDataSource> iterator = fullDatasources.iterator();
        assertTrue(iterator.next().getUrl().contains("shard_3"), "First datasource should represent full shard with id 3 (sorted by shard id desc)");
        assertTrue(iterator.next().getUrl().contains("shard_2"), "Second datasource should represent full shard with id 2 (sorted by shard id desc)");
    }

    @Test
    void testGetOrInitFullShardDataSourceForShardWhichNotExist() {
        TransactionalDataSource dataSource = ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(0L);

        assertNull(dataSource, "Shard datasource with shardId=0 should not exist");
    }

    @Test
    void testGetOrInitFullShardDataSourceForNotFullShard() {
        TransactionalDataSource dataSource = ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(1L);

        assertNull(dataSource, "Shard datasource with shardId=1 should not be full, (shard state != 100)");
    }

    @Test
    void testGetOrInitFullShardDataSourceForFullShardId() {
        TransactionalDataSource dataSource = ((ShardManagement) databaseManager).getOrInitFullShardDataSourceById(2L);

        assertNotNull(dataSource, "Shard datasource with shardId=2 should be full, (shard state = 100)");
        assertTrue(dataSource.getUrl().contains("shard_2"), "Datasource should represent full shard with id 2");
    }

    @Test
    void testGetDatasourceWhenDbIsUnavailable() throws ExecutionException, InterruptedException {
        TransactionalDataSource currentDatasource = databaseManager.getDataSource();
        databaseManager.setAvailable(false);
        CompletableFuture<Void> getDatasourceFuture = CompletableFuture.supplyAsync(() -> {
            checkDatasource(databaseManager.getDataSource());
            return null;
        });
        CompletableFuture<Void> setAvailableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
                databaseManager.setAvailable(true);
                return null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        setAvailableFuture.get();
        getDatasourceFuture.get();
        assertSame(currentDatasource, databaseManager.getDataSource());
    }

    @Test
    void testDoubleShutdownAndGetDatasource() {
        TransactionalDataSource currentDatasource = databaseManager.getDataSource();
        List<TransactionalDataSource> fullDatasources = ((ShardManagement) databaseManager).getAllFullDataSources(null);
        databaseManager.shutdown();
        databaseManager.shutdown();
        assertTrue(currentDatasource.isShutdown());
        for (TransactionalDataSource fullDatasource : fullDatasources) {
            assertTrue(fullDatasource.isShutdown());
        }
        TransactionalDataSource newCurrentDatasource = databaseManager.getDataSource();
        assertNotSame(currentDatasource, newCurrentDatasource);
        checkDatasource(newCurrentDatasource);

        List<TransactionalDataSource> newFullDatasources = ((ShardManagement) databaseManager).getAllFullDataSources(null);
        for (int i = 0; i < newFullDatasources.size(); i++) {
            assertNotSame(fullDatasources.get(i), newFullDatasources.get(i));
            checkDatasource(newFullDatasources.get(i));
        }
    }

    @Test
    void testAddNewFullShardId() {
        databaseManager.addFullShard(1L);
        List<TransactionalDataSource> fullDatasources = databaseManager.getAllFullDataSources(3L);
        assertEquals(3, fullDatasources.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(fullDatasources.get(i).getUrl().contains("shard_" + (3 - i)));
            checkDatasource(fullDatasources.get(i));
        }
        assertSame(fullDatasources.get(2), databaseManager.getShardDataSourceById(1L));
    }

    @Test
    void testCreateShardDataSourceById() {
        TransactionalDataSource datasource = databaseManager.getOrCreateShardDataSourceById(2L);
        checkDatasource(datasource);
        assertTrue(datasource.getUrl().contains("shard_2"));
    }

    @Test
    void testGetExistingShardDataSourceById() {
        List<TransactionalDataSource> fullDatasources = databaseManager.getAllFullDataSources(null);
        TransactionalDataSource datasource = databaseManager.getOrCreateShardDataSourceById(2L);
        checkDatasource(datasource);
        assertTrue(datasource.getUrl().contains("shard_2"));
        assertSame(fullDatasources.get(1), datasource);
    }

    @Test
    void testGetExistingShardDataSourceByIdWithVersion() {
        List<TransactionalDataSource> fullDatasources = databaseManager.getAllFullDataSources(null);
        TransactionalDataSource datasource = databaseManager.getOrCreateShardDataSourceById(3L, new ShardInitDBUpdater());
        checkDatasource(datasource);
        assertTrue(datasource.getUrl().contains("shard_3"));
        assertSame(fullDatasources.get(0), datasource);
    }

    @Test
    void testInitAndClearPrevFullShards() {
        databaseManager.initFullShards(Set.of(1L));

        List<TransactionalDataSource> datasources = databaseManager.getAllFullDataSources(1L);
        assertEquals(1, datasources.size());
        TransactionalDataSource dataSource = datasources.get(0);
        checkDatasource(dataSource);
        assertTrue(dataSource.getUrl().contains("shard_1"));
    }

    private void checkDatasource(TransactionalDataSource dataSource) {
        try {
            assertFalse(dataSource.isShutdown());
            ResultSet rs = dataSource.getConnection().createStatement().executeQuery("select 1");
            assertTrue(rs.next());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Test
    void testTryInitSeveralSameDatasourcesFromThreads() throws ExecutionException, InterruptedException {
        DatabaseManagerImpl spyDbManager = spy(databaseManager);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                ThreadUtils.sleep(100);
                spyDbManager.getOrCreateShardDataSourceById(1L);
                return null;
            }));
        }
        for (int i = 0; i < 20; i++) {
            futures.get(i).get();
        }
        verify(spyDbManager).createOrUpdateShard(anyLong(), any(ShardInitDBUpdater.class));

    }

}