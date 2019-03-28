/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@EnableWeld
class ShardRecoveryDaoJdbcTest {

    private static String BASE_SUB_DIR = "unit-test-db";
    private static Path pathToDb = FileSystems.getDefault().getPath(System.getProperty("user.dir") + File.separator  + BASE_SUB_DIR);;
    @RegisterExtension
    DbExtension extension = new DbExtension(baseDbProperties, propertiesHolder);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, EpochTime.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(baseDbProperties, DbProperties.class))
            .build();

    private static PropertiesHolder propertiesHolder;
    private static DbProperties baseDbProperties;
    private DatabaseManager databaseManager;
    private Connection connection;

    @Inject
    private ShardRecoveryDaoJdbc daoJdbc;

    @BeforeAll
    static void setUpAll() {
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

    @BeforeEach
    void setup() throws SQLException {
        databaseManager = extension.getDatabaseManger();
        assertNotNull(databaseManager);
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        assertNotNull(dataSource);
        connection = dataSource.getConnection();
        assertNotNull(connection);
    }

    @AfterEach
    void tearDown() {
        extension.getDatabaseManger().shutdown();
        FileUtils.deleteQuietly(pathToDb.toFile());
    }

    @Test
    void getShardRecoveryByUnknownId() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getShardRecoveryById(connection, 200L);
        assertNull(recovery);
    }

    @Test
    void getShardRecoveryId() throws SQLException {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getShardRecoveryById(connection, 1L);
        assertNotNull(recovery);
        assertNotNull(recovery.getShardRecoveryId());
        assertEquals(1L, recovery.getShardRecoveryId().longValue());
        assertEquals(MigrateState.INIT, recovery.getState());
    }

    @Test
    void getLatestShardRecovery() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(recovery);
    }

    @Test
    void getLatestShardRecoveryDataSource() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(databaseManager.getDataSource());
        assertNotNull(recovery);
    }

    @Test
    void getAllShardRecovery() {
        assertNotNull(daoJdbc);
        List<ShardRecovery> result = daoJdbc.getAllShardRecovery(connection);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void countShardRecovery() {
        assertNotNull(daoJdbc);
        long result = daoJdbc.countShardRecovery(connection);
        assertEquals(1, result);
    }

    @Test
    void saveShardRecovery() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = new ShardRecovery(
                MigrateState.COMPLETED, "Object1", "DB_ID", 100L,
                "BLOCK");
        long result = daoJdbc.saveShardRecovery(connection, recovery);
        assertTrue(result > 1);

        long deleteResult = daoJdbc.hardDeleteShardRecovery(connection, result);
        assertEquals(1, deleteResult);
    }

    @Test
    void saveShardRecoveryStateOnly() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = new ShardRecovery(MigrateState.INIT);
        long result = daoJdbc.saveShardRecovery(connection, recovery);
        assertTrue(result > 1);

        long deleteResult = daoJdbc.hardDeleteShardRecovery(connection, result);
        assertEquals(1, deleteResult);
    }

    @Test
    void saveShardRecoveryDataSource() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = new ShardRecovery(
                MigrateState.COMPLETED, "Object1", "DB_ID", 100L,
                "BLOCK");
        long result = daoJdbc.saveShardRecovery(databaseManager.getDataSource(), recovery);
        assertTrue(result > 1);

        long deleteResult = daoJdbc.hardDeleteShardRecovery(connection, result);
        assertEquals(1, deleteResult);
    }

    @Test
    void updateShardRecovery() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(recovery);
        recovery.setState(MigrateState.DATA_COPY_TO_SHARD_STARTED);
        recovery.setColumnName("DB_ID");
        recovery.setObjectName("Object");
        recovery.setLastColumnValue(10L);
        recovery.setProcessedObject("BLOCK");
        int updated = daoJdbc.updateShardRecovery(connection, recovery);
        assertEquals(1, updated);

        ShardRecovery result = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(result);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_STARTED, result.getState());
        assertEquals("DB_ID", result.getColumnName());
        assertEquals("Object", result.getObjectName());
    }

    @Test
    void updateShardRecoveryDataSource() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(recovery);
        recovery.setState(MigrateState.DATA_COPY_TO_SHARD_STARTED);
        recovery.setColumnName("ID");
        recovery.setObjectName("Object");
        recovery.setLastColumnValue(10L);
        recovery.setProcessedObject("TRANSACTION");
        int updated = daoJdbc.updateShardRecovery(databaseManager.getDataSource(), recovery);
        assertEquals(1, updated);

        ShardRecovery result = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(result);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_STARTED, result.getState());
        assertEquals("ID", result.getColumnName());
        assertEquals("TRANSACTION", result.getProcessedObject());
    }

    @Test
    void hardDeleteShardRecovery() {
        ShardRecovery recovery = new ShardRecovery(MigrateState.COMPLETED, "Object1",
                "DB_ID", 100L, "TRANSACTION");
        long saveResult = daoJdbc.saveShardRecovery(connection, recovery);
        assertTrue(saveResult > 1);

        List<ShardRecovery> allResult = daoJdbc.getAllShardRecovery(connection);
        assertNotNull(allResult);
        assertEquals(2, allResult.size());

        long deleteResult = daoJdbc.hardDeleteShardRecovery(connection, saveResult);
        assertEquals(1, deleteResult);
        long resultCount = daoJdbc.countShardRecovery(connection);
        assertEquals(1, resultCount);
    }

}