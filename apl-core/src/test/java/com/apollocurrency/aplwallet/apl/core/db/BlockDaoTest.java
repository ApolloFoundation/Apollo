package com.apollocurrency.aplwallet.apl.core.db;

import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_0_ID;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_3_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.BLOCK_7_TIMESTAMP;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_HEIGHT;
import static com.apollocurrency.aplwallet.apl.data.BlockTestData.GENESIS_BLOCK_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
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
import java.util.Set;
import javax.inject.Inject;

@EnableWeld
class BlockDaoTest {

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
    private BlockDao blockDao;
    private BlockTestData testData;

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
        testData = new BlockTestData();
    }

    @AfterEach
    void shutdown() {
        jdbiHandleFactory.close();
    }

    @Test
    void findByBlockId() {
        Block block = blockDao.findBlock(BLOCK_0_ID, );
        assertEquals(block.getId(), BLOCK_0_ID);
    }

    @Test
    void findLastBlock() {
        Block block = blockDao.findLastBlock();
        assertEquals(block.getId(), testData.LAST_BLOCK.getId());
    }

    @Test
    void hasLastBlockFromTo() {
        boolean isBlock = blockDao.hasBlock(testData.BLOCK_3.getId(), BLOCK_3_HEIGHT, );
        assertTrue(isBlock);
    }

    @Test
    void hasLastBlock() {
        boolean isBlock = blockDao.hasBlock(testData.BLOCK_3.getId());
        assertTrue(isBlock);
    }

    @Test
    void findLastBlockTimestamp() {
        Block block = blockDao.findLastBlock(BLOCK_7_TIMESTAMP);
        assertEquals(block.getTimestamp(), BLOCK_7_TIMESTAMP);
    }

    @Test
    void findBlockAtHeight() {
        Block block = blockDao.findBlockAtHeight(BLOCK_7_HEIGHT, );
        assertEquals(block.getTimestamp(), BLOCK_7_TIMESTAMP);
    }

    @Test
    void findBlockCountRange() {
        Long count = blockDao.getBlockCount(BLOCK_0_HEIGHT, BLOCK_7_HEIGHT);
        assertEquals(7L , count.longValue());
    }

    @Test
    void getBlocksRange() {
        DbIterator<Block> result = blockDao.getBlocks(BLOCK_7_HEIGHT, BLOCK_0_HEIGHT);
        assertNotNull(result);
        int count = 0;
        while (result.hasNext()) {
            result.next();
            count++;
        }
        assertEquals(8, count);
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
        Set<Long> count = blockDao.getBlockGenerators(BLOCK_0_HEIGHT, );
        assertNotNull(count);
        assertEquals(2 , count.size());
    }

    @Test
    void countByHeight() {
        long count = blockDao.getBlockCount(GENESIS_BLOCK_HEIGHT, BlockTestData.BLOCK_7_HEIGHT);
        assertEquals(8, count);

        count = blockDao.getBlockCount(extension.getDatabaseManger().getDataSource(), BlockTestData.BLOCK_7_HEIGHT, BlockTestData.BLOCK_11_HEIGHT);
        assertEquals(4, count);
    }
}