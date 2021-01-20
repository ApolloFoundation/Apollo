/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.app.AplAppStatus;
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
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.KeyFactoryProducer;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionBuilder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TaggedDataServiceTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension extension = new DbExtension(mariaDBContainer, Map.of("tagged_data", List.of("name", "description", "tags")));
    @Inject
    TaggedDataService taggedDataService;
    @Inject
    Blockchain blockchain;
    TaggedTestData tagTd;
    BlockTestData btd;
    @Inject
    TaggedDataTimestampDao timestampDao;
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    private TransactionTestData ttd = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
        TaggedDataServiceImpl.class,
        GlobalSyncImpl.class,
        TaggedDataTable.class,
        DataTagDao.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionRowMapper.class, TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilder.class,
        KeyFactoryProducer.class,
        TaggedDataTimestampDao.class,
        TaggedDataExtendDao.class,
        FullTextConfigImpl.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class, TransactionDaoImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(ConfigDirProvider.class), ConfigDirProvider.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(AplAppStatus.class), AplAppStatus.class))
        .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
        .addBeans(MockBean.of(extension.getFullTextSearchService(), FullTextSearchService.class))
        .addBeans(MockBean.of(mock(AccountService.class), AccountServiceImpl.class, AccountService.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(ttd.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(FullTextSearchUpdater.class), FullTextSearchUpdater.class))
        .build();

    @BeforeEach
    void setUp() throws Exception {
        btd = new BlockTestData();
        tagTd = new TaggedTestData();
    }

    @Tag("skip-fts-init")
    @Test
    void getTaggedDataCount() {
        int result = taggedDataService.getTaggedDataCount();
        assertEquals(5, result);
    }

    @Tag("skip-fts-init")
    @Test
    void getDataTagCount() {
        int result = taggedDataService.getDataTagCount();
        assertEquals(2, result);
    }

    @Tag("skip-fts-init")
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

    @Tag("skip-fts-init")
    @Test
    void getTagsLike() {
        DbIterator<DataTag> result = taggedDataService.getTagsLike("trw", 0, 1);
        int count = 0;
        while (result.hasNext()) {
            DataTag dataTag = result.next();
            assertNotNull(dataTag);
            count++;
        }
        assertEquals(1, count);
    }

    @Tag("skip-fts-init")
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

    @Tag("skip-fts-init")
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

    @Tag("skip-fts-init")
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