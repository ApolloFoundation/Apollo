/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.exchange;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.DbContainerBaseTest;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.ExchangeRequest;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.data.ExchangeRequestTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j

@Tag("slow")
@EnableWeld
class ExchangeRequestTableTest extends DbContainerBaseTest {

    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(mariaDBContainer);

    @Inject
    ExchangeRequestTable table;
    ExchangeRequestTestData td;

    Comparator<ExchangeRequest> exchangeRequestComparator = Comparator
        .comparing(ExchangeRequest::getDbId)
        .thenComparing(ExchangeRequest::getAccountId).reversed();

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, ExchangeRequestTable.class
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
        td = new ExchangeRequestTestData();
    }

    @AfterEach
    void tearDown() {
        dbExtension.cleanAndPopulateDb();
    }

    @Test
    void testLoad() {
        ExchangeRequest result = table.get(table.getDbKeyFactory().newKey(td.EXCHANGE_REQUEST_0));
        assertNotNull(result);
        assertEquals(td.EXCHANGE_REQUEST_0, result);
    }

    @Test
    void testLoad_returnNull_ifNotExist() {
        ExchangeRequest result = table.get(table.getDbKeyFactory().newKey(td.EXCHANGE_REQUEST_NEW));
        assertNull(result);
    }

    @Test
    void testSave_insert_new_entity() {
        ExchangeRequest previous = table.get(table.getDbKeyFactory().newKey(td.EXCHANGE_REQUEST_NEW));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(td.EXCHANGE_REQUEST_NEW));
        ExchangeRequest actual = table.get(table.getDbKeyFactory().newKey(td.EXCHANGE_REQUEST_NEW));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(td.EXCHANGE_REQUEST_NEW.getAccountId(), actual.getAccountId());
        assertEquals(td.EXCHANGE_REQUEST_NEW.getCurrencyId(), actual.getCurrencyId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        ExchangeRequest previous = table.get(table.getDbKeyFactory().newKey(td.EXCHANGE_REQUEST_1));
        assertNotNull(previous);

        assertThrows(RuntimeException.class, () -> // not permitted by DB constraints
            DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous))
        );
    }

    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<ExchangeRequest> expectedAll = td.ALL_EXCHANGE_REQUEST_ORDERED_BY_DBID.stream()
            .sorted(exchangeRequestComparator).collect(Collectors.toList());
        List<ExchangeRequest> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }


}