package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.TransactionEntity;
import com.apollocurrency.aplwallet.apl.core.model.TransactionDbInfo;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.PrunableTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@QuarkusTest
class TransactionDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();
    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
/*    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .addBeans(MockBean.of(mock(Blockchain.class), Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(mock(PropertiesHolder.class), PropertiesHolder.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(TimeService.class), TimeService.class))
        .build();*/

    private TransactionModelToEntityConverter toEntityConverter;
    private TransactionEntityToModelConverter toModelConverter;

    private TransactionDao dao;
    private TransactionTestData td;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();

        dao = new TransactionDaoImpl(
            new TxReceiptRowMapper(td.getTransactionTypeFactory()),
            new TransactionEntityRowMapper(),
            new PrunableTxRowMapper(td.getTransactionTypeFactory()),
            extension.getDatabaseManager());

        toEntityConverter = new TransactionModelToEntityConverter();
        toModelConverter = new TransactionEntityToModelConverter(td.getTransactionTypeFactory(),
            new TransactionBuilder(td.getTransactionTypeFactory()));
    }


    @Test
    void findByBlockId() {
        List<TransactionEntity> transactions = dao.findBlockTransactions(BLOCK_0_ID, extension.getDatabaseManager().getDataSource());
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
    }

    @Test
    void findTransactionId() {
        TransactionEntity transaction = dao.findTransaction(td.TRANSACTION_0.getId(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_0.getId(), transaction.getId());
    }

    @Test
    void findTransactionIdHeight() {
        TransactionEntity transaction = dao.findTransaction(td.TRANSACTION_1.getId(), td.TRANSACTION_1.getHeight(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void findTransactionByFullHash() {
        TransactionEntity transaction = dao.findTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertNotNull(transaction);
        assertEquals(td.TRANSACTION_5.getId(), transaction.getId());
    }

    @Test
    void testFindTransactionByFullHashNotExist() {
        TransactionEntity tx = dao.findTransactionByFullHash(new byte[32], Integer.MAX_VALUE, extension.getDatabaseManager().getDataSource());

        assertNull(tx, "Transaction with zero hash should not exist");
    }

    @Test
    void testFindTransactionByIdNotExist() {
        TransactionEntity tx = dao.findTransaction(Integer.MIN_VALUE, Integer.MAX_VALUE, extension.getDatabaseManager().getDataSource());

        assertNull(tx, "Transaction with Integer.MIN_VALUE id should not exist");
    }

    @Test
    void hasTransactionBy() {
        boolean isFound = dao.hasTransaction(td.TRANSACTION_5.getId(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void hasTransactionByFullHash() {
        boolean isFound = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), td.TRANSACTION_5.getHeight(), extension.getDatabaseManager().getDataSource());
        assertTrue(isFound);
    }

    @Test
    void getFullHash() {
        byte[] fullHash = dao.getFullHash(td.TRANSACTION_5.getId(), extension.getDatabaseManager().getDataSource());
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
        List<TransactionEntity> result = dao.getTransactions((int) td.DB_ID_0, (int) td.DB_ID_9);
        assertNotNull(result);
        assertEquals(9, result.size());
    }

    @Test
    void getTransactionsFromAccount() {
        int count = dao.getTransactionCount(9211698109297098287L, (byte) 0, (byte) 0);
        assertEquals(9, count);
    }

    @Test
    void testFindTransactionByFullHashWithDataSource() {
        TransactionEntity tx = dao.findTransactionByFullHash(td.TRANSACTION_6.getFullHash(), extension.getDatabaseManager().getDataSource());

        assertArrayEquals(td.TRANSACTION_6.getFullHash(), tx.getFullHash());
    }

    @Test
    void testHasTransactionByIdWithDataSource() {
        boolean hasTransaction = dao.hasTransaction(td.TRANSACTION_3.getId(), extension.getDatabaseManager().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testHasTransactionByFullHashWithDataSource() {
        boolean hasTransaction = dao.hasTransactionByFullHash(td.TRANSACTION_5.getFullHash(), extension.getDatabaseManager().getDataSource());

        assertTrue(hasTransaction, "Transaction should exist");
    }

    @Test
    void testFindPrunableTransactions() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(0, Integer.MAX_VALUE);
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampOuterLimit() {
        List<Long> expectedIds = List.of(td.TRANSACTION_6.getId(), td.TRANSACTION_13.getId(), td.TRANSACTION_14.getId());

        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(td.TRANSACTION_6.getTimestamp(), td.TRANSACTION_14.getTimestamp());
            assertEquals(expectedIds.size(), prunableTransactions.size());
            for (int i = 0; i < prunableTransactions.size(); i++) {
                assertEquals(expectedIds.get(i), prunableTransactions.get(i).getId());
            }
        });
    }

    @Test
    void testFindPrunableTransactionsWithTimestampInnerLimit() {
        DbUtils.inTransaction(extension, (con) -> {
            List<PrunableTransaction> prunableTransactions = dao.findPrunableTransactions(td.TRANSACTION_6.getTimestamp() + 1, td.TRANSACTION_14.getTimestamp() - 1);
            assertEquals(1, prunableTransactions.size());
            assertEquals(td.TRANSACTION_13.getId(), prunableTransactions.get(0).getId());
        });
    }

    @Test
    void testSaveTransactions() {
        DbUtils.inTransaction(extension, (con) -> dao.saveTransactions(toEntityConverter.convert(List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0))));
        List<TransactionEntity> blockTransactions = dao.findBlockTransactions(td.NEW_TRANSACTION_0.getBlockId(), extension.getDatabaseManager().getDataSource());
        assertEquals(List.of(td.NEW_TRANSACTION_1, td.NEW_TRANSACTION_0), toModelConverter.convert(blockTransactions));
    }

    @Test
    void testGetTransactionsByAccountId() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 8, (byte) -1, 0, false, false, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0);
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsWithPhasingOnlyAndNonPhasedOnly() {
        assertThrows(IllegalArgumentException.class, () -> dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 8, (byte) -1, 0, false, true, true, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0));
    }

    @Test
    void testGetPrivateTransactionsWhenIncludePrivateIsFalse() {
        assertThrows(RuntimeException.class, () -> dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 1, 0, false, true, false, 0, Integer.MAX_VALUE, false, false, false, Integer.MAX_VALUE, 0));
    }

    @Test
    void testGetPhasedTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, false, true, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0);
        assertEquals(List.of(td.TRANSACTION_13), toModelConverter.convert(transactions));
    }

    @Test
    void testGetAllNotPhasedTransactionsWithPagination() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, false, false, true, 1, 3, false, false, true, td.TRANSACTION_7.getHeight() - 1, 0);
        assertEquals(List.of(td.TRANSACTION_5, td.TRANSACTION_4, td.TRANSACTION_3), toModelConverter.convert(transactions));
    }

    @Test
    void testGetExecutedOnlyTransactions() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, td.TRANSACTION_3.getBlockTimestamp() + 1, false, false, false, 0, Integer.MAX_VALUE, false, true, false, Integer.MAX_VALUE, 0);
        assertEquals(List.of(td.TRANSACTION_9, td.TRANSACTION_8, td.TRANSACTION_7, td.TRANSACTION_6, td.TRANSACTION_5, td.TRANSACTION_4), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsWithMessage() {
        List<TransactionEntity> transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, true, Integer.MAX_VALUE, 0);
        assertEquals(List.of(td.TRANSACTION_13), toModelConverter.convert(transactions));
        transactions = dao.getTransactions(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_14.getSenderId(), 0, (byte) -1, (byte) -1, 0, true, false, false, 0, Integer.MAX_VALUE, false, false, false, Integer.MAX_VALUE, 0);
        assertEquals(List.of(td.TRANSACTION_14), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsWithPagination() {
        extension.cleanAndPopulateDb();
        List<TransactionEntity> transactions = dao.getTransactions((byte) -1, (byte) -1, 2, 4);
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11, td.TRANSACTION_10), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsByType() {
        List<TransactionEntity> transactions = dao.getTransactions((byte) 8, (byte) -1, 0, Integer.MAX_VALUE);
        assertEquals(List.of(td.TRANSACTION_12, td.TRANSACTION_11), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionsByTypeAndSubtypeWithPagination() {
        List<TransactionEntity> transactions = dao.getTransactions((byte) 0, (byte) 0, 3, 5);
        assertEquals(List.of(td.TRANSACTION_8, td.TRANSACTION_7, td.TRANSACTION_6), toModelConverter.convert(transactions));
    }

    @Test
    void testGetTransactionCountFoAccountInDataSource() {
        int count = dao.getTransactionCountByFilter(extension.getDatabaseManager().getDataSource(), td.TRANSACTION_1.getSenderId(), 0, (byte) 0, (byte) 0, td.TRANSACTION_3.getBlockTimestamp() + 1, false, false, false, false, true, false, Integer.MAX_VALUE, 0);
        assertEquals(6, count);
    }

    @Test
    void testGetTransactionsBeforeHeight() {
        List<TransactionDbInfo> result = dao.getTransactionsBeforeHeight(td.TRANSACTION_6.getHeight());
        List<TransactionDbInfo> expected = List.of(new TransactionDbInfo(td.DB_ID_0, td.TRANSACTION_0.getId()), new TransactionDbInfo(td.DB_ID_1, td.TRANSACTION_1.getId()), new TransactionDbInfo(td.DB_ID_2, td.TRANSACTION_2.getId()), new TransactionDbInfo(td.DB_ID_3, td.TRANSACTION_3.getId()));
        assertEquals(expected, result);
    }

    @Test
    void testGetTransactionsBeforeZeroHeight() {
        List<TransactionDbInfo> result = dao.getTransactionsBeforeHeight(0);
        assertEquals(List.of(), result);
    }

    @Test
    void testGetTransactionsByPreparedStatementOnConnection() {
        DbUtils.checkAndRunInTransaction(extension, (con) -> {
            try (PreparedStatement pstm = con.prepareStatement("select * from transaction where id = ?")) {
                pstm.setLong(1, td.TRANSACTION_10.getId());
                List<TransactionEntity> transactions = dao.getTransactions(con, pstm);
                assertEquals(List.of(td.TRANSACTION_10).stream().map(Transaction::getId).collect(Collectors.toList()),
                    transactions.stream().map(TransactionEntity::getId).collect(Collectors.toList()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

}