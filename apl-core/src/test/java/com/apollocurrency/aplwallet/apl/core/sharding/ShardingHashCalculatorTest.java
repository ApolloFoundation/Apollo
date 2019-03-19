/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.core.app.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbExtension;
import com.apollocurrency.aplwallet.apl.core.db.DbVersion;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import javax.inject.Inject;

@Disabled
@EnableWeld
public class ShardingHashCalculatorTest {
    static final String SHA_512 = "SHA-256";
    static final byte[] FULL_MEKLE_ROOT = Convert.parseHexString("1c3d41be25207be8d1119e958102fbb2e5933ff06f483f125371efae2bc6ca1d1a5248929443a521f850691a9180be51c4490c52aee9fedb1ce026b128acc479");
    static final byte[] PARTIAL_MERKLE_ROOT_2_6 = Convert.parseHexString("fd0c5b17d693d5cd5cd3453090ffcdcfe121e39782de478c43e24e2416d4969901c807bcd2afe2ed8ab13723c9341fe26cf04b8d0405df179bad531c607c7610");
    static final byte[] PARTIAL_MERKLE_ROOT_7_12 = Convert.parseHexString("c6e0d2347aa247757a57d1b52117bc32e8b024f9ec62b5d8a5d40b0765700fea7a63c932a2dd3e12c06477a24c1074a6971b6819c79c6beebfb42e866cca389d");
    static final byte[] PARTIAL_MERKLE_ROOT_1_8 = Convert.parseHexString("b492fe046090127b5a53fc425e117c8aea4535ee0ad90e9affb98a056951671d66594ebaca0f064042a0b0ac0856273520eb2f487aab2dce3d7c822f3127516f");
    BlockchainConfig blockchainConfig = Mockito.mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
    DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
    HeightConfig heightConfig = Mockito.mock(HeightConfig.class);

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(BlockchainImpl.class, BlockDaoImpl.class, DerivedDbTablesRegistry.class, EpochTime.class, GlobalSyncImpl.class, TransactionDaoImpl.class)
            .addBeans(
                    MockBean.of(blockchainConfig, BlockchainConfig.class),
                    MockBean.of(propertiesHolder, PropertiesHolder.class),
                    MockBean.of(databaseManager, DatabaseManager.class),
                    MockBean.of(Mockito.mock(NtpTime.class), NtpTime.class)
            ).build();
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();
    @Inject
    Blockchain blockchain;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(dbExtension.getDataSource()).when(databaseManager).getDataSource();
        Mockito.doReturn(SHA_512).when(heightConfig).getShardingDigestAlgorithm();
        Mockito.doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
    }
    @Test
    public void testCalculateHashForAllBlocks() throws IOException {
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 500);

        byte[] merkleRoot1 = shardingHashCalculator.calculateHash(BlockTestData.BLOCK_1.getHeight(), BlockTestData.BLOCK_12.getHeight() + 1);
        byte[] merkleRoot2 = shardingHashCalculator.calculateHash(0, BlockTestData.BLOCK_12.getHeight() + 1);
        byte[] merkleRoot3 = shardingHashCalculator.calculateHash(0, BlockTestData.BLOCK_12.getHeight() + 20000);

        Assertions.assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot1);
        Assertions.assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot2);
        Assertions.assertArrayEquals(FULL_MEKLE_ROOT, merkleRoot3);
    }
    @Test
    public void testCalculateHashWhenNoBlocks() throws IOException {
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 200);

        byte[] merkleRoot = shardingHashCalculator.calculateHash(0, BlockTestData.BLOCK_1.getHeight());

        Assertions.assertNull(merkleRoot);
    }
    @Test
    public void testCalculateHashForMiddleBlocks() throws IOException {
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 200);

        byte[] merkleRoot = shardingHashCalculator.calculateHash(BlockTestData.BLOCK_2.getHeight(), BlockTestData.BLOCK_6.getHeight());

        Assertions.assertArrayEquals(PARTIAL_MERKLE_ROOT_2_6, merkleRoot);
    }
    @Test
    public void testCalculateHashForFirstBlocks() throws IOException {
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 200);

        byte[] merkleRoot = shardingHashCalculator.calculateHash(0, BlockTestData.BLOCK_9.getHeight());

        Assertions.assertArrayEquals(PARTIAL_MERKLE_ROOT_1_8, merkleRoot);
    }
    @Test
    public void testCalculateHashForLastBlocks() throws IOException {
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 200);

        byte[] merkleRoot = shardingHashCalculator.calculateHash(BlockTestData.BLOCK_7.getHeight(), BlockTestData.BLOCK_12.getHeight() + 1000);

        Assertions.assertArrayEquals(PARTIAL_MERKLE_ROOT_7_12, merkleRoot);
    }
    @Test
    public void testCreateShardingHashCalculatorWithZeroBlockSelectLimit() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ShardingHashCalculator(blockchain, blockchainConfig, 0));
    }

    @Test
    @Disabled
    public void testCalculateShardingHashFromMainDb() {
        DbProperties dbFileProperties = DbTestData.getDbFileProperties(Paths.get("unit-test-db").resolve(Constants.APPLICATION_DIR_NAME).toAbsolutePath().toString());
        TransactionalDataSource transactionalDataSource = new TransactionalDataSource(dbFileProperties, new PropertiesHolder());
        transactionalDataSource.init(new DbVersion() {
            @Override
            protected void update(int nextUpdate) {} //do not modify original db!!!
        });
        Mockito.doReturn(transactionalDataSource).when(databaseManager).getDataSource();
        ShardingHashCalculator shardingHashCalculator = new ShardingHashCalculator(blockchain, blockchainConfig, 5000);
        byte[] bytes = shardingHashCalculator.calculateHash(0, 2_000_000);
    }
}
