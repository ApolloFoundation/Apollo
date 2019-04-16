/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import static com.apollocurrency.aplwallet.apl.testutil.DbUtils.inTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.apollocurrency.aplwallet.apl.core.account.dao.AccountGuaranteedBalanceTable;
import com.apollocurrency.aplwallet.apl.core.account.model.AccountGuaranteedBalance;
import com.apollocurrency.aplwallet.apl.core.app.CollectionUtil;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.data.AccountGuaranteedBalanceData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;

@EnableWeld
public class AccountGuaranteedBalanceTableTest {
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = 100;
    @RegisterExtension
    DbExtension dbExtension = new DbExtension();

    BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    PropertiesHolder propertiesHolder = mockPropertiesHolder();

    @WeldSetup
    WeldInitiator weldInitiator = WeldInitiator.from(AccountGuaranteedBalanceTable.class, DerivedDbTablesRegistryImpl.class, FullTextConfigImpl.class)
            .addBeans(MockBean.of(dbExtension.getDatabaseManger(), DatabaseManager.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(propertiesHolder, PropertiesHolder.class))
            .build();


    @Inject
    AccountGuaranteedBalanceTable table;
    AccountGuaranteedBalanceData atd;

    @BeforeEach
    void setUp() {
        atd = new AccountGuaranteedBalanceData();
    }
    @Test
    public void testRollbackWhenLessThanRollbackHeight() throws SQLException {
        inTransaction(dbExtension, (con) -> table.rollback(atd.BALANCE_5.getHeight() - 1));
        List<AccountGuaranteedBalance> balances = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(Arrays.asList(atd.BALANCE_4, atd.BALANCE_3, atd.BALANCE_2, atd.BALANCE_1), balances);
    }

    @Test
    void testRollbackWhenEqualToRollbackHeight() throws SQLException {
        inTransaction(dbExtension, (con) -> table.rollback(atd.BALANCE_4.getHeight()));
        List<AccountGuaranteedBalance> balances = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(Arrays.asList(atd.BALANCE_4, atd.BALANCE_3, atd.BALANCE_2, atd.BALANCE_1), balances);
    }

    @Test
    public void testTrimWhenEqualToUpperBound() throws SQLException {
        doReturn(GUARANTEED_BALANCE_CONFIRMATIONS).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        inTransaction(dbExtension, (con) -> table.trim(atd.BALANCE_3.getHeight() + GUARANTEED_BALANCE_CONFIRMATIONS, dbExtension.getDatabaseManger().getDataSource()));
        List<AccountGuaranteedBalance> balances = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(Arrays.asList(atd.BALANCE_5, atd.BALANCE_4, atd.BALANCE_3), balances);

    }

    @Test
    public void testTrimWhenExceedUpperBound() throws SQLException {
        doReturn(GUARANTEED_BALANCE_CONFIRMATIONS - 1).when(blockchainConfig).getGuaranteedBalanceConfirmations();
        inTransaction(dbExtension, (con) -> table.trim(atd.BALANCE_3.getHeight() + GUARANTEED_BALANCE_CONFIRMATIONS, dbExtension.getDatabaseManger().getDataSource()));
        List<AccountGuaranteedBalance> balances = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
        assertEquals(Arrays.asList(atd.BALANCE_5), balances);
    }

    @Test
    void testSave() throws SQLException {

        inTransaction(dbExtension, (con) -> {
            table.insert(atd.NEW_BALANCE);
        });
        List<AccountGuaranteedBalance> balances = CollectionUtil.toList(table.getAll(0, Integer.MAX_VALUE));
        List<AccountGuaranteedBalance> expected = new ArrayList<>(atd.BALANCES);
        expected.add(0, atd.NEW_BALANCE);
        assertEquals(expected, balances);
    }

    private PropertiesHolder mockPropertiesHolder() {
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        Properties properties = new Properties();
        properties.put("apl.batchCommitSize", 1);
        propertiesHolder.init(properties);
        return propertiesHolder;
    }
}
