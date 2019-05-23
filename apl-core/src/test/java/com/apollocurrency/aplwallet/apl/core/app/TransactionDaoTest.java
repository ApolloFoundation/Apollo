package com.apollocurrency.aplwallet.apl.core.app;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class TransactionDaoTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockDaoTestDb").toAbsolutePath().toString()));
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
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();

    @Inject
    private  JdbiHandleFactory jdbiHandleFactory;
    @Inject
    private TransactionDao dao;
    private TransactionTestData testData;
    private BlockTestData blockTestData;

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
        blockTestData = new BlockTestData();
    }

    @AfterEach
    void shutdown() {
        jdbiHandleFactory.close();
    }

    @Test
    void findByBlockId() {
        List<Transaction> transactions = dao.findBlockTransactions(BLOCK_0_ID);
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
    }

    @Test
    void findTransactionId() {
        Transaction transaction = dao.findTransaction(testData.TRANSACTION_0.getId());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_0.getId(), transaction.getId());
    }

    @Test
    void findTransactionIdHeight() {
        Transaction transaction = dao.findTransaction(testData.TRANSACTION_1.getId(), testData.TRANSACTION_1.getHeight());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_1.getId(), transaction.getId());
    }

    @Test
    void findTransactionByFullHash() {
        Transaction transaction = dao.findTransactionByFullHash(testData.TRANSACTION_5.getFullHash(), testData.TRANSACTION_5.getHeight());
        assertNotNull(transaction);
        assertEquals(testData.TRANSACTION_5.getId(), transaction.getId());
    }

    @Test
    void hasTransactionBy() {
        boolean isFound = dao.hasTransaction(testData.TRANSACTION_5.getId(), testData.TRANSACTION_5.getHeight());
        assertTrue(isFound);
    }

    @Test
    void hasTransactionByFullHash() {
        boolean isFound = dao.hasTransactionByFullHash(testData.TRANSACTION_5.getFullHash(), testData.TRANSACTION_5.getHeight());
        assertTrue(isFound);
    }

    @Test
    void getFullHash() {
        byte[] fullHash = dao.getFullHash(testData.TRANSACTION_5.getId());
        assertNotNull(fullHash);
        assertArrayEquals(testData.TRANSACTION_5.getFullHash(), fullHash);
    }

    @Test
    void getTransactionCount() {
        int count = dao.getTransactionCount();
        assertEquals(14, count);
    }

    @Test
    void getTransactionsFromDbToDb() {
        List<Transaction> result = dao.getTransactions((int)testData.DB_ID_0, (int)testData.DB_ID_9);
        assertNotNull(result);
        assertEquals(9, result.size());
    }

    @Test
    void getTransactionsFromAccount() {
        int count = dao.getTransactionCount(9211698109297098287L, (byte)0, (byte)0);
        assertEquals(9, count);
    }


}