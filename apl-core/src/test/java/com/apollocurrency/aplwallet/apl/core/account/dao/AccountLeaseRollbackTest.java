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
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountLeaseRollbackTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-lease-data.sql", "db/schema.sql");

    private Blockchain blockchain = mock(BlockchainImpl.class);
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainProcessor blockchainProcessor = mock(BlockchainProcessor.class);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountLeaseTable.class, AccountTable.class
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
    AccountLeaseTable table;

    @Inject
    AccountTable accountTable;

    @Test
    void testRollbackAllData() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(0));
        int actual = table.getAccountLeaseCount();
        assertEquals(0, actual);
    }

    @Test
    void testRollbackAt5000() {
        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(5000));
        int actual = table.getAccountLeaseCount();
        assertEquals(3, actual);
    }

    @Test
    void testToRollbackAt7000() {
        DbKey key150 = AccountTable.newKey(150);
        DbKey key110 = AccountTable.newKey(110);

        Account acc150 = accountTable.get(key150);
        assertEquals(50, acc150.getActiveLesseeId());

        Account acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());

        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(7000));
        int expected = 4;
        int actual = table.getAccountLeaseCount();
        assertEquals(expected, actual);

        acc150 = accountTable.get(key150);
        assertEquals(0, acc150.getActiveLesseeId());

        acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());
    }

    @ParameterizedTest(name = "{index} rollback {arguments}")
    @ValueSource(ints = {8000, 9000, 9439})
    void testRollbackAtHeight(int height) {
        DbKey key150 = AccountTable.newKey(150);
        DbKey key110 = AccountTable.newKey(110);

        Account acc150 = accountTable.get(key150);
        assertEquals(50, acc150.getActiveLesseeId());

        Account acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());

        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(height));
        int expected = 6;
        int actual = table.getAccountLeaseCount();
        assertEquals(expected, actual);

        acc150 = accountTable.get(key150);
        assertEquals(0, acc150.getActiveLesseeId());

        acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());
    }

    @Test
    void testNothingToRollbackAt10000() {
        DbKey key150 = AccountTable.newKey(150);
        DbKey key110 = AccountTable.newKey(110);

        Account acc150 = accountTable.get(key150);
        assertEquals(50, acc150.getActiveLesseeId());

        Account acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());

        DbUtils.inTransaction(dbExtension, (con) -> table.rollback(10000));
        int expected = 6;
        int actual = table.getAccountLeaseCount();
        assertEquals(expected, actual);

        acc150 = accountTable.get(key150);
        assertEquals(50, acc150.getActiveLesseeId());

        acc110 = accountTable.get(key110);
        assertEquals(10, acc110.getActiveLesseeId());
    }

}