/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static com.apollocurrency.aplwallet.apl.data.ShardTestData.NOT_SAVED_SHARD;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARDS;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_0;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_1;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.SHARD_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.data.IndexTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
class ShardDaoTest {

    @Inject
    private JdbiHandleFactory jdbiHandleFactory;
    @RegisterExtension
    static DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class, ShardDao.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .build();

    @Inject
    private ShardDao dao;

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @Test
    void testGetAll() {
        List<Shard> allShards = dao.getAllShard();

        assertEquals(SHARDS.size(), allShards.size());
        assertEquals(SHARDS, allShards);
    }

    @Test
    void testGetById() {
        Shard shard = dao.getShardById(SHARD_0.getShardId());

        assertNotNull(shard);
        assertEquals(SHARD_0, shard);
    }

    @Test
    void testUnknownShardById() {
        Shard shard = dao.getShardById(NOT_SAVED_SHARD.getShardId());

        assertNull(shard);
    }

    @Test
    void testInsert() {
        int insertCount = dao.saveShard(NOT_SAVED_SHARD);

        assertEquals(1, insertCount);

        Shard found = dao.getShardById(NOT_SAVED_SHARD.getShardId());

        assertNotNull(found);
        assertEquals(NOT_SAVED_SHARD, found);

        List<Shard> actual = dao.getAllShard();
        List<Shard> expected = new ArrayList<>(SHARDS);
        expected.add(NOT_SAVED_SHARD);

        assertEquals(SHARDS.size() + 1, actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void testUpdate() {
        Shard copy = SHARD_0.copy();
        copy.setShardHash(NOT_SAVED_SHARD.getShardHash());

        int updateCount = dao.updateShard(copy);

        assertEquals(1, updateCount);

        Shard found = dao.getShardById(SHARD_0.getShardId());

        assertEquals(copy, found);

        List<Shard> allShards = dao.getAllShard();

        assertEquals(Arrays.asList(copy, SHARD_1, SHARD_2), allShards);
    }

    @Test
    void testDelete() {
        int deleteCount = dao.hardDeleteShard(SHARD_1.getShardId());
        assertEquals(1, deleteCount);

        List<Shard> allShards = dao.getAllShard();
        assertEquals(Arrays.asList(SHARD_0, SHARD_2), allShards);
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
        long maxId = dao.getMaxShardId();
        assertEquals(SHARDS.get(SHARDS.size() - 1).getShardId() + 1, maxId);
    }

    @Test
    void testGetLast() {
        Shard lastShard = dao.getLastShard();
        assertEquals(SHARD_2, lastShard);
    }

    @Test
    void testGetShardAtHeight() {
        Shard shardAtHeight = dao.getShardAtHeight(IndexTestData.BLOCK_INDEX_1.getBlockHeight());
        assertEquals(shardAtHeight, SHARD_0);
    }
}