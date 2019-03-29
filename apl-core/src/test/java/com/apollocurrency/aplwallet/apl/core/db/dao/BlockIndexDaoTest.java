/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEXES;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_0;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_1;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_2;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.NOT_SAVED_BLOCK_INDEX;
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
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
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
public class BlockIndexDaoTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    @Inject
    private  JdbiHandleFactory jdbiHandleFactory;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistryImpl.class,
            JdbiHandleFactory.class, BlockIndexDao.class, TransactionIndexDao.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(dbExtension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class))
            .build();

    @Inject
    private BlockIndexDao blockIndexDao;
    @Inject
    private TransactionIndexDao transactionIndexDao;

    @AfterEach
    void shutdown() {
        jdbiHandleFactory.close();
    }

    @Test
    void testGetAll() {
        List<BlockIndex> allBlockIndex = blockIndexDao.getAllBlockIndex();
        assertEquals(BLOCK_INDEXES, allBlockIndex);
    }

    @Test
    void testInsert() {
        blockIndexDao.saveBlockIndex(NOT_SAVED_BLOCK_INDEX);
        List<BlockIndex> allBlockIndex = blockIndexDao.getAllBlockIndex();
        ArrayList<BlockIndex> expected = new ArrayList<>(BLOCK_INDEXES);
        expected.add(NOT_SAVED_BLOCK_INDEX);
        assertEquals(expected.size(), allBlockIndex.size());
        assertEquals(expected, allBlockIndex);
    }

    @Test
    void testGetShardIdByBlockId() {
        Long shardId = blockIndexDao.getShardIdByBlockId(BLOCK_INDEX_1.getBlockId());
        assertNotNull(shardId);
        assertEquals(BLOCK_INDEX_1.getShardId(), shardId);
    }

    @Test
    void testGetShardIdByUnknownBlockId() {
        Long shardId = blockIndexDao.getShardIdByBlockId(NOT_SAVED_BLOCK_INDEX.getBlockId());
        assertNull(shardId);
    }

    @Test
    void testGetShardIdByHeight() {
        Long height = blockIndexDao.getShardIdByBlockHeight(BLOCK_INDEX_2.getBlockHeight());
        assertNotNull(height);
        assertEquals(BLOCK_INDEX_2.getBlockHeight().longValue(), height.longValue());
    }

    @Test
    void testGetShardIdByUnknownHeight() {
        Long height = blockIndexDao.getShardIdByBlockHeight(NOT_SAVED_BLOCK_INDEX.getBlockHeight());
        assertNull(height);
    }

    @Test
    void testDeleteAll() {
        int deleteCount = blockIndexDao.hardDeleteAllBlockIndex();
        assertEquals(3, deleteCount);
        List<BlockIndex> allBlockIndex = blockIndexDao.getAllBlockIndex();
        assertEquals(0, allBlockIndex.size());
        // test cascade delete
        List<TransactionIndex> allTransactionIndex = transactionIndexDao.getAllTransactionIndex();
        assertEquals(0, allTransactionIndex.size());
    }

    @Test
    void searchForMissingData() {
        BlockIndex blockIndex = blockIndexDao.getByBlockId(NOT_SAVED_BLOCK_INDEX.getBlockId());
        assertNull(blockIndex);

        Long shardId = blockIndexDao.getShardIdByBlockId(NOT_SAVED_BLOCK_INDEX.getBlockId());
        assertNull(shardId);

        shardId = blockIndexDao.getShardIdByBlockHeight(NOT_SAVED_BLOCK_INDEX.getBlockHeight());
        assertNull(shardId);
    }

    @Test
    void testGetByBlockId() {
        BlockIndex blockIndex = blockIndexDao.getByBlockId(BLOCK_INDEX_1.getBlockId());
        assertNotNull(blockIndex);
        assertEquals(BLOCK_INDEX_1, blockIndex);
    }

    @Test
    void testGetByUnknownBlockId() {
        BlockIndex blockIndex = blockIndexDao.getByBlockId(NOT_SAVED_BLOCK_INDEX.getBlockId());
        assertNull(blockIndex);
    }

    @Test
    void testGetByBlockHeight() {
        BlockIndex blockIndex = blockIndexDao.getByBlockHeight(BLOCK_INDEX_0.getBlockHeight());
        assertNotNull(blockIndex);
        assertEquals(BLOCK_INDEX_0, blockIndex);
    }

    @Test
    void testGetByUknownBlockHeight() {
        BlockIndex blockIndex = blockIndexDao.getByBlockHeight(NOT_SAVED_BLOCK_INDEX.getBlockHeight());
        assertNull(blockIndex);
    }

    @Test
    void testUpdateBlockIndex() {
        BlockIndex copy = BLOCK_INDEX_1.copy();
        copy.setBlockHeight(NOT_SAVED_BLOCK_INDEX.getBlockHeight());
        int updateCount = blockIndexDao.updateBlockIndex(copy);
        assertEquals(1, updateCount);
        BlockIndex blockIndex = blockIndexDao.getByBlockId(BLOCK_INDEX_1.getBlockId());
        assertEquals(blockIndex, copy);
        List<BlockIndex> allBlockIndex = blockIndexDao.getAllBlockIndex();
        assertEquals(Arrays.asList(BLOCK_INDEX_0, copy, BLOCK_INDEX_2), allBlockIndex);
    }

    @Test
    void testCount() {
        int actualCount = blockIndexDao.count();
        assertEquals(BLOCK_INDEXES.size(), actualCount);
    }

    @Test
    void testGetLast() {
        BlockIndex last = blockIndexDao.getLast();

        assertEquals(BLOCK_INDEX_0, last);
    }

    @Test
    void getLastHeight() {
        Integer height = blockIndexDao.getLastHeight();

        assertEquals(BLOCK_INDEX_0.getBlockHeight(), height);
    }

    @Test
    void getLastHeightWhenNoBlockIndexesExist() {
        blockIndexDao.hardDeleteAllBlockIndex();

        Integer height = blockIndexDao.getLastHeight();

        assertNull(height);
    }

    @Test
    void getLastWhenNoBlockIndexesExist() {
        blockIndexDao.hardDeleteAllBlockIndex();

        BlockIndex last = blockIndexDao.getLast();

        assertNull(last);
    }

    @Test
    void testDelete() {
        int deleteCount = blockIndexDao.hardBlockIndex(BLOCK_INDEX_1);
        assertEquals(1, deleteCount);
        assertEquals(Arrays.asList(BLOCK_INDEX_0, BLOCK_INDEX_2), blockIndexDao.getAllBlockIndex());
        // test cascade delete
        List<TransactionIndex> transactionIndexList = transactionIndexDao.getByBlockId(BLOCK_INDEX_1.getBlockId(), Integer.MAX_VALUE);
        assertEquals(0, transactionIndexList.size());
    }
}