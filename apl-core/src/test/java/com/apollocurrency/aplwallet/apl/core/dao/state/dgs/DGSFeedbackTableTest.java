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
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.config.JdbiConfiguration;
import com.apollocurrency.aplwallet.apl.core.entity.state.derived.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSFeedback;
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

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;


@Tag("slow")
@EnableWeld
public class DGSFeedbackTableTest extends ValuesDbTableTest<DGSFeedback> {

    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private NtpTimeConfig ntpTimeConfig = new NtpTimeConfig();
    private TimeService timeService = new TimeServiceImpl(ntpTimeConfig.time());
    TransactionTestData td = new TransactionTestData();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(BlockchainImpl.class, DaoConfig.class,
        GlobalSyncImpl.class,
        FullTextConfigImpl.class,
        DGSFeedbackTable.class,
        TransactionServiceImpl.class, ShardDbExplorerImpl.class,
        TransactionEntityRowMapper.class, TxReceiptRowMapper.class, PrunableTxRowMapper.class,
        TransactionModelToEntityConverter.class, TransactionEntityToModelConverter.class,
        TransactionBuilderFactory.class,
        DerivedDbTablesRegistryImpl.class,
        BlockDaoImpl.class,
        BlockEntityRowMapper.class, BlockEntityToModelConverter.class, BlockModelToEntityConverter.class,
        TransactionDaoImpl.class, JdbiHandleFactory.class, JdbiConfiguration.class)
        .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
        .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
        .addBeans(MockBean.of(mock(PublicKeyDao.class), PublicKeyDao.class))
        .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
        .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
        .addBeans(MockBean.of(mock(PrunableLoadingService.class), PrunableLoadingService.class))
        .addBeans(MockBean.of(td.getTransactionTypeFactory(), TransactionTypeFactory.class))
        .addBeans(MockBean.of(ntpTimeConfig, NtpTimeConfig.class))
        .addBeans(MockBean.of(timeService, TimeService.class))
        .addBeans(MockBean.of(mock(BlockchainConfig.class), BlockchainConfig.class))
        .build();
    @Inject
    DGSFeedbackTable table;

    DGSTestData dtd;

    public DGSFeedbackTableTest() {
        super(DGSFeedback.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        dtd = new DGSTestData();
        super.setUp();
    }

    @Override
    protected List<DGSFeedback> dataToInsert() {
        return List.of(dtd.NEW_FEEDBACK_0, dtd.NEW_FEEDBACK_1, dtd.NEW_FEEDBACK_2);
    }

    @Override
    public DerivedDbTable<DGSFeedback> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSFeedback> getAll() {
        return List.of(dtd.FEEDBACK_0, dtd.FEEDBACK_1, dtd.FEEDBACK_2, dtd.FEEDBACK_3, dtd.FEEDBACK_4, dtd.FEEDBACK_5, dtd.FEEDBACK_6, dtd.FEEDBACK_7, dtd.FEEDBACK_8, dtd.FEEDBACK_9, dtd.FEEDBACK_10, dtd.FEEDBACK_11);
    }

    @Override
    protected List<DGSFeedback> getAllLatest() {
        return getAll().stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
    }
}
