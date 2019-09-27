/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import com.apollocurrency.aplwallet.apl.core.message.PrunableMessageService;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

@Disabled
@EnableWeld
public class DexContractTableTest extends VersionedEntityDbTableTest<ExchangeContract> {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, BlockchainConfig.class, BlockchainImpl.class, DaoConfig.class,
            JdbiHandleFactory.class,
            GlobalSyncImpl.class,
            FullTextConfigImpl.class,
            DexContractTable.class,
            DerivedDbTablesRegistryImpl.class,
            TimeServiceImpl.class, BlockDaoImpl.class, TransactionDaoImpl.class,
            GenesisPublicKeyTable.class)
            .addBeans(MockBean.of(getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(mock(PhasingPollService.class), PhasingPollService.class))
            .addBeans(MockBean.of(getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(mock(TransactionProcessor.class), TransactionProcessor.class))
            .addBeans(MockBean.of(mock(NtpTime.class), NtpTime.class))
            .addBeans(MockBean.of(mock(PrunableMessageService.class), PrunableMessageService.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();
    @Inject
    DexContractTable table;
    @Inject
    Blockchain blockchain;

    DexTestData dtd;

    public DexContractTableTest() {
        super(ExchangeContract.class);
    }

    @BeforeEach
    @Override
    public void setUp() {
        dtd = new DexTestData();
        super.setUp();
    }
    @Override
    public Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public ExchangeContract valueToInsert() {
        return dtd.NEW_EXCHANGE_CONTRACT_16;
    }

    @Override
    public DerivedDbTable<ExchangeContract> getDerivedDbTable() {
        return table;
    }

    @Override
    protected List<ExchangeContract> getAll() {
        return new ArrayList<>(List.of(
                dtd.EXCHANGE_CONTRACT_1, dtd.EXCHANGE_CONTRACT_2, dtd.EXCHANGE_CONTRACT_3,
                dtd.EXCHANGE_CONTRACT_4, dtd.EXCHANGE_CONTRACT_5, dtd.EXCHANGE_CONTRACT_6,
                dtd.EXCHANGE_CONTRACT_7, dtd.EXCHANGE_CONTRACT_8, dtd.EXCHANGE_CONTRACT_9,
                dtd.EXCHANGE_CONTRACT_10, dtd.EXCHANGE_CONTRACT_11, dtd.EXCHANGE_CONTRACT_12,
                dtd.EXCHANGE_CONTRACT_13, dtd.EXCHANGE_CONTRACT_14, dtd.EXCHANGE_CONTRACT_15
        ));
    }

    @Override
    public Comparator<ExchangeContract> getDefaultComparator() {
        return Comparator.comparing(ExchangeContract::getId).thenComparing(ExchangeContract::getOrderId);
    }
}
