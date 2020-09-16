/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyBuyOfferTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencyBuyOfferServiceImpl;
import com.apollocurrency.aplwallet.apl.data.CurrencyBuyOfferTestData;
import com.apollocurrency.aplwallet.apl.data.CurrencySupplyTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
@Testcontainers
@Tag("slow")
@EnableWeld
class CurrencyBuyOfferServiceTest {
    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.4")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);

    @Inject
    CurrencyBuyOfferTable table;
    CurrencyBuyOfferTestData td;
    CurrencySupplyTestData supplyTestData;
    @Inject
    private CurrencyBuyOfferService offerService;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, CurrencyBuyOfferTable.class, CurrencyBuyOfferServiceImpl.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
        .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .addBeans(MockBean.of(blockChainInfoService, BlockChainInfoService.class))
        .build();

    @BeforeEach
    void setUp() {
        td = new CurrencyBuyOfferTestData();
    }

    @Test
    void test_count() {
        int result = offerService.getCount();
        assertEquals(9, result);
    }

    @Test
    void test_getOffer_by_id() {
        CurrencyBuyOffer result = offerService.getOffer(td.OFFER_3.getId());
        assertNotNull(result);
        assertEquals(td.OFFER_3, result);
    }

    @Test
    void test_getAllStream() {
        Stream<CurrencyBuyOffer> result = offerService.getAllStream(0, 5);
        assertNotNull(result);
        assertEquals(6, result.count());
    }

    @Test
    void test_getOffersStream() {
        supplyTestData = new CurrencySupplyTestData();
        Currency currency = new Currency(-3205373316822570812L, -6392448561240417498L, "Gold",      "gold",     "GLD",
            "A new token allowing the easy trade of gold bullion.", 3,   9900000000000000L, 0, 9900000000000000L,
            3015,             0,              0,                         0,               0,
            (byte)0,      (byte)0,          (byte)5,    supplyTestData.CURRENCY_SUPPLY_0,   3015,   true,   false);
        Stream<CurrencyBuyOffer> result = offerService.getOffersStream(currency, 0, 10);
        assertNotNull(result);
        assertEquals(2, result.count());
    }

    @Test
    void test_getCurrencyOffersStream() {
        Stream<CurrencyBuyOffer> result = offerService.getCurrencyOffersStream(
            td.OFFER_1.getCurrencyId(), false,0, 5);
        assertNotNull(result);
        assertEquals(2, result.count());
    }

    @Test
    void test_getAccountOffersStream() {
        Stream<CurrencyBuyOffer> result = offerService.getAccountOffersStream(
            td.OFFER_5.getAccountId(), false,0, 3);
        assertNotNull(result);
        assertEquals(4, result.count());
    }

    @Test
    void test_getOffer_by_currency_account() {
        CurrencyBuyOffer result = offerService.getOffer(td.OFFER_7.getCurrencyId(), td.OFFER_7.getAccountId());
        assertNotNull(result);
        assertEquals(td.OFFER_7, result);
    }

    @Test
    void test_getOffersStream_by_clause_no_sort() {
        Stream<CurrencyBuyOffer> result = offerService.getOffersStream(
            new DbClause.IntClause("expiration_height", td.OFFER_6.getExpirationHeight()), 0, -1);
        assertNotNull(result);
        assertEquals(9, result.count());
    }

    @Test
    void test_getOffersStream_by_clause_sort() {
        Stream<CurrencyBuyOffer> result = offerService.getOffersStream(
            new DbClause.LongClause("currency_id", td.OFFER_8.getCurrencyId()), 0, -1,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
        assertNotNull(result);
        assertEquals(2, result.count());
    }

}