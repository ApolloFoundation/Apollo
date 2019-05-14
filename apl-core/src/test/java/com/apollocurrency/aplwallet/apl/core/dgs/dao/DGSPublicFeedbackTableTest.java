/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.VersionedValuesDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPublicFeedback;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@EnableWeld
public class DGSPublicFeedbackTableTest extends VersionedValuesDbTableTest<DGSPublicFeedback> {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            FullTextConfigImpl.class,
            DGSPublicFeedbackTable.class,
            DerivedDbTablesRegistryImpl.class,
            EpochTime.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    DGSPublicFeedbackTable table;

    @Inject
    JdbiHandleFactory jdbiHandleFactory;

    DGSTestData dtd;

    public DGSPublicFeedbackTableTest() {
        super(DGSPublicFeedback.class);
    }

    @AfterEach
    void cleanup() {
        jdbiHandleFactory.close();
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
}
