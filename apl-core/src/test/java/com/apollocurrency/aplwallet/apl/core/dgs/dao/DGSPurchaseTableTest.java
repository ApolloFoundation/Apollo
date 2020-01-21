/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.GlobalSyncImpl;
import com.apollocurrency.aplwallet.apl.core.app.TimeServiceImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionDaoImpl;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.VersionedEntityDbTableTest;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.DerivedDbTable;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSPurchase;
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexService;
import com.apollocurrency.aplwallet.apl.core.shard.BlockIndexServiceImpl;
import com.apollocurrency.aplwallet.apl.data.DGSTestData;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.mockito.Mockito.mock;

@EnableWeld
public class DGSPurchaseTableTest extends VersionedEntityDbTableTest<DGSPurchase> {
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            GlobalSyncImpl.class,
            FullTextConfigImpl.class,
            DGSPurchaseTable.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(mock(BlockIndexService.class), BlockIndexService.class, BlockIndexServiceImpl.class))
            .build();
    @Inject
    DGSPurchaseTable table;
    @Inject
    Blockchain blockchain;

    DGSTestData dtd;

    public DGSPurchaseTableTest() {
        super(DGSPurchase.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        dtd = new DGSTestData();
        super.setUp();
    }
    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public DGSPurchase valueToInsert() {
        return dtd.NEW_PURCHASE;
    }

    @Override
    public DerivedDbTable<DGSPurchase> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<DGSPurchase> getAll() {
        return new ArrayList<>(List.of(dtd.PURCHASE_0, dtd.PURCHASE_1, dtd.PURCHASE_2, dtd.PURCHASE_3, dtd.PURCHASE_4, dtd.PURCHASE_5, dtd.PURCHASE_6, dtd.PURCHASE_7, dtd.PURCHASE_8, dtd.PURCHASE_9, dtd.PURCHASE_10, dtd.PURCHASE_11, dtd.PURCHASE_12, dtd.PURCHASE_13, dtd.PURCHASE_14, dtd.PURCHASE_15, dtd.PURCHASE_16, dtd.PURCHASE_17, dtd.PURCHASE_18));
    }

    @Override
    public Comparator<DGSPurchase> getDefaultComparator() {
        return Comparator.comparing(DGSPurchase::getTimestamp).reversed().thenComparing(DGSPurchase::getId);
    }
}
