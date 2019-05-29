package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DataSourceWrapper;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.ShardManagement;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbPopulator;
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
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;

@EnableWeld
class BlockchainTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockchainTestDb").toAbsolutePath().toString()), "db/shard-main-data.sql", null);

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, TransactionDaoImpl.class, BlockchainImpl.class,
            JdbiHandleFactory.class, BlockDaoImpl.class, TransactionIndexDao.class, DaoConfig.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    @Inject
    private Blockchain blockchain;
    private TransactionTestData testData;
    private BlockTestData btd;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    void setUp() {
        testData = new TransactionTestData();
        btd = new BlockTestData();
        TransactionalDataSource shardDatasource1 = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(1L);
        TransactionalDataSource shardDatasource2 = ((ShardManagement) extension.getDatabaseManger()).getOrCreateShardDataSourceById(2L);
        initDbAndPopulate(shardDatasource1, "db/shard1-data.sql");
        initDbAndPopulate(shardDatasource2, "db/shard2-data.sql");
    }

    private void initDbAndPopulate(DataSourceWrapper dataSourceWrapper, String dataScriptPath) {
        DbPopulator dbPopulator = new DbPopulator(dataSourceWrapper, "db/schema.sql", dataScriptPath);
        dbPopulator.initDb();
        dbPopulator.populateDb();
    }



    @Test
    void findLastBlock() {
        Block block = blockchain.findLastBlock();
        assertNotNull(block);
    }

    @Test
    void findTransaction() {
        Transaction transaction = blockchain.findTransaction(testData.TRANSACTION_1.getId(), testData.TRANSACTION_1.getHeight());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void getTransaction() {
        Transaction transaction = blockchain.getTransaction(testData.TRANSACTION_1.getId());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void getHasTransaction() {
        Transaction transaction = blockchain.getTransaction(testData.TRANSACTION_1.getId());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_1.getId(), transaction.getId());

        boolean hasTransaction = blockchain.hasTransaction(testData.TRANSACTION_1.getId());
        assertTrue(hasTransaction);
    }

    @Test
    void testGetHeight() {
        blockchain.setLastBlock(btd.BLOCK_13);

        assertEquals(btd.BLOCK_13.getHeight(), blockchain.getHeight());
        assertEquals(btd.BLOCK_13, blockchain.getLastBlock());
    }

    @Test
    void testGetHeightWhenLastBlockWasNotSet() {
        assertEquals(0, blockchain.getHeight());
        assertNull(blockchain.getLastBlock());
    }

    @Test
    void testGetLastBlockTimestamp() {
        blockchain.setLastBlock(btd.BLOCK_10);

        assertEquals(btd.BLOCK_10.getTimestamp(), blockchain.getLastBlockTimestamp());
    }

    @Test
    void testGetLastBlockTimestampWhenLastBlockWasNotSet() {
        assertEquals(0, blockchain.getLastBlockTimestamp());
    }

    @Test
    void testGetLastBlockByTimestampWhichGreateOrEqualToLastBlock() {
        blockchain.setLastBlock(btd.BLOCK_8);
        Block lastBlock = blockchain.getLastBlock(btd.BLOCK_8.getTimestamp());

        assertEquals(btd.BLOCK_8, lastBlock);

        lastBlock = blockchain.getLastBlock(Integer.MAX_VALUE);

        assertEquals(btd.BLOCK_8, lastBlock);
    }

    @Test
    void testGetLastBlockByTimestampWhichLessThanLastBlockTimestamp() {
        blockchain.setLastBlock(btd.BLOCK_6);

        Block lastBlock = blockchain.getLastBlock(btd.BLOCK_5.getTimestamp() - 1);

        assertEquals(btd.BLOCK_4, lastBlock);
    }

    @Test
    void testGetBlockByBlockIdWhenLastBlockHasSameId() {
        blockchain.setLastBlock(btd.BLOCK_5);

        Block block = blockchain.getBlock(btd.BLOCK_5.getId());

        assertEquals(btd.BLOCK_5, block);
    }

    @Test
    void testGetBlockByBlockIdFromSecondShardDataSource() {
        blockchain.setLastBlock(btd.BLOCK_10);

        Block block = blockchain.getBlock(btd.BLOCK_5.getId());

        assertEquals(btd.BLOCK_5, block);
    }

    @Test
    void testGetBlockByBlockIdFirstShardDataSource() {
        blockchain.setLastBlock(btd.BLOCK_13);

        Block block = blockchain.getBlock(btd.BLOCK_1.getId());

        assertEquals(btd.BLOCK_1, block);
    }

}