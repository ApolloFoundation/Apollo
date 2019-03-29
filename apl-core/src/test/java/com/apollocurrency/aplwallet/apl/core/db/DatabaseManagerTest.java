/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


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
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null, new ShardAddConstraintsSchemaVersion());
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
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null, new ShardInitTableSchemaVersion());
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
        newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null, new ShardAddConstraintsSchemaVersion());
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

}