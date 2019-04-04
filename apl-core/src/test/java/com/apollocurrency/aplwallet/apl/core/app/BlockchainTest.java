package com.apollocurrency.aplwallet.apl.core.app;

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
import javax.inject.Inject;

@EnableWeld
class BlockchainTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("blockchainTestDb").toAbsolutePath().toString()));
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
    private Blockchain blockchain;
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
    void getBlock() {
        Block block = blockchain.getBlock(blockTestData.BLOCK_0.getId());
        assertNotNull(block);
        assertEquals(blockTestData.BLOCK_0.getId(), block.getId());
    }
// DISABLED tests still creates Weld container and do not shutdown it!!!
//    @Disabled // doesn't work
//    @Test
//    void getLastBlock() {
//        Block block = blockchain.getLastBlock();
//        assertNotNull(block);
//        assertEquals(blockTestData.BLOCK_11.getId(), block.getId());
//    }


}