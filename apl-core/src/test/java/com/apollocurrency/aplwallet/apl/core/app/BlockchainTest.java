package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.cache.NullCacheProducerForTests;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.ShardDataSourceCreateHelper;
import com.apollocurrency.aplwallet.apl.core.db.ShardInitTableSchemaVersion;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.phasing.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbPopulator;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_11_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_11_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_12_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_13_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_1_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_4_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_6_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_8_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_9_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@EnableWeld
@Execution(ExecutionMode.SAME_THREAD) //for better performance we will not recreate 3 datasources for each test method
class BlockchainTest {


    private static final Path blockchainTestDbPath = createPath("blockchainTestDbPath");
    @RegisterExtension
    static DbExtension extension = new DbExtension(blockchainTestDbPath,"mainDb", "db/shard-main-data.sql");
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    TimeService timeService = mock(TimeService.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(TransactionDaoImpl.class, BlockchainImpl.class, BlockDaoImpl.class,
            TransactionIndexDao.class, DaoConfig.class,
            BlockIndexService.class, NullCacheProducerForTests.class)
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .addBeans(MockBean.of(timeService, TimeService.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    @Inject
    private Blockchain blockchain;
    private TransactionTestData txd;
    private BlockTestData btd;
    static DbPopulator shard1Populator;
    static DbPopulator shard2Populator;


    private static Path createPath(String fileName) {
        try {
            return Files.createTempDirectory(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeAll
    static void init() {
        shard1Populator = initDb("db/shard1-data.sql",1);
        shard2Populator = initDb("db/shard2-data.sql",2);
    }



    @AfterAll
    static void shutdown() throws IOException {
        extension.getDatabaseManager().shutdown();
        FileUtils.deleteDirectory(blockchainTestDbPath.toFile());
    }

    @BeforeEach
    void setUp() {
        txd = new TransactionTestData();
        btd = new BlockTestData();
    }

    private static DbPopulator initDb(String dataScriptPath, long shardId) {
        ShardDataSourceCreateHelper shardDataSourceCreateHelper =
                new ShardDataSourceCreateHelper(extension.getDatabaseManager(), shardId).createUninitializedDataSource();
        TransactionalDataSource shardDb = shardDataSourceCreateHelper.getShardDb();
        shardDb.initWithJdbi(new ShardInitTableSchemaVersion());

        DbPopulator dbPopulator = new DbPopulator(shardDb, "db/schema.sql", dataScriptPath);
        dbPopulator.initDb();
        dbPopulator.populateDb();
        shardDb.shutdown();
        return dbPopulator;
    }

    @Test
    void testGetTxFromNotFinishedDataSource() {
        Transaction transaction = blockchain.getTransaction(100);

        assertNull(transaction);
    }

    @Test
    void findLastBlock() {
        Block block = blockchain.findLastBlock();
        assertNotNull(block);
    }

    @Test
    void findTransaction() {
        Transaction transaction = blockchain.findTransaction(txd.TRANSACTION_1.getId(), Integer.MAX_VALUE);
        assertNotNull(transaction);
        assertEquals(txd.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void getTransaction() {
        Transaction transaction = blockchain.getTransaction(txd.TRANSACTION_1.getId());
        assertNotNull(transaction);
        assertEquals(txd.TRANSACTION_1.getId(), transaction.getId());
    }


    @Test
    void testGetHeight() {
        blockchain.setLastBlock(btd.BLOCK_13);

        assertEquals(btd.BLOCK_13.getHeight(), blockchain.getHeight());
        assertEquals(btd.BLOCK_13, blockchain.getLastBlock());
    }

    @Test
    void testGetHeightWhenLastBlockWasNotSet() {
        blockchain.setLastBlock(null);
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
        blockchain.setLastBlock(null);
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
        blockchain.setLastBlock(btd.BLOCK_13);

        Block lastBlock = blockchain.getLastBlock(btd.BLOCK_12.getTimestamp() - 1);

        assertEquals(btd.BLOCK_11, lastBlock);
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

    @Test
    void testHasBlockByIdWhenLastBlockIdEqualToRequestedBlockId() {
        blockchain.setLastBlock(btd.BLOCK_11);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_11.getId(), btd.BLOCK_11.getHeight());

        assertTrue(hasBlock);
    }

    @Test
    void testHasBlockByIdWhenHeightOfBlockIdGreaterThanRequestedHeight() {
        blockchain.setLastBlock(btd.BLOCK_10);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_9.getId(), btd.BLOCK_8.getHeight());

        assertFalse(hasBlock);
    }

    @Test
    void testHasBlockByIdWhenHeightIsMax() {
        blockchain.setLastBlock(btd.BLOCK_8);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_10.getId(), Integer.MAX_VALUE);

        assertTrue(hasBlock);
    }

    @Test
    void testHasBlockByIdFromFirstShard() {
        blockchain.setLastBlock(btd.BLOCK_10);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_1.getId(), Integer.MAX_VALUE);

        assertFalse(hasBlock); // do not search index or shard datasource
    }

    @Test
    void testHasBlockById() {
        blockchain.setLastBlock(btd.BLOCK_12);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_11.getId());

        assertTrue(hasBlock);
    }

    @Test
    void testHasBlockByIdNotExist() {
        blockchain.setLastBlock(btd.BLOCK_13);

        boolean hasBlock = blockchain.hasBlock(Integer.MAX_VALUE);

        assertFalse(hasBlock);
    }

    @Test
    void testGetBlocks() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Block> blocks = CollectionUtil.toList(blockchain.getBlocks(1, btd.BLOCK_13.getHeight() - btd.BLOCK_11.getHeight()));

        assertEquals(List.of(btd.BLOCK_12, btd.BLOCK_11), blocks);

    }

    @Test
    void testGetAccountBlocks() {
        List<Block> blocks = CollectionUtil.toList(blockchain.getBlocks(btd.BLOCK_12.getGeneratorId(), 0, 0, Integer.MAX_VALUE));

        assertEquals(List.of(btd.BLOCK_13, btd.BLOCK_12), blocks);
    }

    @Test
    void testFindLastBlock() {
        Block lastBlock = blockchain.findLastBlock();

        assertEquals(btd.LAST_BLOCK, lastBlock);
    }

    @Test
    void testLoadBlock() {
        DbUtils.inTransaction(extension, (con)-> {
            try (Statement stmt = con.createStatement()) {
                ResultSet rs = stmt.executeQuery("select * from block where id = " + btd.BLOCK_13.getId());
                rs.next();
                Block block = blockchain.loadBlock(con, rs, true);
                assertEquals(btd.BLOCK_13, block);
                assertEquals(List.of(txd.TRANSACTION_14), btd.BLOCK_13.getTransactions());
            }
            catch (SQLException ignored) {}
        });
    }

    @Test
    void testLoadBlockWithoutTransactions() {
        DbUtils.inTransaction(extension, (con)-> {
            try (Statement stmt = con.createStatement()) {
                ResultSet rs = stmt.executeQuery("select * from block where id = " + btd.BLOCK_10.getId());
                assertTrue(rs.next());
                Block block = blockchain.loadBlock(con, rs, false);
                assertEquals(btd.BLOCK_10, block);
                assertNull(block.getTransactions());
            }
            catch (SQLException ignored) {}
        });
    }

    @Test
    void testSaveBlock() {
        List<Transaction> newTransactions = List.of(txd.NEW_TRANSACTION_0, txd.NEW_TRANSACTION_1);
        newTransactions.forEach(tx-> {
            tx.setBlock(btd.NEW_BLOCK);
        });
        btd.NEW_BLOCK.setTransactions(newTransactions);

        DbUtils.inTransaction(extension, (con)-> {
            blockchain.saveBlock(con, btd.NEW_BLOCK);
            blockchain.commit(btd.NEW_BLOCK);
        });

        Block lastBlock = blockchain.findLastBlock();
        assertEquals(btd.NEW_BLOCK, lastBlock);

        List<Transaction> blockTransactions = blockchain.getBlockTransactions(btd.NEW_BLOCK.getId());
        assertEquals(newTransactions, blockTransactions);
    }

    @Test
    void testGetBlockCount() {
        int blockCount = blockchain.getBlockCount(btd.BLOCK_12.getGeneratorId());

        assertEquals(2, blockCount);
    }

    @Test
    void testGetBlockIdsAfter() {
        List<Long> blockIds = blockchain.getBlockIdsAfter(btd.BLOCK_6.getId(), 6);
        assertEquals(List.of(BLOCK_7_ID,BLOCK_8_ID,BLOCK_9_ID,BLOCK_10_ID, BLOCK_11_ID, BLOCK_12_ID), blockIds);
    }


    @Test
    void testGetAllBlockIdsFromIndex() {
        List<Long> blockIds = blockchain.getBlockIdsAfter(btd.BLOCK_1.getId(), 6);
        assertEquals(List.of(BLOCK_2_ID,BLOCK_3_ID,BLOCK_4_ID,BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID), blockIds);
    }

    @Test
    void testGetBlockIdsAfterForLastShardBlockId() {
        List<Long> blockIds = blockchain.getBlockIdsAfter(btd.BLOCK_9.getId(), 1);

        assertEquals(List.of(BLOCK_10_ID), blockIds);
    }

    @Test
    void testGetBlockIdsFromMainDataSource() {
        List<Long> blockIds = blockchain.getBlockIdsAfter(btd.BLOCK_10.getId(), 100);

        assertEquals(List.of(BLOCK_11_ID, BLOCK_12_ID, BLOCK_13_ID), blockIds);
    }

    @Test
    void testGetBlockSignatures() {
        List<byte[]> signatures = blockchain.getBlockSignaturesFrom(btd.BLOCK_8.getHeight(), btd.BLOCK_12.getHeight());

        List<byte[]> expectedBlockSignatures = List.of(btd.BLOCK_10.getBlockSignature(), btd.BLOCK_11.getBlockSignature());
        for (int i = 0; i < expectedBlockSignatures.size(); i++) {
            assertArrayEquals(expectedBlockSignatures.get(i), signatures.get(i));
        }
    }

    @Test
    void testGetBlocksAfter() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        List<Long> blockIds = List.of(BLOCK_3_ID, BLOCK_4_ID, BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID, BLOCK_8_ID, BLOCK_9_ID, BLOCK_10_ID);
        List<Block> blocks = blockchain.getBlocksAfter(BLOCK_2_ID, blockIds);
        List<Block> expectedBlocks = List.of(btd.BLOCK_3, btd.BLOCK_4, btd.BLOCK_5, btd.BLOCK_6, btd.BLOCK_7, btd.BLOCK_8, btd.BLOCK_9, btd.BLOCK_10);
        compareBlocks(expectedBlocks, blocks);
    }

    private void compareBlocks(List<Block> expectedBlocks, List<Block> blocks) {
        for (int i = 0; i < expectedBlocks.size(); i++) {
            Block expectedBlock = expectedBlocks.get(i);
            Block actualBlock = blocks.get(i);
            assertEquals(expectedBlock, actualBlock);
            List<Transaction> transactions = expectedBlock.getOrLoadTransactions();
            if (transactions != null) {
                assertEquals(transactions, actualBlock.getOrLoadTransactions());
            } else {
                assertNull(actualBlock.getOrLoadTransactions());
            }
        }
    }

    @Test
    void testGetBlocksAfterBlockInAnotherDataSource() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        List<Long> blockIds = List.of(BLOCK_10_ID, BLOCK_11_ID);
        List<Block> blocks = blockchain.getBlocksAfter(BLOCK_9_ID, blockIds);
        List<Block> expectedBlocks = List.of(btd.BLOCK_10, btd.BLOCK_11);
        compareBlocks(expectedBlocks, blocks);
    }

    @Test
    void testGetBlocksAfterFromShardDataSource() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        List<Long> blockIds = List.of(BLOCK_0_ID, BLOCK_1_ID);
        List<Block> blocks = blockchain.getBlocksAfter(GENESIS_BLOCK_ID, blockIds);
        List<Block> expectedBlocks = List.of(btd.BLOCK_0, btd.BLOCK_1);
        compareBlocks(expectedBlocks, blocks);
    }

