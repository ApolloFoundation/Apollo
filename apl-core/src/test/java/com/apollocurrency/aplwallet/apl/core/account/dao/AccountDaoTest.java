/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
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
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountDaoTest  {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);


    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            PropertiesHolder.class, EntityProducer.class, AccountTable.class
            )
            .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchain, Blockchain.class, BlockchainImpl.class))

            .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
            .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
            .addBeans(MockBean.of(mock(BlockchainProcessor.class), BlockchainProcessor.class, BlockchainProcessorImpl.class))
            .build();

    @Inject
    AccountTable table;

    AccountTestData testData;

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
    }

    @Test
    void testLoad() {
        Account account = table.get(table.getDbKeyFactory().newKey(testData.ACC_0));
        assertNotNull(account);
        assertEquals(testData.ACC_0, account);
    }

    @Test
    void testLoad_ifNotExist_thenReturnNull() {
        Account account = table.get(table.getDbKeyFactory().newKey(testData.newAccount));
        assertNull(account);
    }

    @Test
    void testSave() {
        DbUtils.inTransaction(dbExtension, (con) -> table.insert(testData.newAccount));
        Account actual = table.get(table.getDbKeyFactory().newKey(testData.newAccount));
        assertNotNull(actual);
        assertTrue(actual.getDbId() != 0);
        assertEquals(testData.newAccount.getId(), actual.getId());
        assertEquals(testData.newAccount.getBalanceATM(), actual.getBalanceATM());
    }

    @Test
    void testTrim_on_0_height() throws SQLException {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(0));

        List<Account> expected = testData.ALL_ACCOUNTS;
        List<Account> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(expected, all);
    }

    @Test
    void testTrim_on_MAX_height() throws SQLException {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        DbUtils.inTransaction(dbExtension, (con) -> table.trim(Integer.MAX_VALUE));

        List<Account> expected = testData.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
        List<Account> all = table.getAllByDbId(Long.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE).getValues();

        assertEquals(expected, all);
    }

    @Test
    void testCheckAvailable_on_correct_height() {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        doReturn(testData.ACC_BLOCKCHAIN_HEIGHT).when(blockchain).getHeight();
        assertDoesNotThrow(() -> table.checkAvailable(testData.ACC_BLOCKCHAIN_HEIGHT));
    }

    @Test
    void testCheckAvailable_on_wrong_height() {
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        assertThrows(IllegalArgumentException.class, () -> table.checkAvailable(testData.ACC_BLOCKCHAIN_WRONG_HEIGHT));
    }

    @Test
    void getTotalSupply() {
        long total = table.getTotalSupply(testData.CREATOR_ID);
        assertEquals(999990000000000L, total);
    }

    @Test
    void getTopHolders() {
        List<Account> expected = testData.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).collect(Collectors.toList());
        List<Account> result = table.getTopHolders(100);
        assertEquals(expected.size(), result.size());
        assertEquals(expected, result.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
    }

    @Test
    void getTotalAmountOnTopAccounts() {
        long expected = testData.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).mapToLong(Account::getBalanceATM).sum();
        long result = table.getTotalAmountOnTopAccounts(100);
        assertEquals(expected, result);
    }

    @Test
    void getTotalNumberOfAccounts() throws SQLException {
        long expected = testData.ALL_ACCOUNTS.stream().filter(VersionedDerivedEntity::isLatest).count();
        long result = table.getTotalNumberOfAccounts();
        assertEquals(expected, result);
    }
}