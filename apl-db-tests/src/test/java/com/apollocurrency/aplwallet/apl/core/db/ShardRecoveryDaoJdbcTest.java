/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardRecoveryDaoJdbc;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.impl.ShardRecoveryDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class ShardRecoveryDaoJdbcTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, ShardRecoveryDaoJdbcImpl.class, TimeServiceImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
        .build();

    private Connection connection;

    @Inject
    private ShardRecoveryDaoJdbc daoJdbc;

    @BeforeEach
    void setUp() throws SQLException {
        connection = extension.getDatabaseManager().getDataSource().getConnection();
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
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(extension.getDatabaseManager().getDataSource());
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
            MigrateState.COMPLETED, "Object1", "db_id", 100L,
            "block");
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
            MigrateState.COMPLETED, "Object1", "db_id", 100L,
            "block");
        long result = daoJdbc.saveShardRecovery(extension.getDatabaseManager().getDataSource(), recovery);
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
        recovery.setColumnName("db_id");
        recovery.setObjectName("Object");
        recovery.setLastColumnValue(10L);
        recovery.setProcessedObject("block");
        int updated = daoJdbc.updateShardRecovery(connection, recovery);
        assertEquals(1, updated);

        ShardRecovery result = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(result);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_STARTED, result.getState());
        assertEquals("db_id", result.getColumnName());
        assertEquals("Object", result.getObjectName());
    }

    @Test
    void updateShardRecoveryDataSource() {
        assertNotNull(daoJdbc);
        ShardRecovery recovery = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(recovery);
        recovery.setState(MigrateState.DATA_COPY_TO_SHARD_STARTED);
        recovery.setColumnName("id");
        recovery.setObjectName("Object");
        recovery.setLastColumnValue(10L);
        recovery.setProcessedObject("transaction");
        int updated = daoJdbc.updateShardRecovery(extension.getDatabaseManager().getDataSource(), recovery);
        assertEquals(1, updated);

        ShardRecovery result = daoJdbc.getLatestShardRecovery(connection);
        assertNotNull(result);
        assertEquals(MigrateState.DATA_COPY_TO_SHARD_STARTED, result.getState());
        assertEquals("id", result.getColumnName());
        assertEquals("transaction", result.getProcessedObject());
    }

    @Test
    void hardDeleteShardRecovery() throws SQLException {
        ShardRecovery recovery = new ShardRecovery(MigrateState.COMPLETED, "Object1",
            "db_id", 100L, "transaction");
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