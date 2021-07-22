/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

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
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.blockchain.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.publickey.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.appdata.impl.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionServiceImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.prunable.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.PublicKeyDao;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.core.shard.ShardDbExplorerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


@Tag("slow")
@EnableWeld
public class DGSPublicFeedbackTableTest extends ValuesDbTableTest<DGSPublicFeedback> {

    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(BlockchainImpl.class, DaoConfig.class,
        GlobalSyncImpl.class,
        FullTextConfigImpl.class,
        DGSPublicFeedbackTable.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TransactionEntityRowMapper.class, TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionBuilderFactory.class,
        GenesisPublicKeyTable.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .build();
    @Inject
    DGSPublicFeedbackTable table;

    DGSTestData dtd;

    public DGSPublicFeedbackTableTest() {
        super(DGSPublicFeedback.class);
    }


    @BeforeEach
    @Override
    public void setUp() {
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    protected List<DGSPublicFeedback> dataToInsert() {
        return List.of(dtd.NEW_PUBLIC_FEEDBACK_0, dtd.NEW_PUBLIC_FEEDBACK_1, dtd.NEW_PUBLIC_FEEDBACK_2);
    }

    @Override
    public DerivedDbTable<DGSPublicFeedback> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSPublicFeedback> getAll() {
        return new ArrayList<>(List.of(dtd.PUBLIC_FEEDBACK_0, dtd.PUBLIC_FEEDBACK_1, dtd.PUBLIC_FEEDBACK_2, dtd.PUBLIC_FEEDBACK_3, dtd.PUBLIC_FEEDBACK_4, dtd.PUBLIC_FEEDBACK_5, dtd.PUBLIC_FEEDBACK_6, dtd.PUBLIC_FEEDBACK_7, dtd.PUBLIC_FEEDBACK_8, dtd.PUBLIC_FEEDBACK_9, dtd.PUBLIC_FEEDBACK_10, dtd.PUBLIC_FEEDBACK_11, dtd.PUBLIC_FEEDBACK_12, dtd.PUBLIC_FEEDBACK_13));
    }

    @Test
    void testGetByPurchaseId() {
        List<DGSPublicFeedback> feedbacks = table.get(dtd.PUBLIC_FEEDBACK_12.getId());

        assertEquals(List.of(dtd.PUBLIC_FEEDBACK_11, dtd.PUBLIC_FEEDBACK_12, dtd.PUBLIC_FEEDBACK_13), feedbacks);
    }

    @Test
    void testGetDeletedByPurchaseId() {
        List<DGSPublicFeedback> feedbacks = table.get(dtd.PUBLIC_FEEDBACK_8.getId());

        assertEquals(0, feedbacks.size());
    }

    @Test
    void testNonexistentById() {
        List<DGSPublicFeedback> feedbacks = table.get(-1);
        assertEquals(0, feedbacks.size());
    }

    @Override
    protected List<DGSPublicFeedback> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }

}
