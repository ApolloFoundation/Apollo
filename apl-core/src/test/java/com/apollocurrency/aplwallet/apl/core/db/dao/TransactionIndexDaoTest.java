/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSync;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.TransactionIndex;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.data.IndexTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class TransactionIndexDaoTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(NtpTime.class,
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSync.class,
            GlobalSyncImpl.class,
            DerivedDbTablesRegistryImpl.class, BlockIndexDao.class, TransactionIndexDao.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class))
            .build();

    @Inject
    TransactionIndexDao dao;

    @Test
    void testGetAll() {
        List<TransactionIndex> result = dao.getAllTransactionIndex();
        assertNotNull(result);
        Assertions.assertEquals(IndexTestData.TRANSACTION_INDEXES.size(), result.size());
        Assertions.assertEquals(IndexTestData.TRANSACTION_INDEXES, result);
    }

    @Test
    void testGetCountByBlockHeight() {
        long count = dao.countTransactionIndexByBlockHeight(IndexTestData.BLOCK_INDEX_0.getBlockHeight());
        assertEquals(1L, count);
    }
    @Test
    void testGetCountByUnknownBlockId() {
        long count = dao.countTransactionIndexByBlockHeight(IndexTestData.NOT_SAVED_BLOCK_INDEX.getBlockHeight());
        assertEquals(0L, count);
    }

    @Test
    void testGetByTransactionId() {
        TransactionIndex found = dao.getByTransactionId(IndexTestData.TRANSACTION_INDEX_0.getTransactionId());
        assertNotNull(found);
        Assertions.assertEquals(IndexTestData.TRANSACTION_INDEX_0, found);
    }

    @Test
    void testDelete() {
        int deleteCount = dao.hardDeleteTransactionIndex(IndexTestData.TRANSACTION_INDEX_0);
        Assertions.assertEquals(1, deleteCount);
        long actualCount = dao.countTransactionIndexByBlockHeight(IndexTestData.TRANSACTION_INDEX_0.getHeight());
        Assertions.assertEquals(0, actualCount);
    }

    @Test
    void searchForMissingData() {
        TransactionIndex transactionIndex = dao.getByTransactionId(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1.getTransactionId());
        assertNull(transactionIndex);

        Long shardId = dao.getShardIdByTransactionId(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1.getTransactionId());
        assertNull(shardId);

        List<TransactionIndex> result = dao.getByBlockHeight(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1.getHeight(), 10);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetShardIdByTransactionId() {
        Long shardId = dao.getShardIdByTransactionId(IndexTestData.TRANSACTION_INDEX_1.getTransactionId());
        assertNotNull(shardId);
        assertEquals(1, shardId);
    }
    @Test
    void testInsert() {
        dao.saveTransactionIndex(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_0);
        dao.saveTransactionIndex(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1);
        List<TransactionIndex> result = dao.getByBlockHeight(IndexTestData.BLOCK_INDEX_2.getBlockHeight(), 10);
        assertNotNull(result);
        List<TransactionIndex> expectedByBlockid = Arrays.asList(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_0, IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1);
        assertEquals(2, result.size());
        Assertions.assertEquals(expectedByBlockid, result);
        List<TransactionIndex> all = dao.getAllTransactionIndex();
        List<TransactionIndex> expectedAll = Arrays.asList(
                IndexTestData.TRANSACTION_INDEX_1,
                IndexTestData.TRANSACTION_INDEX_2,
                IndexTestData.TRANSACTION_INDEX_3,
                IndexTestData.NOT_SAVED_TRANSACTION_INDEX_0,
                IndexTestData.NOT_SAVED_TRANSACTION_INDEX_1,
                IndexTestData.TRANSACTION_INDEX_0
                );
        Assertions.assertEquals(6, all.size());
        Assertions.assertEquals(expectedAll, all);
    }

    @Test
    void testUpdateBlockIndex() {
        TransactionIndex copy = IndexTestData.TRANSACTION_INDEX_3.copy();
        copy.setHeight(2);
        int updateCount = dao.updateBlockIndex(copy);
        Assertions.assertEquals(1, updateCount);
        TransactionIndex found = dao.getByTransactionId(IndexTestData.TRANSACTION_INDEX_3.getTransactionId());
        assertNotNull(found);
        Assertions.assertEquals(copy, found);
        List<TransactionIndex> expected = Arrays.asList(IndexTestData.TRANSACTION_INDEX_1, IndexTestData.TRANSACTION_INDEX_2, copy, IndexTestData.TRANSACTION_INDEX_0);
        List<TransactionIndex> all = dao.getAllTransactionIndex();
        Assertions.assertEquals(expected.size(), all.size());
        Assertions.assertEquals(expected, all);
    }

    @Test
    void testDeleteAll() {
        int deleteCount = dao.hardDeleteAllTransactionIndex();
        Assertions.assertEquals(4, deleteCount);
        List<TransactionIndex> allTransactionIndexes = dao.getAllTransactionIndex();
        Assertions.assertNotNull(allTransactionIndexes);
        Assertions.assertEquals(0, allTransactionIndexes.size());
    }

    @Test
    void testGetHeightForTransactionId() {
        Integer height = dao.getTransactionHeightByTransactionId(IndexTestData.TRANSACTION_INDEX_0.getTransactionId());
        Assertions.assertEquals(IndexTestData.BLOCK_INDEX_0.getBlockHeight(), height);
    }
    @Test
    void testGetHeightForUnknownTransaction() {
        Integer height = dao.getTransactionHeightByTransactionId(IndexTestData.NOT_SAVED_TRANSACTION_INDEX_0.getTransactionId());
        Assertions.assertNull(height);
    }

    @Test
    void testCountTransactionIndexesByShardId() {


        long count = dao.countTransactionIndexByShardId(1L);

        assertEquals(3, count);

        count = dao.countTransactionIndexByShardId(2L);

        assertEquals(0, count);

        count = dao.countTransactionIndexByShardId(3L);

        assertEquals(1, count);
    }

    @Test
    void testCountTransactionIndexesyShardIdWhichNotExist() {
        long count = dao.countTransactionIndexByShardId(Long.MAX_VALUE);

        assertEquals(0, count);
    }

}