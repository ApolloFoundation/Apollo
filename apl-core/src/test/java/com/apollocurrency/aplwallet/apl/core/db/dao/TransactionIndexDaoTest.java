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
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class TransactionIndexDaoTest {
    static final BlockIndex blockIndex0 = new BlockIndex(10L, 30L, 30);
    static final BlockIndex blockIndex1 = new BlockIndex(1L, 1L, 1);
    static final BlockIndex blockIndex2 = new BlockIndex(2L, 2L, 2);
    static final TransactionIndex transactionIndex0 = new TransactionIndex(100L, 30L);
    static final TransactionIndex transactionIndex1 = new TransactionIndex(101L, 1L);
    static final TransactionIndex transactionIndex2 = new TransactionIndex(102L, 1L);
    static final TransactionIndex transactionIndex3 = new TransactionIndex(103L, 1L);
    static final TransactionIndex transactionIndex4 = new TransactionIndex(200L, 2L);
    static final TransactionIndex transactionIndex5 = new TransactionIndex(201L, 2L);
    public static final long UNKNOWN_TRANSACTION_ID = 200L;
    @Inject
    private DaoConfig daoConfig;
    private static Jdbi jdbi;
    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistry.class,
            JdbiHandleFactory.class, BlockIndexDao.class, TransactionIndexDao.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(dbExtension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class))
            .build();

    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao dao;


    @BeforeEach
    void setUp() {
        jdbi = dbExtension.getDatabaseManger().getJdbi();
        jdbiHandleFactory.setJdbi(jdbi);
        daoConfig.setJdbiHandleFactory(jdbiHandleFactory);
    }

    @Test
    void testGetAll() {
        List<TransactionIndex> result = dao.getAllTransactionIndex();
        assertNotNull(result);
        List<TransactionIndex> expected = Arrays.asList(transactionIndex0, transactionIndex1, transactionIndex2, transactionIndex3);
        Assertions.assertEquals(expected.size(), result.size());
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetCountByBlockId() {
        long count = dao.countTransactionIndexByBlockId(blockIndex0.getBlockId());
        assertEquals(1L, count);
    }

    @Test
    void testGetByTransactionId() {
        TransactionIndex found = dao.getByTransactionId(transactionIndex0.getTransactionId());
        assertNotNull(found);
        Assertions.assertEquals(found, transactionIndex0);
    }

    @Test
    void testDelete() {
        int deleteCount = dao.hardDeleteTransactionIndex(transactionIndex0);
        Assertions.assertEquals(1, deleteCount);
        long actualCount = dao.countTransactionIndexByBlockId(transactionIndex0.getTransactionId());
        Assertions.assertEquals(0, actualCount);
    }

    @Test
    void searchForMissingData() {
        TransactionIndex transactionIndex = dao.getByTransactionId(UNKNOWN_TRANSACTION_ID);
        assertNull(transactionIndex);

        Long shardId = dao.getShardIdByTransactionId(UNKNOWN_TRANSACTION_ID);
        assertNull(shardId);

        List<TransactionIndex> result = dao.getByBlockId(UNKNOWN_TRANSACTION_ID, 10);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetShardIdByTransactionId() {
        Long shardId = dao.getShardIdByTransactionId(transactionIndex1.getTransactionId());
        assertNotNull(shardId);
        assertEquals(blockIndex1.getShardId(), shardId);
    }
    @Test
    void testInsert() {
        dao.saveTransactionIndex(transactionIndex4);
        dao.saveTransactionIndex(transactionIndex5);
        List<TransactionIndex> result = dao.getByBlockId(blockIndex2.getBlockId(), 10);
        assertNotNull(result);
        List<TransactionIndex> expectedByBlockid = Arrays.asList(transactionIndex4, transactionIndex5);
        assertEquals(2, result.size());
        Assertions.assertEquals(expectedByBlockid, result);
        List<TransactionIndex> all = dao.getAllTransactionIndex();
        List<TransactionIndex> expectedAll = Arrays.asList(transactionIndex0, transactionIndex1, transactionIndex2, transactionIndex3, transactionIndex4, transactionIndex5);
        Assertions.assertEquals(6, all.size());
        Assertions.assertEquals(expectedAll, all);
    }

    @Test
    void testUpdateBlockIndex() {
        TransactionIndex copy = transactionIndex3.copy();
        copy.setBlockId(2L);
        int updateCount = dao.updateBlockIndex(copy);
        Assertions.assertEquals(1, updateCount);
        TransactionIndex found = dao.getByTransactionId(transactionIndex3.getTransactionId());
        assertNotNull(found);
        Assertions.assertEquals(copy, found);
        List<TransactionIndex> expected = Arrays.asList(transactionIndex0, transactionIndex1, transactionIndex2, copy);
        List<TransactionIndex> all = dao.getAllTransactionIndex();
        Assertions.assertEquals(expected.size(), all.size());
        Assertions.assertEquals(expected, all);
    }

    @Test
    void testDeleteAll() {
        int deleteCount = dao.hardDeleteAllTransactionIndex();
        Assertions.assertEquals(4, deleteCount);
        List<TransactionIndex> allTransactionIndexes = dao.getAllTransactionIndex();
        Assertions.assertNotNull(allTransactionIndexes);
        Assertions.assertEquals(0, allTransactionIndexes.size());
    }

    @Test
    void testGetHeightForTransactionId() {
        Integer height = dao.getTransactionHeightByTransactionId(transactionIndex0.getTransactionId());
        Assertions.assertEquals(blockIndex0.getBlockHeight(), height);
    }
    @Test
    void testGetHeightForUnknownTransaction() {
        Integer height = dao.getTransactionHeightByTransactionId(UNKNOWN_TRANSACTION_ID);
        Assertions.assertNull(height);
    }
}