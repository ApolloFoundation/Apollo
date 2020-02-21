/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountCurrency;
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
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.testutil.DbUtils;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.testutil.DbUtils.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountCurrencyDaoTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, AccountCurrencyTable.class
    )
            .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))
            .addBeans(MockBean.of(blockchainProcessor, BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
            .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
            .build();

    @Inject
    AccountCurrencyTable table;

    AccountTestData testData = new AccountTestData();

    Comparator<AccountCurrency> currencyComparator = Comparator
            .comparing(AccountCurrency::getUnits, Comparator.reverseOrder())
            .thenComparing(AccountCurrency::getAccountId)
            .thenComparing(AccountCurrency::getCurrencyId);

    @Test
    void testLoad() {
        AccountCurrency accountCurrency = table.get(table.getDbKeyFactory().newKey(testData.CUR_0));
        assertNotNull(accountCurrency);
        assertEquals(testData.CUR_0, accountCurrency);
    }

    @Test
    void testLoad_ifNotExist_thenReturnNull() {
        AccountCurrency accountCurrency = table.get(table.getDbKeyFactory().newKey(testData.newCurrency));
        assertNull(accountCurrency);
    }

    @Test
    void testSave_insert_new_entity() {//SQL MERGE -> INSERT
        AccountCurrency previous = table.get(table.getDbKeyFactory().newKey(testData.newCurrency));
        assertNull(previous);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newCurrency));
        AccountCurrency actual = table.get(table.getDbKeyFactory().newKey(testData.newCurrency));

        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newCurrency.getAccountId(), actual.getAccountId());
        assertEquals(testData.newCurrency.getCurrencyId(), actual.getCurrencyId());
    }

    @Test
    void testSave_update_existing_entity() {//SQL MERGE -> UPDATE
        AccountCurrency previous = table.get(table.getDbKeyFactory().newKey(testData.CUR_0));
        assertNotNull(previous);
        previous.setUnconfirmedUnits(previous.getUnconfirmedUnits()+50000);

        DbUtils.inTransaction(dbExtension, (con) -> table.insert(previous));
        AccountCurrency actual = table.get(table.getDbKeyFactory().newKey(previous));

        assertNotNull(actual);
        assertTrue(actual.getUnconfirmedUnits()-testData.CUR_0.getUnconfirmedUnits() == 50000);
        assertEquals(previous.getUnits(), actual.getUnits());
        assertEquals(previous.getCurrencyId(), actual.getCurrencyId());
    }


    @Test
    void testDefaultSort() {
        assertNotNull(table.defaultSort());
        List<AccountCurrency> expectedAll = testData.ALL_CURRENCY.stream()
                .sorted(currencyComparator)
                .collect(Collectors.toList());
        List<AccountCurrency> actualAll = toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(expectedAll, actualAll);
    }

    @Test
    void testGetAccountCurrencies() {
        List<AccountCurrency> actual = toList(table.getByAccount(testData.CUR_2.getAccountId(), 0, Integer.MAX_VALUE));
        assertEquals(2, actual.size());
        List<AccountCurrency> expected = testData.ALL_CURRENCY.stream()
                .filter(cur -> cur.getAccountId()==testData.CUR_2.getAccountId())
                .sorted(currencyComparator).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testGetAccountCurrencies_on_Height() {
        doReturn(testData.CUR_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        List<AccountCurrency> actual = toList(table.getByAccount(testData.CUR_2.getAccountId(), testData.CUR_2.getHeight(), 0, Integer.MAX_VALUE));
        assertEquals(1, actual.size());
        assertEquals(testData.CUR_2.getAccountId(), actual.get(0).getAccountId());
        assertEquals(testData.CUR_2, actual.get(0));
    }

    @Test
    void testGetCurrencyAccounts() {
        List<AccountCurrency> actual = toList(table.getByCurrency(testData.CUR_2.getCurrencyId(), 0, Integer.MAX_VALUE));
        assertEquals(7, actual.size());
        List<AccountCurrency> expected = testData.ALL_CURRENCY.stream()
                .filter(cur -> cur.getCurrencyId()==testData.CUR_2.getCurrencyId())
                .sorted(currencyComparator).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testGetCurrencyAccounts_on_Height() {
        doReturn(testData.CUR_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        List<AccountCurrency> actual = toList(table.getByCurrency(testData.CUR_2.getCurrencyId(), testData.CUR_2.getHeight(), 0, Integer.MAX_VALUE));
        assertEquals(1, actual.size());
        assertEquals(testData.CUR_2, actual.get(0));
    }

    @Test
    void testGetCurrencyAccountCount() {
        long actual = table.getCountByCurrency(testData.CUR_2.getCurrencyId());
        assertEquals(7, actual);
    }

    @Test
    void testGetCurrencyAccountCount_on_Height() {
        doReturn(testData.CUR_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        long actual = table.getCountByCurrency(testData.CUR_2.getCurrencyId(), testData.CUR_2.getHeight());
        assertEquals(1, actual);
    }

    @Test
    void testGetAccountCurrencyCount() {
        long actual = table.getCountByAccount(testData.CUR_2.getAccountId());
        assertEquals(2, actual);
    }

    @Test
    void testGetAccountCurrencyCount_on_Height() {
        doReturn(testData.CUR_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        long actual = table.getCountByAccount(testData.CUR_2.getAccountId(), testData.CUR_2.getHeight());
        assertEquals(1, actual);
    }
}