/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockEntityToModelConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.BlockModelToEntityConverter;
import com.apollocurrency.aplwallet.apl.core.converter.db.PrunableTxRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionEntityRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TransactionRowMapper;
import com.apollocurrency.aplwallet.apl.core.converter.db.TxReceiptRowMapper;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.entity.appdata.BlockIndex;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
/*import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;*/
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEXES;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_0;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_1;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.BLOCK_INDEX_2;
import static com.apollocurrency.aplwallet.apl.data.IndexTestData.NOT_SAVED_BLOCK_INDEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@QuarkusTest
public class BlockIndexDaoTest extends DbContainerBaseTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

/*
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainConfig.class, DaoConfig.class,
        DerivedDbTablesRegistryImpl.class, BlockIndexDao.class,
        TransactionRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionBuilder.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class)
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .build();
*/

    @Inject
    private BlockIndexDao blockIndexDao;

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
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
        assertEquals(1, shardId);
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
        assertEquals(BLOCK_INDEX_2.getBlockHeight(), height.intValue());
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
        assertEquals(Arrays.asList(BLOCK_INDEX_2, BLOCK_INDEX_0, copy), allBlockIndex);
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
        int deleteCount = blockIndexDao.hardDeleteBlockIndex(BLOCK_INDEX_1);
        assertEquals(1, deleteCount);
        assertEquals(Arrays.asList(BLOCK_INDEX_2, BLOCK_INDEX_0), blockIndexDao.getAllBlockIndex());
    }

    @Test
    void testGetBlockIdsAfterHeight() {
        List<Long> blockIdsAfter = blockIndexDao.getBlockIdsAfter(BLOCK_INDEX_1.getBlockHeight(), 2);

        assertEquals(List.of(BLOCK_INDEX_2.getBlockId(), BLOCK_INDEX_0.getBlockId()), blockIdsAfter);
    }

    @Test
    void testGetHeight() {
        Integer height = blockIndexDao.getHeight(BLOCK_INDEX_0.getBlockId());

        assertEquals(BLOCK_INDEX_0.getBlockHeight(), height);
    }

    @Test
    void testCountByShardId() {
        long count = blockIndexDao.countBlockIndexByShard(3L);

        assertEquals(1, count);

        count = blockIndexDao.countBlockIndexByShard(1L);

        assertEquals(1, count);

        count = blockIndexDao.countBlockIndexByShard(2L);

        assertEquals(1, count);
    }

}