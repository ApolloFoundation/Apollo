/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.CurrencyBuyOfferTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class CurrencyBuyOfferTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);

    @Inject
    CurrencyBuyOfferTable table;
    CurrencyBuyOfferTestData td;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, CurrencyBuyOfferTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new CurrencyBuyOfferTestData();
    }

    @Test
    void testLoad() {
        CurrencyBuyOffer offer = table.get(table.getDbKeyFactory().newKey(td.OFFER_0));
        assertNotNull(offer);
        assertEquals(td.OFFER_0, offer);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        dbExtension.cleanAndPopulateDb();

        CurrencyBuyOffer offer = table.get(table.getDbKeyFactory().newKey(td.OFFER_NEW));
        assertNull(offer);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        CurrencyBuyOffer previous = table.get(table.getDbKeyFactory().newKey(td.OFFER_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.OFFER_NEW));
        CurrencyBuyOffer actual = table.get(table.getDbKeyFactory().newKey(td.OFFER_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.OFFER_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.OFFER_NEW.getId(), actual.getId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        CurrencyBuyOffer previous = table.get(table.getDbKeyFactory().newKey(td.OFFER_1));
        assertNotNull(previous);
        previous.setLimit(previous.getLimit() + 10);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        CurrencyBuyOffer actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertEquals(10, actual.getLimit() - td.OFFER_1.getLimit());
        assertEquals(previous.getCurrencyId(), actual.getCurrencyId());
        assertEquals(previous.getId(), actual.getId());
    }

    @Test
    void test_getCount() {
        dbExtension.cleanAndPopulateDb();

        int result = table.getCount();
        assertEquals(9, result);
    }

    @Test
    void test_getOffer() {
        CurrencyBuyOffer result = table.get(CurrencyBuyOfferTable.buyOfferDbKeyFactory.newKey(td.OFFER_2));
        assertNotNull(result);
        assertEquals(td.OFFER_2, result);
    }
}