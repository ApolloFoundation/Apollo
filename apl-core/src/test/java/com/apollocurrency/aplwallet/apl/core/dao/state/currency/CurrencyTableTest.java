/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.data.CurrencyTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.testutil.EntityProducer;
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

@Tag("slow")
@EnableWeld
class CurrencyTableTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/currency-data.sql", "db/schema.sql");

    @Inject
    CurrencyTable table;
    CurrencyTestData td;
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, EntityProducer.class, CurrencyTable.class
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
        td = new CurrencyTestData();
    }

    @Test
    void testLoad() {
        Currency currencySupply = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_0));
        assertNotNull(currencySupply);
        assertEquals(td.CURRENCY_0, currencySupply);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        Currency currencySupply = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));
        assertNull(currencySupply);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        Currency previous = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.CURRENCY_NEW));
        Currency actual = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.CURRENCY_NEW.getCurrencyId(), actual.getCurrencyId());
        assertEquals(td.CURRENCY_NEW.getAccountId(), actual.getAccountId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        Currency previous = table.get(table.getDbKeyFactory().newKey(td.CURRENCY_1));
        assertNotNull(previous);
        previous.setInitialSupply(previous.getInitialSupply() + 100);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        Currency actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertEquals(100, actual.getInitialSupply() - td.CURRENCY_1.getInitialSupply());
        assertEquals(previous.getCurrencyId(), actual.getCurrencyId());
        assertEquals(previous.getIssuanceHeight(), actual.getIssuanceHeight());
    }

}