    @Test
    void testGetBlocksAfterBlockInMainDataSource() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        List<Long> blockIds = List.of(BLOCK_11_ID, BLOCK_12_ID, BLOCK_13_ID);
        List<Block> blocks = blockchain.getBlocksAfter(BLOCK_10_ID, blockIds);
        List<Block> expectedBlocks = List.of(btd.BLOCK_11, btd.BLOCK_12, btd.BLOCK_13);
        compareBlocks(expectedBlocks, blocks);
    }

    @Test
    void testGetBlocksAfterBlockWhichNotExist() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        List<Long> blockIds = List.of(BLOCK_11_ID, BLOCK_12_ID, BLOCK_13_ID);
        List<Block> blocks = blockchain.getBlocksAfter(Long.MIN_VALUE, blockIds);
        compareBlocks(List.of(), blocks);
    }

    @Test
    void testGetBlocksAfterWithEmptyIdList() {

        List<Long> blockIds = List.of();
        List<Block> blocks = blockchain.getBlocksAfter(BLOCK_2_ID, blockIds);
        compareBlocks(List.of(), blocks);
    }

    @Test
    void testGetBlockId() {
        blockchain.setLastBlock(btd.BLOCK_13);

        long blockIdAtHeight = blockchain.getBlockIdAtHeight(btd.BLOCK_11.getHeight());

        assertEquals(btd.BLOCK_11.getId(), blockIdAtHeight);

    }

    @Test
    void testGetBlockIdWhenHeightGreaterThanLastBlockHeight() {
        blockchain.setLastBlock(btd.BLOCK_11);

        assertThrows(IllegalArgumentException.class, () -> blockchain.getBlockIdAtHeight(btd.BLOCK_12.getHeight()));
    }

    @Test
    void testGetBlockIdWhenHeightEqualToLastBlockHeight() {
        blockchain.setLastBlock(btd.BLOCK_12);

        long id = blockchain.getBlockIdAtHeight(btd.BLOCK_12.getHeight());

        assertEquals(btd.BLOCK_12.getId(), id);
    }

    @Test
    void testGetBlockIdFromIndex() {
        blockchain.setLastBlock(btd.BLOCK_9);
        long id = blockchain.getBlockIdAtHeight(btd.BLOCK_8.getHeight());
        assertEquals(btd.BLOCK_8.getId(), id);
    }

    @Test
    void testGetBlockAtHeightWhenHeightIsEqualToLastBlocHeight() {
        blockchain.setLastBlock(btd.BLOCK_10);

        Block blockAtHeight = blockchain.getBlockAtHeight(btd.BLOCK_10.getHeight());

        assertEquals(btd.BLOCK_10, blockAtHeight);
    }

    @Test
    void testGetBlockAtHeightWhenHeightGreaterThanLastBlockHeight() {
        blockchain.setLastBlock(btd.BLOCK_9);

        assertThrows(IllegalArgumentException.class, () -> blockchain.getBlockAtHeight(btd.BLOCK_10.getHeight()));
    }

    @Test
    void testGetBlockAtHeightForMainDataSource() {
        blockchain.setLastBlock(btd.BLOCK_11);

        Block blockAtHeight = blockchain.getBlockAtHeight(btd.BLOCK_10.getHeight());

        assertEquals(btd.BLOCK_10, blockAtHeight);
    }

    @Test
    void testGetBlockAtHeightForShardDataSource() {
        blockchain.setLastBlock(btd.BLOCK_11);

        Block blockAtHeight = blockchain.getBlockAtHeight(btd.BLOCK_6.getHeight());

        assertEquals(btd.BLOCK_6, blockAtHeight);
    }

    @Test
    void testGetBlockAtHeightWhichNotExist() {
        blockchain.setLastBlock(btd.BLOCK_11);

        assertThrows(RuntimeException.class, () -> blockchain.getBlockAtHeight(Integer.MIN_VALUE));
    }

    @Test
    void testGetShardInitialBlock() {
        blockchain.setLastBlock(btd.BLOCK_12);

        Block shardIntialBlock = blockchain.getShardInitialBlock();

        assertEquals(btd.BLOCK_10, shardIntialBlock);
    }

    @Test
    void testGetEcBlockWhenLastBlockByTimestampWasNotFound() {
        blockchain.setLastBlock(btd.BLOCK_13);
        Blockchain spy = spy(blockchain);
        doReturn(null).when(spy).getLastBlock(btd.BLOCK_12.getTimestamp());

        EcBlockData ecBlock = spy.getECBlock(btd.BLOCK_12.getTimestamp());

        assertEquals(btd.BLOCK_10.getId(), ecBlock.getId());
    }

    @Test
    void testGetEcBlockWhenLastBlockByTimestampWasFound() {
        blockchain.setLastBlock(btd.BLOCK_13);
        Block mockBlock = mock(Block.class);
        Blockchain spy = spy(blockchain);
        doReturn(mockBlock).when(spy).getLastBlock(btd.BLOCK_12.getTimestamp() - 1);
        doReturn(btd.BLOCK_11.getHeight() + 720).when(mockBlock).getHeight();

        EcBlockData ecBlock = spy.getECBlock(btd.BLOCK_12.getTimestamp() - 1);

        assertEquals(btd.BLOCK_11.getId(), ecBlock.getId());
    }

    @Test
    void testDeleteBlocksFrom() {
        Block block = blockchain.deleteBlocksFrom(btd.BLOCK_12.getId());

        assertEquals(btd.BLOCK_11, block);

        Block lastBlock = blockchain.findLastBlock();

        assertEquals(btd.BLOCK_11, lastBlock);

        blockchain.setLastBlock(btd.BLOCK_13);
        List<Block> blocks = CollectionUtil.toList(blockchain.getBlocks(0, Integer.MAX_VALUE));
        assertEquals(List.of(btd.BLOCK_11, btd.BLOCK_10), blocks);
    }

    @Test
    void testDeleteBlocksFromHeight() {
        blockchain.deleteBlocksFromHeight(btd.BLOCK_11.getHeight());

        Block lastBlock = blockchain.findLastBlock();

        assertEquals(btd.BLOCK_10, lastBlock);

        blockchain.setLastBlock(btd.BLOCK_13);
        List<Block> blocks = CollectionUtil.toList(blockchain.getBlocks(0, Integer.MAX_VALUE));
        assertEquals(List.of(btd.BLOCK_10), blocks);
    }

    @Test
    void testDeleteAll() {
        blockchain.setLastBlock(btd.BLOCK_12);

        blockchain.deleteAll();

        Block lastBlock = blockchain.findLastBlock();
        assertNull(lastBlock);
    }

    @Test
    void testGetTransaction() {
        Transaction transaction = blockchain.getTransaction(txd.TRANSACTION_3.getId());

        assertEquals(txd.TRANSACTION_3, transaction);
    }

    @Test
    void testFindTransaction() {
        Transaction transaction = blockchain.findTransaction(txd.TRANSACTION_6.getId(), Integer.MAX_VALUE);

        assertEquals(txd.TRANSACTION_6, transaction);
    }

    @Test
    void testGetTransactionByStringFullHash() {
        Transaction tx = blockchain.getTransactionByFullHash(Convert.toHexString(txd.TRANSACTION_4.getFullHash()));

        assertEquals(txd.TRANSACTION_4, tx);
    }

    @Test
    void testFindTransactionByFullHashBytes() {
        Transaction tx = blockchain.findTransactionByFullHash(txd.TRANSACTION_6.getFullHash());

        assertEquals(txd.TRANSACTION_6, tx);
    }

    @Test
    void testFindTransactionByFullHashBytesWithHeight() {
        Transaction tx = blockchain.findTransactionByFullHash(txd.TRANSACTION_8.getFullHash(), btd.BLOCK_13.getHeight());

        assertEquals(txd.TRANSACTION_8, tx);
    }

    @Test
    void testFindTransactinByFullHashWithCollision() {
        Transaction tx = blockchain.findTransactionByFullHash(fullHashWithCollision(txd.TRANSACTION_13.getFullHash()), Integer.MAX_VALUE);

        assertNull(tx);
    }

    @Test
    void testFindTransactinByFullHashWithCollisionFromShardDataSource() {
        Transaction tx = blockchain.findTransactionByFullHash(fullHashWithCollision(txd.TRANSACTION_4.getFullHash()), Integer.MAX_VALUE);

        assertNull(tx);
    }

    @Test
    void testHasTransactionInShards() {
        boolean hasTransaction = blockchain.hasTransaction(txd.TRANSACTION_1.getId());

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionInMainDb() {
        boolean hasTransaction = blockchain.hasTransaction(txd.TRANSACTION_13.getId());

        assertTrue(hasTransaction);
    }

    @Test
    void testHasNotTransactionWithUnknownId() {
        boolean hasTransaction = blockchain.hasTransaction(Long.MAX_VALUE);

        assertFalse(hasTransaction);
    }

    @Test
    void testHasTransactionWithShardHeight() {
        boolean hasTransaction = blockchain.hasTransaction(txd.TRANSACTION_7.getId(), BLOCK_11_HEIGHT);

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionFromMainDataSource() {
        boolean hasTransaction = blockchain.hasTransaction(txd.TRANSACTION_14.getId(), Integer.MAX_VALUE);

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionWhichNotExist() {
        boolean hasTransaction = blockchain.hasTransaction(Long.MAX_VALUE, Integer.MAX_VALUE);

        assertFalse(hasTransaction);
    }

    @Test
    void testHasTransactionByStringFullHash() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(Convert.toHexString(txd.TRANSACTION_4.getFullHash()));

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashBytes() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(txd.TRANSACTION_4.getFullHash());

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactinByFullHashBytesFromMainDataSource() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(txd.TRANSACTION_13.getFullHash());

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashBytesWithHeight() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(txd.TRANSACTION_13.getFullHash(), txd.TRANSACTION_13.getHeight());

        assertTrue(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashWithHeightInIndex() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(txd.TRANSACTION_2.getFullHash(), BLOCK_11_HEIGHT);

        assertTrue(hasTransaction);
    }


    // COMMENTED OUT tests because they still creates Weld container and do not shutdown it!!!
//    @Disabled // doesn't work, but creates additional Weld container which it not shutdown later
    @Test
    void testHasTransactionByFullHashBytesWhenHeightOfTransactionIsGreaterThanRequestedHeight() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(txd.TRANSACTION_7.getFullHash(), 0);

        assertFalse(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashBytesWhenTransactionIsNotExist() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(new byte[32],Integer.MAX_VALUE);

        assertFalse(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashWithCollision() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(fullHashWithCollision(txd.TRANSACTION_12.getFullHash()), Integer.MAX_VALUE);

        assertFalse(hasTransaction);
    }

    @Test
    void testHasTransactionByFullHashWithCollisionInIndex() {
        boolean hasTransaction = blockchain.hasTransactionByFullHash(fullHashWithCollision(txd.TRANSACTION_5.getFullHash()), Integer.MAX_VALUE);

        assertFalse(hasTransaction);
    }

    @Test
    void testGetTransactionHeight() {
        Integer transactionHeight = blockchain.getTransactionHeight(txd.TRANSACTION_4.getFullHash(), Integer.MAX_VALUE);

        assertEquals(txd.TRANSACTION_4.getHeight(), transactionHeight);
    }

    @Test
    void testGetTransactionHeightForTxFromMainDataSource() {
        Integer transactionHeight = blockchain.getTransactionHeight(txd.TRANSACTION_13.getFullHash(), Integer.MAX_VALUE);

        assertEquals(txd.TRANSACTION_13.getHeight(), transactionHeight);
    }

    @Test
    void testGetTransactionHeightWhichIsGreaterThanRequested() {
        Integer transactionHeight = blockchain.getTransactionHeight(txd.TRANSACTION_13.getFullHash(), 0);

        assertNull(transactionHeight);
    }

    @Test
    void testGetTransactionHeightWhichNotExist() {
        Integer transactionHeight = blockchain.getTransactionHeight(new byte[32], Integer.MAX_VALUE);

        assertNull(transactionHeight);
    }

    @Test
    void testGetTransactionHeightByFullHashWithCollision() {
        Integer transactionHeight = blockchain.getTransactionHeight(fullHashWithCollision(txd.TRANSACTION_13.getFullHash()), Integer.MAX_VALUE);

        assertNull(transactionHeight);
    }

    @Test
    void testGetTransactionHeightByFullHashWithCollisionFromIndex() {
        Integer transactionHeight = blockchain.getTransactionHeight(fullHashWithCollision(txd.TRANSACTION_4.getFullHash()), Integer.MAX_VALUE);

        assertNull(transactionHeight);
    }

    @Test
    void testGetFullHash() {
        byte[] fullHash = blockchain.getFullHash(txd.TRANSACTION_5.getId());

        assertArrayEquals(txd.TRANSACTION_5.getFullHash(), fullHash);
    }

    @Test
    void testGetFullHashFromMainDataSource() {
        byte[] fullHash = blockchain.getFullHash(txd.TRANSACTION_12.getId());

        assertArrayEquals(txd.TRANSACTION_12.getFullHash(), fullHash);
    }

    @Test
    void testGetFullHashWhichNotExist() {
        byte[] fullHash = blockchain.getFullHash(Long.MAX_VALUE);

        assertNull(fullHash);
    }

    @Test
    void testLoadTransaction() {
        DbUtils.inTransaction(extension, (con)-> {
            try (Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select * from transaction where id = " + txd.TRANSACTION_14.getId())) {
                    rs.next();
                    Transaction tx = blockchain.loadTransaction(con, rs);
                    assertEquals(txd.TRANSACTION_14, tx);
                }
            }
            catch (SQLException | AplException.NotValidException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testGetTransactionCount() {
        int transactionCount = blockchain.getTransactionCount();

        assertEquals(6, transactionCount);
    }

    @Test
    void testGetTransactionsExcludingExpiredPrunable() {
        blockchain.setLastBlock(btd.BLOCK_13);
        int timeOffset = 10_000;
        doReturn(txd.TRANSACTION_14.getTimestamp() + timeOffset).when(timeService).getEpochTime();
        doReturn(timeOffset + 1).when(blockchainConfig).getMinPrunableLifetime();

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_14.getSenderId(), 0, (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, true);

        assertEquals(List.of(txd.TRANSACTION_14), transactions);
    }

    @Test
    void testGetTransactionsIncludingExpiredPrunable() {
        blockchain.setLastBlock(btd.BLOCK_13);
        doReturn(true).when(propertiesHolder).INCLUDE_EXPIRED_PRUNABLE();
        int timeOffset = 10_000;
        doReturn(txd.TRANSACTION_14.getTimestamp() + timeOffset).when(timeService).getEpochTime();
        doReturn(timeOffset + 1).when(blockchainConfig).getMinPrunableLifetime();

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_14.getSenderId(), 0, (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, true);

        assertEquals(List.of(txd.TRANSACTION_14), transactions);
    }

    @Test
    void testGetTransactionsIncludingExpiredPrunableWhenTransactionHasTimestampLessThanMinPrunableTimestamp() {
        blockchain.setLastBlock(btd.BLOCK_13);
        doReturn(true).when(propertiesHolder).INCLUDE_EXPIRED_PRUNABLE();
        int timeOffset = 10_000;
        doReturn(txd.TRANSACTION_14.getTimestamp() + timeOffset).when(timeService).getEpochTime();
        doReturn(timeOffset).when(blockchainConfig).getMinPrunableLifetime();

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_14.getSenderId(), 0, (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, true);

        assertEquals(List.of(), transactions);

    }

    @Test
    void testGetTransactionsWithConfirmation() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), txd.TRANSACTION_14.getHeight() - txd.TRANSACTION_8.getHeight() + 1,
                (byte) -1, (byte) -1, 0, false, false, false,
                0, Integer.MAX_VALUE, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_6, txd.TRANSACTION_5, txd.TRANSACTION_4, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), txd.TRANSACTION_14.getHeight() - txd.TRANSACTION_8.getHeight() + 1,
                (byte) -1, (byte) -1, 0, false, false, false,
                3, 5, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);
    }

    @Test
    void testGetTransactionsFromDifferentDataSources() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                0, Integer.MAX_VALUE, false, false, true);
        // candidates by type+subtype are TR_12, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_13, txd.TRANSACTION_9, txd.TRANSACTION_7, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);
    }


    @Test
    void testGetTransactionsFromDifferentDataSourcesWhenSkipFirstNEntries() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)-1, (byte)-1, 0, false, false, false,
                8, Integer.MAX_VALUE, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_4, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                1, Integer.MAX_VALUE, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_9, txd.TRANSACTION_7, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                2, Integer.MAX_VALUE, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_7, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                3, Integer.MAX_VALUE, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                4, Integer.MAX_VALUE, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_2), transactions);
    }

    @Test
    void testGetTransactionsFromDifferentDataSourcesSkipSecondShard() {
        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_10.getSenderId(), 0, (byte) 0, (byte) 1, 0, false, false, false,
                0, Integer.MAX_VALUE, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_10, txd.TRANSACTION_1, txd.TRANSACTION_0), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_10.getSenderId(), 0, (byte) 0, (byte) 1, 0, false, false, false,
                1, 1, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_1), transactions);
    }

    @Test
    void testGetTransactionsFromDifferentDataSourcesWithLimit() {
        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                0, 1, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_13, txd.TRANSACTION_9), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                0, 2, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_13, txd.TRANSACTION_9, txd.TRANSACTION_7), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                0, 3, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_13, txd.TRANSACTION_9, txd.TRANSACTION_7, txd.TRANSACTION_3), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                0, 4, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_13, txd.TRANSACTION_9, txd.TRANSACTION_7, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);
    }

    @Test
    void testGetTransactionsFromDifferentDataSourcesWhenSkipFirstNEntriesWithLimit() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)-1, (byte)-1, 0, false, false, false,
                8, 10, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_4, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                3, 4, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                3, 5, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                4, 5, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_2), transactions);

        transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)0, (byte)1, 0, false, false, false,
                2, 4, false, false, true);
        // candidates by type+subtype are TR_13, TR_9, TR_7, TR_3, TR_2
        assertEquals(List.of(txd.TRANSACTION_7, txd.TRANSACTION_3, txd.TRANSACTION_2), transactions);

    }


    @Test
    void testGetTransactionsFromDifferentDataSourcesWhenSkipFirstNEntriesWithLimitWithoutSearchingFirstShardDataSource() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte)-1, (byte)-1, 0, false, false, false,
                4, 8, false, false, true);
        assertEquals(List.of(txd.TRANSACTION_8, txd.TRANSACTION_7, txd.TRANSACTION_6, txd.TRANSACTION_5, txd.TRANSACTION_4), transactions);
    }

    @Test
    void testGetTransactionsFromDifferentDataSourcesWhenLimitIsZero() {
        blockchain.setLastBlock(btd.BLOCK_13);

        List<Transaction> transactions = blockchain.getTransactions(txd.TRANSACTION_2.getSenderId(), 0, (byte) -1, (byte) -1, 0, false, false, false, 0, 0, false, false, true);

        assertEquals(List.of(txd.TRANSACTION_13), transactions);
    }

    @Test
    void testGetBlockTransactions() {
        List<Transaction> blockTransactions = blockchain.getBlockTransactions(btd.BLOCK_7.getId());

        assertEquals(btd.BLOCK_7.getOrLoadTransactions(), blockTransactions);
    }

    @Test
    void testHasBlockWithHeightInMainDataSource() {
        blockchain.setLastBlock(btd.BLOCK_13);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_12.getId(), Integer.MAX_VALUE);

        assertTrue(hasBlock);
    }

    @Test
    void testHasBlockWithHeightInShardDataSource() {
        blockchain.setLastBlock(btd.BLOCK_13);

        boolean hasBlock = blockchain.hasBlock(btd.BLOCK_4.getId(), Integer.MAX_VALUE);

        assertFalse(hasBlock);
    }

    @Test
    void testHasBlockWithHeightForBlockWhichNotExist() {
        blockchain.setLastBlock(btd.BLOCK_13);

        boolean hasBlock = blockchain.hasBlock(Long.MAX_VALUE, Integer.MAX_VALUE);

        assertFalse(hasBlock);
    }

    @Test
    void testGetPrivateTransactionsByType() {
        List<Transaction> transactions = CollectionUtil.toList(blockchain.getTransactions((byte) 0, (byte) 1, 1, 3));
        // transactions exists but cannot be extracted
        assertEquals(List.of(), transactions);
    }
    @Test
    void testGetTransactionsByType() {
        List<Transaction> transactions = CollectionUtil.toList(blockchain.getTransactions((byte) 8, (byte) 0, 1, 3));

        assertEquals(List.of(txd.TRANSACTION_11), transactions);
    }

    @Test
    void testTransactionCountByAccount() {
        int transactionCount = blockchain.getTransactionCount(txd.TRANSACTION_2.getSenderId(), (byte) 8, (byte) 0);

        assertEquals(2, transactionCount);
    }

    @Test
    void testGetTransactionsByPreparedStatementOnConnection() {
        DbUtils.inTransaction(extension, (con)-> {
            try (PreparedStatement pstm = con.prepareStatement("select * from transaction where id = ?")) {
                pstm.setLong(1, txd.TRANSACTION_10.getId());
                List<Transaction> transactions = CollectionUtil.toList(blockchain.getTransactions(con, pstm));
                assertEquals(List.of(txd.TRANSACTION_10), transactions);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testFindPrunableTransactionsStartingFromFirstPrunableTransactionTimestampExclusive() {
        DbUtils.inTransaction(extension, (con)-> {
            List<PrunableTransaction> prunableTransactions = blockchain.findPrunableTransactions(con, txd.TRANSACTION_13.getTimestamp() + 1, Integer.MAX_VALUE);
            assertEquals(1, prunableTransactions.size());
            assertEquals(txd.TRANSACTION_14.getId(), prunableTransactions.get(0).getId());
        });
    }
    @Test
    void testFindPrunableTransactionsBetweenTxTimestampsInclusive() {
        DbUtils.inTransaction(extension, (con)-> {
            List<PrunableTransaction> prunableTransactions = blockchain.findPrunableTransactions(con, txd.TRANSACTION_13.getTimestamp(), txd.TRANSACTION_14.getTimestamp());
            assertEquals(2, prunableTransactions.size());
            assertEquals(txd.TRANSACTION_14.getId(), prunableTransactions.get(1).getId());
            assertEquals(txd.TRANSACTION_13.getId(), prunableTransactions.get(0).getId());
        });
    }

    @Test
    void testHasBlockInShards() {
        blockchain.setLastBlock(btd.BLOCK_10);

        boolean res = blockchain.hasBlockInShards(btd.BLOCK_0.getId());

        assertTrue(res);
    }

    @Test
    void testHasBlockInShardsWhichNotExist() {
        blockchain.setLastBlock(btd.BLOCK_10);

        boolean res = blockchain.hasBlockInShards(Integer.MAX_VALUE);

        assertFalse(res);
    }

    @Test
    void testHasBlockInShardsWhichExistInCurrentDb() {
        blockchain.setLastBlock(btd.BLOCK_13);

        boolean res = blockchain.hasBlockInShards(btd.BLOCK_12.getId());

        assertTrue(res);
    }

    @Test
    void testGetBlockGenerators() {
        blockchain.setLastBlock(null);
        Set<Long> blockGenerators = blockchain.getBlockGenerators(2);

        assertEquals(Set.of( 3883484057046974168L,9211698109297098287L), blockGenerators);

    }

    @Test
    void testSetShardInitialBlock() {
        blockchain.setShardInitialBlock(btd.BLOCK_10);
        Block shardInitialBlock = blockchain.getShardInitialBlock();
        assertEquals(btd.BLOCK_10, shardInitialBlock);
    }

    @Test
    void testGetTransactionCountBetweenMinMaxHeights() {
        Long count = blockchain.getTransactionCount(extension.getDatabaseManager().getDataSource(), 0, Integer.MAX_VALUE);
        assertEquals(6, count);
    }


    @Test
    void testGetTransactionCountBetweenHeights() {
        Long count = blockchain.getTransactionCount(extension.getDatabaseManager().getDataSource(), txd.TRANSACTION_10.getHeight(), txd.TRANSACTION_13.getHeight());
        assertEquals(4, count);
    }

    @Test
    void testGetBlockCountBetweenHeights() {
        Long blockCount = blockchain.getBlockCount(extension.getDatabaseManager().getDataSource(), btd.BLOCK_10.getHeight(), btd.BLOCK_12.getHeight());
        assertEquals(2, blockCount);
    }


    @Test
    void testGetTransactionsBeforeHeight() {
        List<TransactionDbInfo> result = blockchain.getTransactionsBeforeHeight(txd.TRANSACTION_13.getHeight());
        List<TransactionDbInfo> expected = List.of(
                new TransactionDbInfo(txd.DB_ID_9, txd.TRANSACTION_9.getId()),
                new TransactionDbInfo(txd.DB_ID_10, txd.TRANSACTION_10.getId()),
                new TransactionDbInfo(txd.DB_ID_11, txd.TRANSACTION_11.getId()),
                new TransactionDbInfo(txd.DB_ID_12, txd.TRANSACTION_12.getId())
        );
        assertEquals(expected, result);
    }

    @Test
    void testGetTransactionsBeforeHeightOfLastBlockHeight() {
        List<TransactionDbInfo> result = blockchain.getTransactionsBeforeHeight(txd.TRANSACTION_12.getHeight());
        assertEquals(List.of(), result);
    }

    @Test
    void testIsInitializedWhenLastBlockNotSet() {
        blockchain.setLastBlock(null);

        boolean initialized = blockchain.isInitialized();

        assertFalse(initialized);
    }
    @Test
    void testIsInitializedWhenShardInitialBlockNotSet() {
        blockchain.setLastBlock(btd.BLOCK_13);
        blockchain.setShardInitialBlock(null);

        boolean initialized = blockchain.isInitialized();

        assertFalse(initialized);
    }
    @Test
    void testIsInitializedWhenShardInitialBlockAndLastBlockSet() {
        blockchain.setLastBlock(btd.BLOCK_13);
        blockchain.setShardInitialBlock(btd.BLOCK_10);

        boolean initialized = blockchain.isInitialized();

        assertTrue(initialized);
    }

    private byte[] fullHashWithCollision(byte[] fullHash) {
        byte[] fullHashWithCollision = Arrays.copyOfRange(fullHash, 0, 32);
        fullHashWithCollision[31] = (byte) (fullHashWithCollision[31] + 1);
        return fullHashWithCollision;
    }
}