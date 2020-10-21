/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.CurrencyFounderTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@Testcontainers
@Tag("slow")
@EnableWeld
class CurrencyFounderTableTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension dbExtension = new DbExtension(mariaDBContainer, DbTestData.getInMemDbProps(), "db/currency_founder-data.sql", "db/schema.sql");
    @Inject
    CurrencyFounderTable table;
    CurrencyFounderTestData td;
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, CurrencyFounderTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))

        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .build();


    @BeforeEach
    void setUp() {
        td = new CurrencyFounderTestData();
    }

    @Test
    void testLoad() {
        CurrencyFounder result = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_FOUNDER_0));
        assertNotNull(result);
        assertEquals(td.CURRENCY_FOUNDER_0, result);
    }

    @Test
    void testLoad_ifNotExist_thenReturnNull() {
        CurrencyFounder result = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_FOUNDER_NEW));
        assertNull(result);
    }

    @Test
    void testSave() {
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.CURRENCY_FOUNDER_NEW));
        CurrencyFounder actual = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_FOUNDER_NEW));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.CURRENCY_FOUNDER_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.CURRENCY_FOUNDER_NEW.getAmountPerUnitATM(), actual.getAmountPerUnitATM());
    }

    @Test
    void testTrim_by_height() throws SQLException {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        int all = table.getRowCount();
        assertEquals(8, all);
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(100000, true));

        all = table.getRowCount();
        assertEquals(3, all);

        List<CurrencyFounder> expected = td.ALL_CURRENCY_FOUNDER_ORDERED_BY_DBID;
        List<CurrencyFounder> result = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();
        assertNotNull(result);
        assertEquals(expected, result);
    }


}