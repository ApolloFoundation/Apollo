package com.apollocurrency.aplwallet.apl.core.db.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.DbConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableWeld
class TransactionIndexDaoTest {

    private static PropertiesHolder propertiesHolder;
    @Inject
    private DaoConfig daoConfig;
    private static DatabaseManager databaseManager;
    private static Jdbi jdbi;
    @Inject
    private JdbiHandleFactory jdbiHandleFactory;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(DbProperties.class, NtpTime.class,
            PropertiesConfigLoader.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DbConfig.class, DaoConfig.class,
            JdbiHandleFactory.class, BlockIndexDao.class, TransactionIndexDao.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            TransactionalDataSource.class, DatabaseManager.class)
            .build();

    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao dao;

    @BeforeAll
    static void setup() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                false,
                "./unit-test-db",
                Constants.APPLICATION_DIR_NAME + ".properties",
                Collections.emptyList());
        propertiesHolder = new PropertiesHolder();
        propertiesHolder.init(propertiesLoader.load());
        DbConfig dbConfig = new DbConfig(propertiesHolder);
        databaseManager = new DatabaseManager(dbConfig.getDbConfig(), propertiesHolder);
    }

    @AfterAll
    static void cleanup() {
        databaseManager.shutdown();
    }

    @BeforeEach
    void setUp() {
        jdbi = databaseManager.getJdbi();
        jdbiHandleFactory.setJdbi(jdbi);
        daoConfig.setJdbiHandleFactory(jdbiHandleFactory);
    }

    @Test
    void insertGetAllDelete() {
        BlockIndex blockIndex = new BlockIndex(10L, 30L, 30);
        blockIndexDao.saveBlockIndex(blockIndex);

        TransactionIndex transactionIndex = new TransactionIndex(100L, 30L);
        dao.saveTransactionIndex(transactionIndex);
        List<TransactionIndex> result = dao.getAllTransactionIndex();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getTransactionId());
        assertEquals(100L, result.get(0).getTransactionId().longValue());
        assertNotNull(result.get(0).getBlockId());
        assertEquals(30L, result.get(0).getBlockId().longValue());

        long count = dao.countTransactionIndexByBlockId(30L);
        assertEquals(1L, count);

        TransactionIndex found = dao.getByTransactionId(100L);
        assertNotNull(found);
        assertNotNull(found.getTransactionId());
        assertEquals(100L, found.getTransactionId().longValue());
        assertNotNull(found.getBlockId());
        assertEquals(30L, found.getBlockId().longValue());

        dao.hardDeleteTransactionIndex(transactionIndex);
        count = dao.countTransactionIndexByBlockId(30L);
        assertEquals(0L, count);

        dao.hardDeleteAllTransactionIndex();
        blockIndexDao.hardDeleteAllBlockIndex();
    }

    @Test
    void searchForMissingData() {
        TransactionIndex transactionIndex = dao.getByTransactionId(200L);
        assertNull(transactionIndex);

        Long shardId = dao.getShardIdByTransactionId(200L);
        assertNull(shardId);

        List<TransactionIndex> result = dao.getByBlockId(200L, 10);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void insertUpdateDelete() {
        BlockIndex blockIndex = new BlockIndex(1L, 1L, 1);
        blockIndexDao.saveBlockIndex(blockIndex);

        BlockIndex blockIndex2 = new BlockIndex(2L, 2L, 2);
        blockIndexDao.saveBlockIndex(blockIndex2);

        TransactionIndex transactionIndex = new TransactionIndex(100L, 1L);
        dao.saveTransactionIndex(transactionIndex);
        TransactionIndex transactionIndex2 = new TransactionIndex(101L, 1L);
        dao.saveTransactionIndex(transactionIndex2);
        TransactionIndex transactionIndex3 = new TransactionIndex(102L, 1L);
        dao.saveTransactionIndex(transactionIndex3);

        Long shardId = dao.getShardIdByTransactionId(102L);
        assertNotNull(shardId);
        assertEquals(1L, shardId.longValue());


        TransactionIndex transactionIndex4 = new TransactionIndex(200L, 2L);
        dao.saveTransactionIndex(transactionIndex4);
        TransactionIndex transactionIndex5 = new TransactionIndex(201L, 2L);
        dao.saveTransactionIndex(transactionIndex5);

        List<TransactionIndex> result = dao.getByBlockId(2L, 10);
        assertNotNull(result);
        assertEquals(2, result.size());

        shardId = dao.getShardIdByTransactionId(200L);
        assertNotNull(shardId);
        assertEquals(2L, shardId.longValue());

        transactionIndex3.setBlockId(2L);
        dao.updateBlockIndex(transactionIndex3);
        TransactionIndex found2 = dao.getByTransactionId(200L);
        assertNotNull(found2);
        assertNotNull(found2.getTransactionId());
        assertNotNull(found2.getBlockId());
        assertEquals(2L, found2.getBlockId().longValue());

        result = dao.getAllTransactionIndex();
        assertNotNull(result);
        assertEquals(5L, result.size());

        dao.hardDeleteAllTransactionIndex();
        blockIndexDao.hardDeleteAllBlockIndex();
    }
}