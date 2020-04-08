package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_10_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_11_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_2_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_4_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_5_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_6_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_8_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_9_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.apollocurrency.aplwallet.apl.core.alias.service.AliasService;
import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
@EnableWeld
class BlockDaoTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockDaoTestDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
            .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
            .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
            .addBeans(MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(AliasService.class), AliasService.class))
            .build();

    private BlockDao blockDao;
    private TransactionDaoImpl transactionDao;
    private BlockTestData td;
    private TransactionTestData txd;

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
        td = new BlockTestData();
        txd = new TransactionTestData();
        blockDao = new BlockDaoImpl(extension.getDatabaseManager());
        transactionDao = new TransactionDaoImpl(extension.getDatabaseManager());
    }



    @Test
    void findByBlockId() {
        Block block = blockDao.findBlock(BLOCK_0_ID, extension.getDatabaseManager().getDataSource());
        assertEquals(block.getId(), BLOCK_0_ID);
    }

    @Test
    void findLastBlock() {
        Block block = blockDao.findLastBlock();
        assertEquals(block.getId(), td.LAST_BLOCK.getId());
    }

    @Test
    void hasLastBlockFromTo() {
        boolean isBlock = blockDao.hasBlock(td.BLOCK_3.getId(), BLOCK_3_HEIGHT, extension.getDatabaseManager().getDataSource());
        assertTrue(isBlock);
    }

    @Test
    void hasLastBlock() {
        boolean isBlock = blockDao.hasBlock(td.BLOCK_3.getId());
        assertTrue(isBlock);
    }

    @Test
    void findLastBlockTimestamp() {
        Block block = blockDao.findLastBlock(BLOCK_7_TIMESTAMP);
        assertEquals(block.getTimestamp(), BLOCK_7_TIMESTAMP);
    }

    @Test
    void findBlockAtHeight() {
        Block block = blockDao.findBlockAtHeight(BLOCK_7_HEIGHT, extension.getDatabaseManager().getDataSource());
        assertEquals(block.getTimestamp(), BLOCK_7_TIMESTAMP);
    }

    @Test
    void findBlockCountRange() {
        Long count = blockDao.getBlockCount(BLOCK_0_HEIGHT, BLOCK_7_HEIGHT);
        assertEquals(7L , count.longValue());
    }

    @Test
    void getBlocksRange() {
        List<Block> result = CollectionUtil.toList(blockDao.getBlocks(BLOCK_7_HEIGHT, BLOCK_0_HEIGHT));
        assertNotNull(result);
        assertEquals(8, result.size());
    }

    @Test
    void getBlocksRangeAccountId() {
        DbIterator<Block> result = blockDao.getBlocks(4363726829568989435L, GENESIS_BLOCK_TIMESTAMP, GENESIS_BLOCK_HEIGHT, BLOCK_7_HEIGHT);
        assertNotNull(result);
        int count = 0;
        while (result.hasNext()) {
            result.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void getGenerators() {
        Set<Long> count = blockDao.getBlockGenerators(BLOCK_0_HEIGHT, Integer.MAX_VALUE);
        assertNotNull(count);
        assertEquals(2 , count.size());
    }

    @Test
    void countByHeight() {
        long count = blockDao.getBlockCount(GENESIS_BLOCK_HEIGHT, BlockTestData.BLOCK_7_HEIGHT);
        assertEquals(8, count);

        count = blockDao.getBlockCount(extension.getDatabaseManager().getDataSource(), BlockTestData.BLOCK_7_HEIGHT, BlockTestData.BLOCK_11_HEIGHT);
        assertEquals(4, count);
    }

    @Test
    void testDeleteFromBlockId() {
        Block block = blockDao.deleteBlocksFrom(td.BLOCK_6.getId());
        assertEquals(td.BLOCK_5, block);

        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.BLOCK_5, lastBlock);

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.BLOCK_5, td.BLOCK_4, td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK), blocks);

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3, txd.TRANSACTION_4,txd.TRANSACTION_5, txd.TRANSACTION_6), transactions);
    }

    @Test
    void testDeleteAll() {
        blockDao.deleteAll();

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(Integer.MAX_VALUE, 0));
        assertEquals(0, blocks.size());

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(0, transactions.size());
    }

    @Test
    void testDeleteBlocksFromHeight() {
        blockDao.deleteBlocksFromHeight(td.BLOCK_4.getHeight());

        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.BLOCK_3, lastBlock);

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK), blocks);

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3), transactions);
    }

    @Test
    void testDeleteBlocksFromHeightWhenBlockIdNotFound() {
        blockDao.deleteBlocksFromHeight(Integer.MIN_VALUE);

        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.LAST_BLOCK, lastBlock);

        Long blockCount = blockDao.getBlockCount(0, Integer.MAX_VALUE);
        assertEquals(15, blockCount);
    }

    @Test
    void testGetBlocksAfter() {
        List<Long> targetBlockIds = List.of(BLOCK_4_ID, BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID, BLOCK_8_ID, BLOCK_9_ID);
        ArrayList<Block> result = new ArrayList<>();

        List<Block> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_3.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 0);

        assertEquals(List.of(td.BLOCK_4, td.BLOCK_5, td.BLOCK_6, td.BLOCK_7, td.BLOCK_8, td.BLOCK_9), blocksAfter);
    }

    @Test
    void testGetBlocksAfterWithOffset() {
        List<Long> targetBlockIds = List.of(BLOCK_2_ID, BLOCK_3_ID, BLOCK_4_ID, BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID);
        ArrayList<Block> result = new ArrayList<>();

        List<Block> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_5.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 4);

        assertEquals(List.of(td.BLOCK_6, td.BLOCK_7), blocksAfter);
    }

    @Test
    void testGetBlocksAfterWithId() {
        List<Long> targetBlockIds = List.of(BLOCK_8_ID, BLOCK_9_ID, BLOCK_11_ID);
        ArrayList<Block> result = new ArrayList<>();

        List<Block> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_7.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 0);

        assertEquals(List.of(td.BLOCK_8, td.BLOCK_9), blocksAfter);
    }

    @Test
    void testGetBlockIdsAfter() {
        List<Long> ids = blockDao.getBlockIdsAfter(td.BLOCK_8.getHeight(), 3);

        assertEquals(List.of(BLOCK_9_ID, BLOCK_10_ID, BLOCK_11_ID), ids);
    }

    @Test
    void testGetBlockCountForGenerator() {
        int blockCount = blockDao.getBlockCount(td.BLOCK_1.getGeneratorId());

        assertEquals(3, blockCount);
    }

    @Test
    void testGetBlocksForAccount() {
        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(td.BLOCK_1.getGeneratorId(), 0, 0, 1));

        assertEquals(List.of(td.BLOCK_12, td.BLOCK_1), blocks);
    }

    @Test
    void testGetBlocksForAccountWithTimestamp() {
        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(td.BLOCK_1.getGeneratorId(), td.BLOCK_0.getTimestamp() + 1, 0, 3));

        assertEquals(List.of(td.BLOCK_12, td.BLOCK_1), blocks);
    }

    @Test
    void testGetBlockSignatures() {
        List<byte[]> signatures = blockDao.getBlockSignaturesFrom(td.BLOCK_3.getHeight(), td.BLOCK_7.getHeight());

        List<byte[]> expectedSignatures = List.of(td.BLOCK_3.getBlockSignature(), td.BLOCK_4.getBlockSignature(), td.BLOCK_5.getBlockSignature(), td.BLOCK_6.getBlockSignature());
        for (int i = 0; i < expectedSignatures.size(); i++) {
            assertArrayEquals(expectedSignatures.get(i), signatures.get(i));
        }
    }

    @Test
    void testFindBlockIdAtHeight() {
        long blockId = blockDao.findBlockIdAtHeight(td.BLOCK_8.getHeight(), extension.getDatabaseManager().getDataSource());

        assertEquals(td.BLOCK_8.getId(), blockId);
    }

    @Test
    void testFindBlockIdAtHeightNotFound() {
        assertThrows(RuntimeException.class, () -> blockDao.findBlockIdAtHeight(Integer.MIN_VALUE, extension.getDatabaseManager().getDataSource()));
    }

    @Test
    void testFindBlockWithVersion() {
        Block block = blockDao.findBlockWithVersion(0, 3);

        assertEquals(block, td.BLOCK_13);
    }

    @Test
    void testFindBlockWithVersionWhenBlocksSkipped() {
        Block block = blockDao.findBlockWithVersion(2, 6);

        assertEquals(block, td.BLOCK_8);
    }

    @Test
    void testSaveBlock() {
        DbUtils.inTransaction(extension, (con)-> {
            blockDao.saveBlock(con, td.NEW_BLOCK);
            blockDao.commit(td.NEW_BLOCK);
        });
        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.NEW_BLOCK, lastBlock);
        Block block = blockDao.findBlock(td.LAST_BLOCK.getId(), extension.getDatabaseManager().getDataSource());
        assertEquals(td.NEW_BLOCK.getId(), block.getNextBlockId());
    }

    @Test
    void testCommitBlock() {
        DbUtils.inTransaction(extension, (con)-> blockDao.commit(td.BLOCK_5));
        Block block = blockDao.findBlock(td.BLOCK_5.getId(), extension.getDatabaseManager().getDataSource());
        assertEquals(0, block.getNextBlockId());
    }

}