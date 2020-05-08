/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account.dao;

import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@EnableWeld
class AccountGuaranteedBalanceTableTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension(DbTestData.getInMemDbProps(), "db/acc-data.sql", "db/schema.sql");
    @Inject
    AccountGuaranteedBalanceTable table;
    AccountTestData testData = new AccountTestData();
    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        PropertiesHolder.class, AccountGuaranteedBalanceTable.class
    )
        .addBeans(MockBean.of(dbExtension.getDatabaseManager(), DatabaseManager.class))
        .addBeans(MockBean.of(dbExtension.getDatabaseManager().getJdbi(), Jdbi.class))
        .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
        .addBeans(MockBean.of(mock(FullTextConfig.class), FullTextConfig.class, FullTextConfigImpl.class))
        .addBeans(MockBean.of(mock(DerivedTablesRegistry.class), DerivedTablesRegistry.class, DerivedDbTablesRegistryImpl.class))
        .build();

    @Test
    void trim() throws SQLException {
        long sizeAll = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues().size();
        assertEquals(testData.ALL_BALANCES.size(), sizeAll);
        DbUtils.inTransaction(dbExtension, con -> table.trim(testData.ACC_GUARANTEE_BALANCE_HEIGHT_MAX));
        long sizeTrim = table.getAllByDbId(0, Integer.MAX_VALUE, Long.MAX_VALUE).getValues().size();
        assertEquals(1, sizeTrim);
    }

    @Test
    void testGetSumOfAdditions() {
        long accountId = testData.ACC_BALANCE_1.getAccountId();
        int height1 = testData.ACC_BALANCE_1.getHeight();
        int height2 = height1 + 1000;

        long expectedSum = testData.ALL_BALANCES.stream().
            filter(b -> (b.getAccountId() == accountId && b.getHeight() > height1 && b.getHeight() <= height2)).
            mapToLong(AccountGuaranteedBalance::getAdditions).sum();

        assertEquals(expectedSum, table.getSumOfAdditions(accountId, height1, height2));
    }

    @Test
    void getLessorsAdditions() {
        List<Long> lessorsList = List.of(testData.ACC_BALANCE_1.getAccountId(), testData.ACC_BALANCE_3.getAccountId(), testData.ACC_BALANCE_5.getAccountId());
        doReturn(1440).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        int height = testData.ACC_GUARANTEE_BALANCE_HEIGHT_MAX;

        Map<Long, Long> result = table.getLessorsAdditions(lessorsList, height, height + 1000);
        assertEquals(lessorsList.size(), result.values().size());
        lessorsList.forEach(accountId -> assertEquals(getSumOfAdditionsByAccountId(accountId), result.get(accountId)));
    }

    private long getSumOfAdditionsByAccountId(final long accountId) {
        long expectedSum = testData.ALL_BALANCES.stream().
            filter(balance -> (accountId == balance.getAccountId())).
            mapToLong(AccountGuaranteedBalance::getAdditions).sum();
        return expectedSum;
    }

    @Test
    void addToGuaranteedBalanceATM() {
        long amountATM = 10000L;
        long expectedSum = testData.ACC_BALANCE_3.getAdditions() + amountATM;

        table.addToGuaranteedBalanceATM(testData.ACC_BALANCE_3.getAccountId(), amountATM, testData.ACC_BALANCE_3.getHeight() + 1);
        assertEquals(expectedSum, table.getSumOfAdditions(testData.ACC_BALANCE_3.getAccountId(), testData.ACC_BALANCE_3.getHeight() - 1, testData.ACC_BALANCE_3.getHeight() + 1));
    }
}