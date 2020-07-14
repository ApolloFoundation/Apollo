/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.tagged;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.config.NtpTimeConfig;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.prunable.impl.PrunableMessageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.data.TaggedTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
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
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Tag("slow")
@EnableWeld
@Execution(ExecutionMode.CONCURRENT)
class TaggedDataTimestampDaoTest {

    @RegisterExtension
    DbExtension extension = new DbExtension();
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
        GlobalSyncImpl.class,
        TaggedDataTimestampDao.class,
        FullTextConfigImpl.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class, TransactionDaoImpl.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class, PrunableMessageServiceImpl.class))
        .addBeans(MockBean.of(mock(AliasService.class), AliasService.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .build();

    @Inject
    TaggedDataTimestampDao dataTimestampDao;
    TaggedTestData tagtd;
    TransactionTestData ttd;


    @BeforeEach
    void setUp() throws Exception {
        ttd = new TransactionTestData();
        tagtd = new TaggedTestData();
    }

    @Test
    void getDataStampById() {
        DbKey dbKey = dataTimestampDao.newDbKey(tagtd.TagDTsmp_1);
        TaggedDataTimestamp stamp = dataTimestampDao.get(dbKey);
        assertNotNull(stamp);
        assertEquals(stamp.getId(), tagtd.TagDTsmp_1.getId());
    }

    @Test
    void getDataStampAllById() throws Exception {
        List<TaggedDataTimestamp> result = dataTimestampDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void insertData() throws Exception {
        DbUtils.inTransaction(extension, (con) -> dataTimestampDao.insert(tagtd.NOT_SAVED_TagDTsmp));
        List<TaggedDataTimestamp> all = dataTimestampDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues();
        assertEquals(List.of(tagtd.TagDTsmp_1, tagtd.TagDTsmp_2, tagtd.TagDTsmp_3, tagtd.NOT_SAVED_TagDTsmp), all);
    }

    @Test
    void testTruncate() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> dataTimestampDao.truncate());
        assertTrue(dataTimestampDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues().isEmpty(), "Table should not have any entries after truncating");
    }

    @Test
    void testRollback() throws SQLException {
        DbUtils.inTransaction(extension, (con) -> dataTimestampDao.rollback(tagtd.TagDTsmp_1.getHeight()));
        assertEquals(List.of(tagtd.TagDTsmp_1),
            dataTimestampDao.getAllByDbId(0, 100, Long.MAX_VALUE).getValues());
    }

}