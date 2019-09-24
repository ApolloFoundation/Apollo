package com.apollocurrency.aplwallet.apl.core.tagged.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.*;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class DataTagDaoTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSyncImpl.class,
            TaggedDataTimestampDao.class,
            FullTextConfigImpl.class, DataTagDao.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
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
        assertEquals(List.of(tagtd.dataTag_1, tagtd.dataTag_2, tagtd.dataTag_3, tagtd.dataTag_4, tagtd.dataTag_NOT_SAVED), all);
    }

    @Test
    void testRollback() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> dataTagDao.rollback(tagtd.dataTag_4.getHeight()));
        assertEquals(List.of(tagtd.dataTag_1, tagtd.dataTag_2, tagtd.dataTag_3, tagtd.dataTag_4),
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

        assertEquals(expected, dataTags);
    }

}