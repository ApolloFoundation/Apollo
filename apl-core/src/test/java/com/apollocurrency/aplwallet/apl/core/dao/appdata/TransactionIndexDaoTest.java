/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.TransactionIndex;
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
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.IndexTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@Slf4j
@Tag("slow")
@EnableWeld
public class TransactionIndexDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(BlockchainImpl.class, DaoConfig.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilderFactory.class,
        GlobalSyncImpl.class,
        DerivedDbTablesRegistryImpl.class, BlockIndexDao.class, TransactionIndexDao.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .build();

    @Inject
    TransactionIndexDao dao;

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }

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