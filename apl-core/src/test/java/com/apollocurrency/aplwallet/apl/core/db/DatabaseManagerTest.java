/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

@EnableWeld
class DatabaseManagerTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static String TEMP_FILE_NAME = "apl-temp-utest-db-name";
    private static Path pathToDb = FileSystems.getDefault().getPath(System.getProperty("user.dir") + File.separator  + BASE_SUB_DIR);;

    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;

//    @Inject
    private static DatabaseManager databaseManager;


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManagerImpl.class, GlobalSyncImpl.class, DerivedDbTablesRegistry.class)
            .build();

    @BeforeAll
    static void setUpAll() throws IOException {
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                null,
                false,
                null,
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        baseDbProperties = dbConfig.getDbConfig();
    }

    @AfterEach
    void tearDown() {
        databaseManager.shutdown();
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @AfterAll
    static void stopAll() {
        databaseManager.shutdown();
    }

    @Test
    void init() {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
    }

    @Test
    void createAndAddShard() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L);
        assertNotNull(newShardDb);
        assertNotNull(newShardDb.getConnection());
        databaseManager.shutdown(newShardDb);
    }

    @Test
    void createShardInitTableSchemaVersion() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(1L, new ShardInitTableSchemaVersion());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
        databaseManager.shutdown(newShardDb);
    }

    private void checkTablesCreated(Connection newShardDbConnection) throws SQLException {
        PreparedStatement sqlStatement = newShardDbConnection.prepareStatement("select * from BLOCK");
        sqlStatement.execute();
        sqlStatement = newShardDbConnection.prepareStatement("select * from TRANSACTION");
        sqlStatement.execute();
    }

    @Test
    void createAndAddShardWithoutId() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager.getJdbi());
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null);
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
        databaseManager.shutdown(newShardDb);
    }

    @Test
    void createShardAddConstraintsSchemaVersion() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource newShardDb = ((ShardManagement)databaseManager).createAndAddShard(null, new ShardAddConstraintsSchemaVersion());
        assertNotNull(newShardDb);
        Connection newShardDbConnection = newShardDb.getConnection();
        assertNotNull(newShardDbConnection);
        checkTablesCreated(newShardDbConnection);
        databaseManager.shutdown(newShardDb);
    }

    @Test
    void createShardTwoSchemaVersion() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
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
        databaseManager.shutdown(newShardDb);
    }

    @Test
    void createTemporaryDb() throws Exception {
        databaseManager = new DatabaseManagerImpl(baseDbProperties, propertiesHolder);
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        TransactionalDataSource temporaryDb = ((ShardManagement)databaseManager).createAndAddTemporaryDb(TEMP_FILE_NAME);
        assertNotNull(temporaryDb);
        assertNotNull(temporaryDb.getConnection());
        databaseManager.shutdown(temporaryDb);
    }

}