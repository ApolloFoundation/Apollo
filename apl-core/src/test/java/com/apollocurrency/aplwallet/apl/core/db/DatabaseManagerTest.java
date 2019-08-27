/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbPopulator;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class DatabaseManagerTest {

    private String TEMP_FILE_NAME = "apl-temp-utest-db-name";

    private static PropertiesHolder propertiesHolder = new PropertiesHolder();
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    private DbProperties baseDbProperties;
    private DatabaseManager databaseManager;
    @BeforeEach
    public void setUp() throws IOException {
        Path dbFilePath = temporaryFolderExtension.newFolder().toPath().resolve(Constants.APPLICATION_DIR_NAME);
        baseDbProperties = DbTestData.getDbFileProperties(dbFilePath.toAbsolutePath().toString());
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        DbPopulator dbPopulator = new DbPopulator(databaseManager.getDataSource(), "db/schema.sql", "db/db-manager-data.sql");
        dbPopulator.initDb();
        dbPopulator.populateDb();
    }

    @AfterEach
    public void tearDown() {
        databaseManager.shutdown();
    }

    @Test
    void init() {

        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
    }

    @Test
    void createAndAddShard() throws Exception {
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L);
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
    }

    @Test
    void createShardInitTableSchemaVersion() throws Exception {
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L, new ShardInitTableSchemaVersion());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    private void checkTablesCreated(Connection newShardDbConnection) throws SQLException {
        PreparedStatement sqlStatement = newShardDbConnection.prepareStatement("select * from BLOCK");
        sqlStatement.execute();
        sqlStatement = newShardDbConnection.prepareStatement("select * from TRANSACTION");
        sqlStatement.execute();
    }

    @Test
    void createAndAddShardWithoutId() throws Exception {
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null);
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    @Test
    void createShardAddConstraintsSchemaVersion() throws Exception {
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L, new ShardAddConstraintsSchemaVersion());
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
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L, new ShardInitTableSchemaVersion());
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
        newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L, new ShardAddConstraintsSchemaVersion());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
    }

    @Test
    void createTemporaryDb() throws Exception {
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource temporaryDb = ((ShardManagement)databaseManager).createAndAddTemporaryDb(TEMP_FILE_NAME);
        assertNotNull(temporaryDb);
        assertNotNull(temporaryDb.getConnection());
    }

    @Test
    void testFindFullDatasources() {
        Collection<TransactionalDataSource> fullDatasources = ((ShardManagement) databaseManager).getFullDataSources(2L);
        assertEquals(2, fullDatasources.size());
        Iterator<TransactionalDataSource> iterator = fullDatasources.iterator();
        assertTrue(iterator.next().getUrl().contains("shard-3"), "First datasource should represent full shard with id 3 (sorted by shard id desc)");
        assertTrue(iterator.next().getUrl().contains("shard-2"), "Second datasource should represent full shard with id 2 (sorted by shard id desc)");
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
        assertTrue(dataSource.getUrl().contains("shard-2"), "Datasource should represent full shard with id 2");
    }


}