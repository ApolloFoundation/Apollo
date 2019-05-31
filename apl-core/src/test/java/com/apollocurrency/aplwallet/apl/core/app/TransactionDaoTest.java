package com.apollocurrency.aplwallet.apl.core.app;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@EnableWeld
class TransactionDaoTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockDaoTestDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
            .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
            .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(mock(EpochTime.class), EpochTime.class))
            .addBeans(MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class))
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .build();

    private TransactionDao dao;
    private TransactionTestData td;

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
        td = new TransactionTestData();
        dao = new TransactionDaoImpl(extension.getDatabaseManger());
    }


    @Test
    void findByBlockId() {
        List<Transaction> transactions = dao.findBlockTransactions(BLOCK_0_ID, extension.getDatabaseManger().getDataSource());
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
    }

    @Test
    void findTransactionId() {
        Transaction transaction = dao.findTransaction(td.TRANSACTION_0.getId(), extension.getDatabaseManger().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_0.getId(), transaction.getId());
    }

    @Test
    void findTransactionIdHeight() {
        Transaction transaction = dao.findTransaction(td.TRANSACTION_1.getId(), td.TRANSACTION_1.getHeight(), extension.getDatabaseManger().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void findTransactionByFullHash() {
        Transaction transaction = dao.findTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManger().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_5.getId(), transaction.getId());
    }

    @Test
    void testFindTransactionByFullHashNotExist() {
        Transaction tx = dao.findTransactionByFullHash(new byte[32], Integer.MAX_VALUE, extension.getDatabaseManger().getDataSource());

        assertNull(tx, "Transaction with zero hash should not exist");
    }

    @Test
    void testFindTransactionByIdNotExist() {
        Transaction tx = dao.findTransaction(Integer.MIN_VALUE, Integer.MAX_VALUE, extension.getDatabaseManger().getDataSource());

        assertNull(tx, "Transaction with Integer.MIN_VALUE id should not exist");
    }

    @Test
    void hasTransactionBy() {
        boolean isFound = dao.hasTransaction(td.TRANSACTION_5.getId(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManger().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void hasTransactionByFullHash() {
        boolean isFound = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManger().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void getFullHash() {
        byte[] fullHash = dao.getFullHash(td.TRANSACTION_5.getId(), extension.getDatabaseManger().getDataSource());
        assertNotNull(fullHash);
        assertArrayEquals(td.TRANSACTION_5.getFullHash(), fullHash);
    }

    @Test
    void getTransactionCount() {
        int count = dao.getTransactionCount();
        assertEquals(15, count);

        long countLong = dao.getTransactionCount(null, 0, 8000);
        assertEquals(7, countLong);
    }

    @Test
    void getTransactionsFromDbToDb() {
        List<Transaction> result = dao.getTransactions((int) td.DB_ID_0, (int) td.DB_ID_9);
        assertNotNull(result);
        assertEquals(9, result.size());
    }

    @Test
    void getTransactionsFromAccount() {
        int count = dao.getTransactionCount(9211698109297098287L, (byte)0, (byte)0);
        assertEquals(9, count);
    }

    @Test
    void testFindTransactionByFullHashWithDataSource() {
        Transaction tx = dao.findTransactionByFullHash(td.TRANSACTION_6.getFullHash(), extension.getDatabaseManger().getDataSource());

        assertArrayEquals(td.TRANSACTION_6.getFullHash(), tx.getFullHash());
    }

    @Test
    void testHasTransactionByIdWithDataSource() {
        boolean hasTransaction = dao.hasTransaction(td.TRANSACTION_3.getId(), extension.getDatabaseManger().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testHasTransactionByFullHashWithDataSource() {
        boolean hasTransaction = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), extension.getDatabaseManger().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testFindPrunableTransactions() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con)-> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(con, 0, Integer.MAX_VALUE);
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampOuterLimit() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con)-> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(con, td.TRANSACTION_6.getTimestamp(), td.TRANSACTION_14.getTimestamp());
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampInnerLimit() {
        DbUtils.inTransaction(extension, (con)-> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(con, td.TRANSACTION_6.getTimestamp() + 1, td.TRANSACTION_14.getTimestamp() - 1);
            assertEquals(1, prunableTransactions.size());
            assertEquals(td.TRANSACTION_13.getId(), prunableTransactions.get(0).getId());
        });
    }

    @Test
    void testSaveTransactions() {
        DbUtils.inTransaction(extension, (con)-> {
            dao.saveTransactions(con, List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0));
        });
        List<Transaction> blockTransactions = dao.findBlockTransactions(td.NEW_TRANSACTION_0.getBlockId(), extension.getDatabaseManger().getDataSource());
        assertEquals(List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0), blockTransactions);
    }

    @Test
    void testGetTransactionsByAccountId() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 8, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11), transactions);
    }

    @Test
    void testGetTransactionsWithPhasingOnlyAndNonPhasedOnly() {
        assertThrows(IllegalArgumentException.class, () -> CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 8, (byte) -1, 0, false, true, true, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0)));
    }

    @Test
    void testGetPrivateTransactionsWhenIncludePrivateIsFalse() {
        assertThrows(RuntimeException.class, () -> CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 1, 0, false, true, false, 0, Integer.MAX_VALUE, false, false, false, Integer.MAX_VALUE, 0)));
    }

    @Test
    void testGetPhasedTransactions() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, false, true, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.TRANSACTION_13), transactions);
    }

    @Test
    void testGetAllNotPhasedTransactionsWithPagination() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, false, false, true, 1, 3, false, false, true, td.TRANSACTION_7.getHeight() - 1, 0));
        assertEquals(List.of(td.TRANSACTION_5, td.TRANSACTION_4, td.TRANSACTION_3), transactions);
    }

    @Test
    void testGetExecutedOnlyTransactions() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, td.TRANSACTION_3.getBlockTimestamp() + 1, false, false, false, 0, Integer.MAX_VALUE, false, true, false, Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.TRANSACTION_9, td.TRANSACTION_8, td.TRANSACTION_7, td.TRANSACTION_6, td.TRANSACTION_5, td.TRANSACTION_4), transactions);
    }

    @Test
    void testGetTransactionsWithMessage() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.TRANSACTION_13), transactions);
        transactions = CollectionUtil.toList(dao.getTransactions(td.TRANSACTION_14.getSenderId(), 0, (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, false, Integer.MAX_VALUE, 0));
        assertEquals(List.of(td.TRANSACTION_14), transactions);
    }

    @Test
    void testGetTransactionsWithPagination() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions((byte) -1, (byte) -1, 2, 4));
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11, td.TRANSACTION_10), transactions);
    }

    @Test
    void testGetTransactionsByType() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions((byte) 8, (byte) -1, 0, Integer.MAX_VALUE));

        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11), transactions);
    }

    @Test
    void testGetTransactinsByTypeAndSubtypeWithPagination() {
        List<Transaction> transactions = CollectionUtil.toList(dao.getTransactions((byte) 0, (byte) 0, 3, 5));

        assertEquals(List.of(td.TRANSACTION_8, td.TRANSACTION_7, td.TRANSACTION_6), transactions);
    }

}