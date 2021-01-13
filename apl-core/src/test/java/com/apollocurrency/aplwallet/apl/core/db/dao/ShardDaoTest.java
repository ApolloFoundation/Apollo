/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.ShardDao;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.Shard;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.ShardTestData.NOT_SAVED_SHARD;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARDS;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_0;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_1;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_2;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class ShardDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class, ShardDao.class,
        GlobalSyncImpl.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilder.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class, TransactionDaoImpl.class)
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();

    @Inject
    private ShardDao dao;

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Test
    void testGetAll() {
        List<Shard> allShards = dao.getAllShard();

        assertEquals(SHARDS.size(), allShards.size());
        assertIterableEquals(SHARDS, allShards);
    }

    @Test
    void testGetById() {
        Shard shard = dao.getShardById(SHARD_0.getShardId());

        assertNotNull(shard);
        assertEquals(SHARD_0, shard);
    }

    @Test
    void testUnknownShardById() {
        Shard shard = dao.getShardById(100_000); // UNKNOWN id

        assertNull(shard);
    }

    @Test
    void testInsert() {
        dao.saveShard(NOT_SAVED_SHARD);

        Shard found = dao.getShardById(NOT_SAVED_SHARD.getShardId());

        assertNotNull(found);
        assertEquals(NOT_SAVED_SHARD, found);
        assertEquals(NOT_SAVED_SHARD.getShardState(), found.getShardState());
        assertArrayEquals(NOT_SAVED_SHARD.getShardHash(), found.getShardHash());
        assertEquals(NOT_SAVED_SHARD.getShardHeight(), found.getShardHeight());
        assertArrayEquals(NOT_SAVED_SHARD.getCoreZipHash(), found.getCoreZipHash());

        List<Shard> actual = dao.getAllShard();
        List<Shard> expected = new ArrayList<>(SHARDS);
        expected.add(NOT_SAVED_SHARD);

        assertEquals(SHARDS.size() + 1, actual.size());
        assertIterableEquals(expected, actual);
    }

    @Test
    void testUpdate() {
        Shard copy = SHARD_0.copy();
        copy.setShardHash(NOT_SAVED_SHARD.getShardHash());

        int updateCount = dao.updateShard(copy);

        assertEquals(1, updateCount);

        Shard found = dao.getShardById(SHARD_0.getShardId());

        assertEquals(copy, found);
        assertEquals(copy.getShardId(), found.getShardId());
        assertEquals(copy.getShardState(), found.getShardState());
        assertArrayEquals(copy.getShardHash(), found.getShardHash());
        assertEquals(copy.getShardHeight(), found.getShardHeight());
        assertArrayEquals(copy.getCoreZipHash(), found.getCoreZipHash());

        List<Shard> allShards = dao.getAllShard();

        assertIterableEquals(Arrays.asList(copy, SHARD_1, SHARD_2), allShards);
    }

    @Test
    void testDelete() {
        int deleteCount = dao.hardDeleteShard(SHARD_1.getShardId());
        assertEquals(1, deleteCount);

        List<Shard> allShards = dao.getAllShard();
        assertIterableEquals(Arrays.asList(SHARD_0, SHARD_2), allShards);
    }

    @Test
    void testDeleteAll() {
        int deleteCount = dao.hardDeleteAllShards();
        assertEquals(SHARDS.size(), deleteCount);
        List<Shard> allShards = dao.getAllShard();
        assertEquals(0, allShards.size());
    }

    @Test
    void testCount() {
        long count = dao.countShard();
        assertEquals(SHARDS.size(), count);
    }

    @Test
    void testGetMaxShardId() {
        Long maxId = dao.getMaxShardId();
        assertEquals(SHARDS.get(SHARDS.size() - 1).getShardId(), maxId);
    }

    @Test
    void testGetLast() {
        Shard lastShard = dao.getLastShard();
        assertEquals(SHARD_2, lastShard);
    }

    @Test
    void testGetShardAtHeight() {
        Shard shardAtHeight = dao.getShardAtHeight(SHARD_1.getShardHeight());
        assertEquals(SHARD_1, shardAtHeight);
    }

    @Test
    void testGetLastCompletedShard() {
        Shard shard = dao.getLastCompletedShard();
        assertEquals(SHARD_1, shard);
    }

    @Test
    void testGetLastCompletedOrArchivedShard() {
        Shard shard = dao.getLastCompletedOrArchivedShard();
        assertEquals(SHARD_2, shard);
    }

    @Test
    void testGetAllCompletedOrArchivedShards() {
        List<Shard> result = dao.getAllCompletedOrArchivedShards();
        assertIterableEquals(Arrays.asList(SHARD_2, SHARD_1), result);
    }

    @Test
    void testGetLatestShardHeight() {
        int latestShardHeight = dao.getLatestShardHeight();
        assertEquals(SHARD_2.getShardHeight(), latestShardHeight);
    }

    @Test
    void testGetCompletedBetweenBlockHeight() {
        List<Shard> result = dao.getCompletedBetweenBlockHeight(2, 4);
        assertEquals(1, result.size());
        assertIterableEquals(Collections.singletonList(SHARD_1), result);
    }
}