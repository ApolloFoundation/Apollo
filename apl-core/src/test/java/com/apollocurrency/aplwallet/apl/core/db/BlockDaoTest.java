/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db;

import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.BlockEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
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
import static com.apollocurrency.aplwallet.apl.testutil.DbUtils.inTransaction;
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
    private TransactionEntityToModelConverter toModelConverter;
    private BlockEntityToModelConverter blockEntityToModelConverter;
    private BlockModelToEntityConverter blockModelToEntityConverter;

    @BeforeEach
    void setUp() {
        assertTrue(mariaDBContainer.isRunning());
        td = new BlockTestData();
        txd = new TransactionTestData();
        blockDao = new BlockDaoImpl(extension.getDatabaseManager(), new BlockEntityRowMapper());
        transactionDao = new TransactionDaoImpl(
            new TxReceiptRowMapper(txd.getTransactionTypeFactory()),
            new TransactionEntityRowMapper(),
            new PrunableTxRowMapper(txd.getTransactionTypeFactory()),
            extension.getDatabaseManager());
        toModelConverter = new TransactionEntityToModelConverter(txd.getTransactionTypeFactory(), new TransactionBuilder(txd.getTransactionTypeFactory()));
        blockEntityToModelConverter = new BlockEntityToModelConverter();
        blockModelToEntityConverter = new BlockModelToEntityConverter();
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }


    @Test
    void findByBlockId() {
        BlockEntity block = blockDao.findBlock(BLOCK_0_ID, extension.getDatabaseManager().getDataSource());
        assertEquals(BLOCK_0_ID, block.getId());
    }

    @Test
    void findLastBlock() {
        BlockEntity block = blockDao.findLastBlock();
        assertEquals(td.LAST_BLOCK.getId(), block.getId());
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
        BlockEntity block = blockDao.findLastBlock(BLOCK_7_TIMESTAMP);
        assertEquals(BLOCK_7_TIMESTAMP, block.getTimestamp());
    }

    @Test
    void findBlockAtHeight() {
        BlockEntity block = blockDao.findBlockAtHeight(BLOCK_7_HEIGHT, extension.getDatabaseManager().getDataSource());
        assertEquals(BLOCK_7_TIMESTAMP, block.getTimestamp());
    }

    @Test
    void findBlockCountRange() {
        long count = blockDao.getBlockCount(null, BLOCK_0_HEIGHT, BLOCK_7_HEIGHT);
        assertEquals(7L, count);
    }

    @Test
    void getBlocksRange() {
        List<BlockEntity> result = blockDao.getBlocks(null, BLOCK_7_HEIGHT, BLOCK_0_HEIGHT, 0);
        assertNotNull(result);
        assertEquals(8, result.size());
    }

    @Test
    void getBlocksRangeAccountId() {
        List<BlockEntity> result = blockDao.getBlocksByAccount(null, 4363726829568989435L, GENESIS_BLOCK_HEIGHT, BLOCK_7_HEIGHT, GENESIS_BLOCK_TIMESTAMP);
        assertNotNull(result);
        assertEquals(2, result.size());
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
        BlockEntity block = blockDao.deleteBlocksFrom(td.BLOCK_6.getId());
        assertEquals(td.BLOCK_5, blockEntityToModelConverter.convert(block));

        BlockEntity lastBlock = blockDao.findLastBlock();
        assertEquals(td.BLOCK_5, blockEntityToModelConverter.convert(lastBlock));

        List<BlockEntity> blocks = blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0);
        assertEquals(List.of(td.BLOCK_5, td.BLOCK_4, td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK)
            , blockEntityToModelConverter.convert(blocks));

        List<TransactionEntity> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);

        assertEquals(
            List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3, txd.TRANSACTION_4, txd.TRANSACTION_5, txd.TRANSACTION_6),
            toModelConverter.convert(transactions)
        );
    }

    @Test
    void testDeleteAll() {
        blockDao.deleteAll();

        List<BlockEntity> blocks = blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0);
        assertEquals(0, blocks.size());

        List<TransactionEntity> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(0, transactions.size());
    }

    @Test
    void testDeleteBlocksFromHeight() {
        blockDao.deleteBlocksFromHeight(td.BLOCK_4.getHeight());

        BlockEntity lastBlock = blockDao.findLastBlock();
        assertEquals(td.BLOCK_3, blockEntityToModelConverter.convert(lastBlock));

        List<BlockEntity> blocks = blockDao.getBlocks(null, Integer.MAX_VALUE, 0, 0);
        assertEquals(List.of(td.BLOCK_3, td.BLOCK_2, td.BLOCK_1, td.BLOCK_0, td.GENESIS_BLOCK), blockEntityToModelConverter.convert(blocks));

        List<TransactionEntity> transactions = transactionDao.getTransactions(0, Integer.MAX_VALUE);
        assertEquals(
            List.of(txd.TRANSACTION_0, txd.TRANSACTION_1, txd.TRANSACTION_2, txd.TRANSACTION_3),
            toModelConverter.convert(transactions)
        );
    }

    @Test
    void testDeleteBlocksFromHeightWhenBlockIdNotFound() {
        blockDao.deleteBlocksFromHeight(Integer.MIN_VALUE);

        BlockEntity lastBlock = blockDao.findLastBlock();
        assertEquals(td.LAST_BLOCK, blockEntityToModelConverter.convert(lastBlock));

        Long blockCount = blockDao.getBlockCount(null, 0, Integer.MAX_VALUE);
        assertEquals(15, blockCount);
    }

    @Test
    void testGetBlocksAfter() {
        List<Long> targetBlockIds = List.of(BLOCK_4_ID, BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID, BLOCK_8_ID, BLOCK_9_ID);
        ArrayList<BlockEntity> result = new ArrayList<>();

        List<BlockEntity> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_3.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 0);

        assertEquals(List.of(td.BLOCK_4, td.BLOCK_5, td.BLOCK_6, td.BLOCK_7, td.BLOCK_8, td.BLOCK_9), blockEntityToModelConverter.convert(blocksAfter));
    }

    @Test
    void testGetBlocksAfterWithOffset() {
        List<Long> targetBlockIds = List.of(BLOCK_2_ID, BLOCK_3_ID, BLOCK_4_ID, BLOCK_5_ID, BLOCK_6_ID, BLOCK_7_ID);
        ArrayList<BlockEntity> result = new ArrayList<>();

        List<BlockEntity> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_5.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 4);

        assertEquals(List.of(td.BLOCK_6, td.BLOCK_7), blockEntityToModelConverter.convert(blocksAfter));
    }

    @Test
    void testGetBlocksAfterWithId() {
        List<Long> targetBlockIds = List.of(BLOCK_8_ID, BLOCK_9_ID, BLOCK_11_ID);
        ArrayList<BlockEntity> result = new ArrayList<>();

        List<BlockEntity> blocksAfter = blockDao.getBlocksAfter(td.BLOCK_7.getHeight(), targetBlockIds, result, extension.getDatabaseManager().getDataSource(), 0);

        assertEquals(List.of(td.BLOCK_8, td.BLOCK_9), blockEntityToModelConverter.convert(blocksAfter));
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
        List<BlockEntity> blocks = blockDao.getBlocksByAccount(null, td.BLOCK_1.getGeneratorId(), 0, 1, 0);

        assertEquals(List.of(td.BLOCK_12, td.BLOCK_1), blockEntityToModelConverter.convert(blocks));
    }

    @Test
    void testGetBlocksForAccountWithTimestamp() {
        List<BlockEntity> blocks = blockDao.getBlocksByAccount(null, td.BLOCK_1.getGeneratorId(), 0, 3, td.BLOCK_0.getTimestamp() + 1);

        assertEquals(List.of(td.BLOCK_12, td.BLOCK_1), blockEntityToModelConverter.convert(blocks));
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
        BlockEntity block = blockDao.findBlockWithVersion(0, 3);

        assertEquals(td.BLOCK_13, blockEntityToModelConverter.convert(block));
    }

    @Test
    void testFindBlockWithVersionWhenBlocksSkipped() {
        BlockEntity block = blockDao.findBlockWithVersion(2, 6);

        assertEquals(td.BLOCK_8, blockEntityToModelConverter.convert(block));
    }

    @Test
    void testSaveBlock() {
        inTransaction(extension, (con) -> {
            blockDao.saveBlock(blockModelToEntityConverter.convert(td.NEW_BLOCK));
            blockDao.commit(td.NEW_BLOCK.getId());
        });
        BlockEntity lastBlock = blockDao.findLastBlock();
        assertEquals(td.NEW_BLOCK, blockEntityToModelConverter.convert(lastBlock));
        BlockEntity block = blockDao.findBlock(td.LAST_BLOCK.getId(), extension.getDatabaseManager().getDataSource());
        assertEquals(td.NEW_BLOCK.getId(), block.getNextBlockId());
    }

    @Test
    void testCommitBlock() {
        inTransaction(extension, (con) -> blockDao.commit(td.BLOCK_5.getId()));
        BlockEntity block = blockDao.findBlock(td.BLOCK_5.getId(), extension.getDatabaseManager().getDataSource());
        assertEquals(0, block.getNextBlockId());
    }

}