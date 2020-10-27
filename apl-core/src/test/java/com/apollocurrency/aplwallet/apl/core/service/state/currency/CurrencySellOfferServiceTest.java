/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySellOfferTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
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
import com.apollocurrency.aplwallet.apl.core.service.state.currency.impl.CurrencySellOfferServiceImpl;
import com.apollocurrency.aplwallet.apl.data.CurrencySellOfferTestData;
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

import javax.inject.Inject;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class CurrencySellOfferServiceTest extends DbContainerBaseTest {

    @RegisterExtension
    DbExtension dbExtension = new DbExtension(mariaDBContainer);

    @Inject
    CurrencySellOfferTable table;
    CurrencySellOfferTestData td;
    @Inject
    private CurrencySellOfferService offerService;

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockChainInfoService blockChainInfoService = mock(BlockChainInfoService.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, CurrencySellOfferTable.class, CurrencySellOfferServiceImpl.class
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
        td = new CurrencySellOfferTestData();
    }

    @Test
    void test_count() {
        int result = offerService.getCount();
        assertEquals(9, result);
    }

    @Test
    void test_getOffer_by_id() {
        CurrencySellOffer result = offerService.getOffer(td.OFFER_3.getId());
        assertNotNull(result);
        assertEquals(td.OFFER_3, result);
    }

    @Test
    void test_getAllStream() {
        Stream<CurrencySellOffer> result = offerService.getAllStream(0, 5);
        assertNotNull(result);
        assertEquals(6, result.count());
    }

    @Test
    void test_getOffersStream() {
        Currency currency = new Currency(
            -4132128809614485872L, 0L, "NotKnown", "not_known", "code", "description",
            0, 0L, 0L, 0L, 0,
            0, 0L, 0, 0,
            (byte)0, (byte)0, (byte)0, null, 0,
            true, true);
        Stream<CurrencySellOffer> result = offerService.getOffersStream(currency, 0, 4);
        assertNotNull(result);
        assertEquals(2, result.count());
    }

    @Test
    void test_getCurrencyOffersStream() {
        Stream<CurrencySellOffer> result = offerService.getCurrencyOffersStream(
            td.OFFER_1.getCurrencyId(), false,0, 5);
        assertNotNull(result);
        assertEquals(2, result.count());
    }

    @Test
    void test_getAccountOffersStream() {
        Stream<CurrencySellOffer> result = offerService.getAccountOffersStream(
            td.OFFER_5.getAccountId(), false,0, 3);
        assertNotNull(result);
        assertEquals(4, result.count());
    }

    @Test
    void test_getOffer_by_currency_account() {
        CurrencySellOffer result = offerService.getOffer(td.OFFER_7.getCurrencyId(), td.OFFER_7.getAccountId());
        assertNotNull(result);
        assertEquals(td.OFFER_7, result);
    }

    @Test
    void test_getOffersStream_by_clause_no_sort() {
        Stream<CurrencySellOffer> result = offerService.getOffersStream(
            new DbClause.IntClause("expiration_height", td.OFFER_6.getExpirationHeight()), 0, -1);
        assertNotNull(result);
        assertEquals(9, result.count());
    }

    @Test
    void test_getOffersStream_by_clause_sort() {
        Stream<CurrencySellOffer> result = offerService.getOffersStream(
            new DbClause.LongClause("currency_id", td.OFFER_8.getCurrencyId()), 0, -1,
            " ORDER BY rate DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
        assertNotNull(result);
        assertEquals(2, result.count());
    }

}