/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

@Slf4j

@Tag("slow")
class BlockDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);

    private BlockDao blockDao;
    private TransactionDaoImpl transactionDao;
    private BlockTestData td;
    private TransactionTestData txd;

    @BeforeEach
    void setUp() {
        assertTrue(mariaDBContainer.isRunning());
        td = new BlockTestData();
        txd = new TransactionTestData();
        blockDao = new BlockDaoImpl(extension.getDatabaseManager());
        transactionDao = new TransactionDaoImpl(extension.getDatabaseManager(), txd.getTransactionTypeFactory(), new TransactionRowMapper(txd.getTransactionTypeFactory(), new TransactionBuilder(txd.getTransactionTypeFactory())));
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
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
        Long count = blockDao.getBlockCount(null, BLOCK_0_HEIGHT, BLOCK_7_HEIGHT);
        assertEquals(7L, count.longValue());
    }

    @Test
    void getBlocksRange() {
        List<Block> result = CollectionUtil.toList(blockDao.getBlocks(null, BLOCK_7_HEIGHT, BLOCK_0_HEIGHT, 0));
        assertNotNull(result);
        assertEquals(8, result.size());
    }

    @Test
    void getBlocksRangeAccountId() {
        DbIterator<Block> result = blockDao.getBlocksByAccount(null, 4363726829568989435L, GENESIS_BLOCK_HEIGHT, BLOCK_7_HEIGHT, GENESIS_BLOCK_TIMESTAMP);
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
        assertEquals(2, count.size());
    }

    @Test
    void countByHeight() {
        long count = blockDao.getBlockCount(null, GENESIS_BLOCK_HEIGHT, BlockTestData.BLOCK_7_HEIGHT);
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

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0));
        assertEquals(List.of(td.BLOCK_5, td.BLOCK_4, td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK), blocks);

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3, txd.TRANSACTION_4, txd.TRANSACTION_5, txd.TRANSACTION_6), transactions);
    }

    @Test
    void testDeleteAll() {
        blockDao.deleteAll();

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0));
        assertEquals(0, blocks.size());

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(0, transactions.size());
    }

    @Test
    void testDeleteBlocksFromHeight() {
        blockDao.deleteBlocksFromHeight(td.BLOCK_4.getHeight());

        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.BLOCK_3, lastBlock);

        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0));
        assertEquals(List.of(td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK), blocks);

        List<Transaction> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3), transactions);
    }

    @Test
    void testDeleteBlocksFromHeightWhenBlockIdNotFound() {
        blockDao.deleteBlocksFromHeight(Integer.MIN_VALUE);

        Block lastBlock = blockDao.findLastBlock();
        assertEquals(td.LAST_BLOCK, lastBlock);

        Long blockCount = blockDao.getBlockCount(null, 0, Integer.MAX_VALUE);
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
        int blockCount = blockDao.getBlockCount(null, td.BLOCK_1.getGeneratorId());

        assertEquals(3, blockCount);
    }

    @Test
    void testGetBlocksForAccount() {
        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocksByAccount(null, td.BLOCK_1.getGeneratorId(), 0, 1, 0));

        assertEquals(List.of(td.BLOCK_12, td.BLOCK_1), blocks);
    }

    @Test
    void testGetBlocksForAccountWithTimestamp() {
        List<Block> blocks = CollectionUtil.toList(blockDao.getBlocksByAccount(null, td.BLOCK_1.getGeneratorId(), 0, 3, td.BLOCK_0.getTimestamp() + 1));

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
        DbUtils.inTransaction(extension, (con) -> {
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
        DbUtils.inTransaction(extension, (con) -> blockDao.commit(td.BLOCK_5));
        Block block = blockDao.findBlock(td.BLOCK_5.getId(), extension.getDatabaseManager().getDataSource());
        assertEquals(0, block.getNextBlockId());
    }

}