package com.apollocurrency.aplwallet.apl.core.db.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbExtension;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import javax.inject.Inject;

@EnableWeld
class BlockIndexDaoTest {
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();

    @Inject
    private DaoConfig daoConfig;
    private static Jdbi jdbi;
    @Inject
    private JdbiHandleFactory jdbiHandleFactory;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            NtpTime.class,
            BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class, GlobalSync.class,
            GlobalSyncImpl.class,
            PropertiesHolder.class,
            JdbiHandleFactory.class, BlockIndexDao.class, DerivedDbTablesRegistry.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class)
            .addBeans(MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManger().getJdbi(), Jdbi.class))
            .build();

    @Inject
    private BlockIndexDao dao;

    @BeforeEach
    void setUp() {
        jdbi = dbExtension.getDatabaseManger().getJdbi();
        jdbiHandleFactory.setJdbi(jdbi);
        daoConfig.setJdbiHandleFactory(jdbiHandleFactory);
    }

    @Test
    void insertGetAllDelete() {
        BlockIndex blockIndex = new BlockIndex(1L, 2L, 2);
        dao.saveBlockIndex(blockIndex);

        List<BlockIndex> result = dao.getAllBlockIndex();
        assertNotNull(result);
        assertEquals(1, result.size());

        BlockIndex blockIndex2 = new BlockIndex(2L, 300L, 3);
        dao.saveBlockIndex(blockIndex2);

        long shardId = dao.getShardIdByBlockId(300L);
        assertEquals(2L, shardId);

        shardId = dao.getShardIdByBlockHeight(2);
        assertEquals(1L, shardId);

        dao.hardDeleteAllBlockIndex();
    }

    @Test
    void insertCountDelete() {
        BlockIndex blockIndex = new BlockIndex(1L, 1L, 1);
        dao.saveBlockIndex(blockIndex);

        long count = dao.countBlockIndexByShard(1L);
        assertEquals(1, count);

        dao.hardDeleteAllBlockIndex();
    }

    @Test
    void searchForMissingData() {
        BlockIndex blockIndex = dao.getByBlockId(100L);
        assertNull(blockIndex);

        Long shardId = dao.getShardIdByBlockId(100L);
        assertNull(shardId);

        shardId = dao.getShardIdByBlockHeight(100);
        assertNull(shardId);
    }

    @Test
    void insertUpdateDelete() {
        BlockIndex blockIndex = new BlockIndex(1L, 1L, 1);
        dao.saveBlockIndex(blockIndex);

        BlockIndex blockIndex2 = new BlockIndex(1L, 2L, 2);
        dao.saveBlockIndex(blockIndex2);
        BlockIndex blockIndex3 = new BlockIndex(1L, 3L, 3);
        dao.saveBlockIndex(blockIndex3);

        BlockIndex blockIndexFound = dao.getByBlockId(2L);
        assertNotNull(blockIndexFound);
        assertNotNull(blockIndexFound.getShardId());
        assertEquals(1L, blockIndexFound.getShardId().longValue());

        blockIndexFound = dao.getByBlockHeight(3);
        assertNotNull(blockIndexFound);
        assertNotNull(blockIndexFound.getBlockHeight());
        assertEquals(3, blockIndexFound.getBlockHeight().intValue());

        blockIndexFound.setBlockHeight(4);
        dao.updateBlockIndex(blockIndexFound);
        blockIndexFound = dao.getByBlockHeight(4);
        assertNotNull(blockIndexFound);
        assertNotNull(blockIndexFound.getBlockHeight());
        assertEquals(4, blockIndexFound.getBlockHeight().intValue());

        List<BlockIndex> result = dao.getByShardId(1L, 10);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getShardId().longValue());
        assertEquals(1L, result.get(0).getBlockId().longValue());
        assertEquals(1, result.get(0).getBlockHeight().intValue());

        dao.hardBlockIndex(blockIndexFound);
        long count = dao.countBlockIndexByShard(1L);
        assertEquals(2, count);

        dao.hardDeleteAllBlockIndex();
    }
}