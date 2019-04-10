/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class ShardRecoveryDaoJdbcTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, EpochTime.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    private Connection connection;

    @Inject
    private ShardRecoveryDaoJdbc daoJdbc;

    @BeforeEach
    void setUp() throws SQLException {
        connection = extension.getDatabaseManger().getDataSource().getConnection();
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
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(extension.getDatabaseManger().getDataSource());
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
        long result = daoJdbc.saveShardRecovery(extension.getDatabaseManger().getDataSource(), recovery);
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
        int updated = daoJdbc.updateShardRecovery(extension.getDatabaseManger().getDataSource(), recovery);
        assertEquals(1, updated);

        ShardRecovery result = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(result);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_STARTED, result.getState());
        assertEquals("ID", result.getColumnName());
        assertEquals("TRANSACTION", result.getProcessedObject());
    }

    @Test
    void hardDeleteShardRecovery() throws SQLException {
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