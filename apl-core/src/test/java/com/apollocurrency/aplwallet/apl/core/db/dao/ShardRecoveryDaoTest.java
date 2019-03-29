/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardRecovery;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import javax.inject.Inject;

@EnableWeld
class ShardRecoveryDaoTest {

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @RegisterExtension
    static DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ShardRecoveryDao.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .build();

    @Inject
    private ShardRecoveryDao dao;

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @Test
    void testGetAllForEmpty() {
        List<ShardRecovery> allShardRecoveries = dao.getAllShardRecovery();
        assertEquals(1, allShardRecoveries.size());
    }

    @Test
    void testUnknownShardById() {
        ShardRecovery recovery = dao.getShardRecoveryById(-100L);
        assertNull(recovery);
    }

    @Test
    void testShortInsert() {
        ShardRecovery recovery = new ShardRecovery(MigrateState.INIT);
        long insertedId = dao.saveShardRecovery(recovery);
        assertTrue(insertedId >= 1L);

        ShardRecovery recovery2 = new ShardRecovery(MigrateState.INIT, "TEST");
        insertedId = dao.saveShardRecovery(recovery2);
        assertTrue(insertedId >= 1L);

        dao.hardDeleteAllShardRecovery();
    }

    @Test
    void testInsert() {
        ShardRecovery recovery = new ShardRecovery(
                MigrateState.INIT, "BLOCK", "DB_ID", 1L, "DB_ID");

        long insertedId = dao.saveShardRecovery(recovery);
        assertTrue(insertedId >= 1L);

        long count = dao.countShardRecovery();
        assertEquals(2, count);

        ShardRecovery found = dao.getLatestShardRecovery();
        assertNotNull(found);
        assertNotNull(found.getShardRecoveryId());
        assertEquals(MigrateState.INIT, found.getState());
        assertNotNull(found.getUpdated());

        List<ShardRecovery> actual = dao.getAllShardRecovery();
        assertEquals(2, actual.size());
        assertEquals(recovery.getState(), actual.get(0).getState());

        int deleted = dao.hardDeleteShardRecovery(actual.get(0).getShardRecoveryId());
        assertEquals(1, deleted);
    }

    @Test
    void testUpdate() {
        ShardRecovery recovery = new ShardRecovery(
                MigrateState.INIT, "BLOCK", "DB_ID", 1L, "DB_ID");
        long insertedId = dao.saveShardRecovery(recovery);
        assertTrue(insertedId > 1L);

        recovery.setShardRecoveryId(insertedId);
        recovery.setState(MigrateState.SHARD_SCHEMA_FULL);
        int updateCount = dao.updateShardRecovery(recovery);
        assertEquals(1, updateCount);

        List<ShardRecovery> actual = dao.getAllShardRecovery();
        assertEquals(2, actual.size());
        assertEquals(recovery.getState(), actual.get(1).getState());
        assertNotNull(actual.get(1).getUpdated());

        ShardRecovery found = dao.getShardRecoveryById(actual.get(1).getShardRecoveryId());
        assertEquals(MigrateState.SHARD_SCHEMA_FULL, found.getState());

        int deleted = dao.hardDeleteShardRecovery(found.getShardRecoveryId());
        assertEquals(1, deleted);
    }

    @Test
    void testDeleteMissing() {
        int deleteCount = dao.hardDeleteShardRecovery(-1L);
        assertEquals(0, deleteCount);
    }

    @Test
    void testCount() {
        long count = dao.countShardRecovery();
        assertEquals(1, count);
    }

}