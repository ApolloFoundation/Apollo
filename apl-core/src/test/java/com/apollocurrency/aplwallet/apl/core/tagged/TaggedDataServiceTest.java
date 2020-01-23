/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
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

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TaggedDataServiceTest {

    private NtpTime time = mock(NtpTime.class);
    @RegisterExtension
    DbExtension extension = new DbExtension(Map.of("tagged_data", List.of("name","description","tags")));
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            TaggedDataServiceImpl.class,
            GlobalSyncImpl.class,
            TaggedDataDao.class,
            DataTagDao.class,
            KeyFactoryProducer.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(ConfigDirProvider.class), ConfigDirProvider.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(AplAppStatus.class), AplAppStatus.class))
            .addBeans(MockBean.of(time, NtpTime.class))
            .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
            .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
            .addBeans(MockBean.of(mock(AccountService.class), AccountServiceImpl.class, AccountService.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
            .build();

    @Inject
    TaggedDataService taggedDataService;
    @Inject
    Blockchain blockchain;
    TaggedTestData tagTd;
    TransactionTestData ttd;
    BlockTestData btd;
    @Inject
    TaggedDataTimestampDao timestampDao;
    @BeforeEach
    void setUp() throws Exception {
        ttd = new TransactionTestData();
        btd = new BlockTestData();
        tagTd = new TaggedTestData();
    }


    @Test
    void getTaggedDataCount() {
        int result = taggedDataService.getTaggedDataCount();
        assertEquals(5, result);
    }

    @Test
    void getDataTagCount() {
        int result = taggedDataService.getDataTagCount();
        assertEquals(2, result);
    }

    @Test
    void getAllTags() {
        DbIterator<DataTag> result = taggedDataService.getAllTags(0, 1);
        int count = 0;
        while (result.hasNext()) {
            DataTag dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void getTagsLike() {
        DbIterator<DataTag> result = taggedDataService.getTagsLike("trw",0, 1);
        int count = 0;
        while (result.hasNext()) {
            DataTag dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void addDataUploadAttach() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        DbUtils.inTransaction(extension, (con) -> {
            taggedDataService.add(ttd.TRANSACTION_8, tagTd.NOT_SAVED_TagDTsmp_ATTACHMENT);
        });
        DbIterator<TaggedData> result = taggedDataService.getAll(0, 5);
        int count = 0;
        while (result.hasNext()) {
            TaggedData dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(5, count);
        TaggedDataTimestamp dataTimestamp = timestampDao.get(new LongKey(ttd.TRANSACTION_8.getId()));
        assertNotNull(dataTimestamp);
        assertEquals(new TaggedDataTimestamp(ttd.TRANSACTION_8.getId(), ttd.TRANSACTION_8.getTimestamp(), btd.LAST_BLOCK.getHeight()), dataTimestamp);
    }

    @Test
    void restore() {
        DbUtils.inTransaction(extension, (con) -> {
            taggedDataService.restore(ttd.TRANSACTION_8, tagTd.NOT_SAVED_TagDTsmp_ATTACHMENT, btd.BLOCK_7.getTimestamp(), btd.BLOCK_7.getHeight());
        });
        DbIterator<TaggedData> result = taggedDataService.getAll(0, 10);
        int count = 0;
        while (result.hasNext()) {
            TaggedData dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    void extend() {
        blockchain.setLastBlock(btd.LAST_BLOCK);
        DbUtils.inTransaction(extension, (con) -> taggedDataService.extend(ttd.NOT_SAVED_TRANSACTION, tagTd.NOT_SAVED_TagExtend_ATTACHMENT));
        DbIterator<TaggedData> result = taggedDataService.getAll(0, 10);
        int count = 0;
        while (result.hasNext()) {
            TaggedData dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(5, count);

        TaggedDataTimestamp dataTimestamp = timestampDao.get(new LongKey(tagTd.NOT_SAVED_TagExtend_ATTACHMENT.getTaggedDataId()));
        assertNotNull(dataTimestamp);
        assertEquals(new TaggedDataTimestamp(tagTd.NOT_SAVED_TagExtend_ATTACHMENT.getTaggedDataId(), tagTd.TagDTsmp_1.getTimestamp(), btd.LAST_BLOCK.getHeight()), dataTimestamp);
    }

}