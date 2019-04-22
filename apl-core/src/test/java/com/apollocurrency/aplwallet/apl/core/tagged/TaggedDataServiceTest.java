/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDao;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.data.BlockTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TaggedDataServiceTest {

    @RegisterExtension
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("targetDb").toAbsolutePath().toString()));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            TaggedDataServiceImpl.class,
            GlobalSyncImpl.class,
            TaggedDataDao.class,
            DataTagDao.class,
            TaggedDataTimestampDao.class,
            TaggedDataExtendDao.class,
            FullTextConfigImpl.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(extension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManger().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .build();
    @Inject
    TaggedDataService taggedDataService;
    @Inject
    TransactionDao transactionDao;
    @Inject
    Blockchain blockchain;
    TaggedTestData tagTd;
    TransactionTestData ttd;
    BlockTestData btd;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        ttd = new TransactionTestData();
        btd = new BlockTestData();
        tagTd = new TaggedTestData();
    }

    @Test
    void getTaggedDataCount() {
        int result = taggedDataService.getTaggedDataCount();
        assertEquals(0, result);
    }

    @Test
    void getDataTagCount() {
        int result = taggedDataService.getDataTagCount();
        assertEquals(1, result);
    }

    @Test
    void getAllTags() {
        DbIterator<DataTag> result = taggedDataService.getAllTags(0, 1);
        int count = 0;
        while (result.hasNext()) {
            DataTag dataTag = result.next();
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    void getTagsLike() {
        DbIterator<DataTag> result = taggedDataService.getTagsLike("trw",0, 1);
        int count = 0;
        while (result.hasNext()) {
            DataTag dataTag = result.next();
            count++;
        }
        assertEquals(1, count);
    }

    @Disabled
    void addDataUploadAttach() {
        DbUtils.inTransaction(extension, (con) -> {
            taggedDataService.add(ttd.TRANSACTION_8, tagTd.NOT_SAVED_TagDTsmp);
        });
    }

    @Disabled
    void restore() {
        DbUtils.inTransaction(extension, (con) -> {
            taggedDataService.restore(ttd.TRANSACTION_8, tagTd.NOT_SAVED_TagDTsmp, btd.BLOCK_7.getTimestamp(), btd.BLOCK_7.getHeight());
        });
    }

}