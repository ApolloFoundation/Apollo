/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static com.apollocurrency.aplwallet.apl.crypto.Convert.parseHexString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static com.apollocurrency.aplwallet.apl.data.ShardTestData.*;
import static org.junit.jupiter.api.Assertions.*;

import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManagerImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbExtension;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.Shard;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            DerivedDbTablesRegistry.class,
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

        assertEquals(2, allShards.size());
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

        assertEquals(3, actual.size());
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

        assertEquals(Arrays.asList(copy, SHARD_1), allShards);
    }

    @Test
    void testDelete() {
        int deleteCount = dao.hardDeleteShard(SHARD_0.getShardId());
        assertEquals(1, deleteCount);

        List<Shard> allShards = dao.getAllShard();
        assertEquals(Collections.singletonList(SHARD_1), allShards);
    }

    @Test
    void testDeleteAll() {
        int deleteCount = dao.hardDeleteAllShards();
        assertEquals(2, deleteCount);
        List<Shard> allShards = dao.getAllShard();
        assertEquals(0, allShards.size());
    }

    @Test
    void testCount() {
        long count = dao.countShard();
        assertEquals(2, count);
    }

    @Test
    void insertDelete() {
        long maxId = dao.getMaxShardId();
        assertEquals(3, maxId);

        Shard shard = new Shard(3L, "aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f");
        dao.saveShard(shard);
        List<Shard> result = dao.getAllShard();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertNotNull(result.get(0).getShardId());
        assertNotNull(result.get(0).getShardHash());

        long count = dao.countShard();
        assertEquals(3, count);
        maxId = dao.getMaxShardId();
        assertEquals(4, maxId);

        long nextId = dao.getNextShardId();
        assertEquals(4, nextId);

        Shard shard2 = new Shard(5L, "0000005");
        dao.saveShard(shard2);

        maxId = dao.getMaxShardId();
        assertEquals(6, maxId);

        Shard found1 = dao.getShardById(3L);
        assertNotNull(found1);
        assertNotNull(found1.getShardId());
        assertArrayEquals(parseHexString("aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f"),
                found1.getShardHash() );

        found1.setShardHash("000000123".getBytes());
        dao.updateShard(found1);

        Shard found3 = dao.getShardById(3L);
        assertNotNull(found3);
        assertArrayEquals("000000123".getBytes(), found3.getShardHash());

        dao.hardDeleteShard(1L);
        count = dao.countShard();
        assertEquals(3, count);

        dao.hardDeleteAllShards();
    }
}