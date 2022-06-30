/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.config.TimeConfig;
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
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class DataTagDaoTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer);
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    TimeConfig config = new TimeConfig(false);
    TimeService timeService = new TimeServiceImpl(config.timeSource());

    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(BlockchainImpl.class, DaoConfig.class,
        GlobalSyncImpl.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilderFactory.class,
        TaggedDataTimestampDao.class,
        FullTextConfigImpl.class, DataTagDao.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(mock(AliasService.class), AliasService.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(timeService, TimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .build();

    @Inject
    DataTagDao dataTagDao;
    TaggedTestData tagtd;
    TransactionTestData ttd;


    @BeforeEach
    void setUp() throws Exception {
        ttd = new TransactionTestData();
        tagtd = new TaggedTestData();
    }

    @AfterEach
    void tearDown() {
        extension.cleanAndPopulateDb();
    }

    @Test
    void getDataTagById() {
        DbKey dbKey = dataTagDao.newDbKey(tagtd.dataTag_1);
        DataTag result = dataTagDao.get(dbKey);
        assertNotNull(result);
        assertEquals(result.getTag(), tagtd.dataTag_1.getTag());
    }

    @Test
    void getDataTagAllById() throws Exception {
        List<DataTag> result = dataTagDao.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertNotNull(result);
        assertEquals(4, result.size());
    }

    @Test
    void insertDataTag() throws Exception {
        DbUtils.inTransaction(extension, (con) -> dataTagDao.insert(tagtd.dataTag_NOT_SAVED));
        List<DataTag> all = dataTagDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues();
        List<DataTag> dataTagTestData = List.of(tagtd.dataTag_1, tagtd.dataTag_2, tagtd.dataTag_3, tagtd.dataTag_4, tagtd.dataTag_NOT_SAVED);
        assertEquals(dataTagTestData.size(), all.size());
        assertIterableEquals(dataTagTestData, all); // dataTag_NOT_SAVED.getDbId is different from real value in DB
    }

    @Test
    void testRollback() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> dataTagDao.rollback(tagtd.dataTag_4.getHeight()));
        assertIterableEquals(List.of(tagtd.dataTag_1, tagtd.dataTag_2, tagtd.dataTag_3, tagtd.dataTag_4),
            dataTagDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues());
    }

    @Test
    void testAdd() {
        TaggedData taggedData = mock(TaggedData.class);
        doReturn(1_000_000).when(taggedData).getHeight();
        doReturn(new String[]{tagtd.dataTag_1.getTag(), "newTag"}).when(taggedData).getParsedTags();

        DbUtils.inTransaction(extension, (con) -> dataTagDao.add(taggedData));

        List<DataTag> dataTags = CollectionUtil.toList(dataTagDao.getAllTags(0, 4));

        DataTag updatedDataTag = new DataTag(tagtd.dataTag_1.getTag(), 1_000_000, tagtd.dataTag_1.getCount() + 1);
        updatedDataTag.setLatest(true);
        updatedDataTag.setDbId(41);

        DataTag newTag = new DataTag("newTag", 1_000_000, 1);
        newTag.setLatest(true);
        newTag.setDbId(42);
        tagtd.dataTag_4.setDbId(40);

        List<DataTag> expected = List.of(
            updatedDataTag,
            newTag,
            tagtd.dataTag_4
        );

        assertIterableEquals(expected, dataTags);
    }

}