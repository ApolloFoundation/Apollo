/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.hash;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.app.BlockImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.ShardDao;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class ShardHashCalculatorImplTest {
    static final String SHA_256 = "SHA-256";
    static final byte[] FULL_MEKLE_ROOT = Convert.parseHexString("b87941d4db242065ac84b4b14dd2b35e22d89d7f41272c0a6448a2c1734c444d");
    static final byte[] PARTIAL_MERKLE_ROOT_2_6 =  Convert.parseHexString("57a86e3f4966f6751d661fbb537780b65d4b0edfc1b01f48780a360c4babdea7");
    static final byte[] PARTIAL_MERKLE_ROOT_7_12 = Convert.parseHexString("da5ad74821dc77fa9fb0f0ddd2e48284fe630fee9bf70f98d7aa38032ddc8f57");
    static final byte[] PARTIAL_MERKLE_ROOT_1_8 =  Convert.parseHexString("3987b0f2fb15fdbe3e815cbdd1ff8f9527d4dc18989ae69bc446ca0b40759a6b");
    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    DatabaseManager databaseManager = mock(DatabaseManager.class);
    HeightConfig heightConfig = mock(HeightConfig.class);
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();
    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(BlockchainImpl.class, ShardHashCalculatorImpl.class, BlockImpl.class, BlockDaoImpl.class, DerivedDbTablesRegistryImpl.class, EpochTime.class, GlobalSyncImpl.class, TransactionDaoImpl.class, DaoConfig.class,
            JdbiHandleFactory.class)
            .addBeans(
                    MockBean.of(blockchainConfig, BlockchainConfig.class),
                    MockBean.of(propertiesHolder, PropertiesHolder.class),
                    MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class),
                    MockBean.of(dbExtension.getDatabaseManger().getJdbi(), Jdbi.class),
                    MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class),
                    MockBean.of(mock(NtpTime.class), NtpTime.class)
            ).build();

    @Inject
    JdbiHandleFactory jdbiHandleFactory;
    @Inject
    ShardHashCalculator shardHashCalculator;
    BlockTestData td;

    @Inject
    Blockchain blockchain;
    @BeforeEach
    void setUp() {
        Mockito.doReturn(SHA_256).when(heightConfig).getShardingDigestAlgorithm();
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        td = new BlockTestData();
        blockchain.setLastBlock(td.BLOCK_11);
    }

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }
    @Test
    void testCalculateHashForAllBlocks() throws IOException {

        byte[] merkleRoot1 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight(), td.BLOCK_11.getHeight() + 1);
        byte[] merkleRoot2 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight(), td.BLOCK_11.getHeight() + 1);
        byte[] merkleRoot3 = shardHashCalculator.calculateHash(td.GENESIS_BLOCK.getHeight(), td.BLOCK_11.getHeight() + 20000);
        assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot1);
        assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot2);
        assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot3);
    }

    private void hash(List<Block> blocks) {
        try {
            MerkleTree merkleTree = new MerkleTree(MessageDigest.getInstance(SHA_256));
            blocks.stream().map(Block::getBlockSignature).forEach(merkleTree::appendLeaf);
            merkleTree.appendLeaf(td.GENESIS_BLOCK.getGenerationSignature());
            byte[] value = merkleTree.getRoot().getValue();
            System.out.println(Convert.toHexString(value));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    @Test
    void testCalculateHashWhenNoBlocks() throws IOException {

        byte[] merkleRoot = shardHashCalculator.calculateHash(td.BLOCK_11.getHeight() + 1, td.BLOCK_11.getHeight() + 100_000);

        Assertions.assertNull(merkleRoot);
    }

    @Test
    void testCalculateHashForMiddleBlocks() throws IOException {
        byte[] merkleRoot = shardHashCalculator.calculateHash(td.BLOCK_1.getHeight(), td.BLOCK_5.getHeight());
        assertArrayEquals(PARTIAL_MERKLE_ROOT_2_6, merkleRoot);
    }
    @Test
    void testCalculateHashForFirstBlocks() throws IOException {

        byte[] merkleRoot = shardHashCalculator.calculateHash(0, td.BLOCK_8.getHeight());
        assertArrayEquals(PARTIAL_MERKLE_ROOT_1_8, merkleRoot);
    }
    @Test
    void testCalculateHashForLastBlocks() throws IOException {

        byte[] merkleRoot = shardHashCalculator.calculateHash(td.BLOCK_6.getHeight(), td.BLOCK_11.getHeight() + 1000);
        assertArrayEquals(PARTIAL_MERKLE_ROOT_7_12, merkleRoot);
    }
    @Test
    void testCreateShardingHashCalculatorWithZeroBlockSelectLimit() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ShardHashCalculatorImpl(mock(Blockchain.class), mock(BlockchainConfig.class), mock(ShardDao.class), 0));
    }

//    @Test
//    public void testCalculateShardingHashFromMainDb() {
//        DbProperties dbFileProperties = DbTestData.getDbFileProperties(Paths.get("unit-test-db").resolve(Constants.APPLICATION_DIR_NAME).toAbsolutePath().toString());
//        TransactionalDataSource transactionalDataSource = new TransactionalDataSource(dbFileProperties, new PropertiesHolder());
//        transactionalDataSource.init(new DbVersion() {
//            @Override
//            protected int update(int nextUpdate) {return 260;} //do not modify original db!!!
//        });
//        Mockito.doReturn(transactionalDataSource).when(databaseManager).getDataSource();
//        byte[] bytes = shardHashCalculator.calculateHash(0, 2_000_000);
//    }
}
